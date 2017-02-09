/**
 * Project: MClubTracker
 * 
 * File Created at 2015-7-28
 * $id$
 * 
 * Copyright 2013, Shawn Chain (shawn.chain@gmail.com).
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mclub.tracker.protocol;

import java.util.TimeZone;

import mclub.sys.ConfigService;
import mclub.tracker.PositionData;
import mclub.tracker.TrackerDataService;
import mclub.tracker.TrackerServer;
import mclub.tracker.protocol.helper.BitUtil;
import mclub.tracker.protocol.helper.Checksum;
import mclub.tracker.protocol.helper.DateBuilder;
import mclub.tracker.protocol.helper.Event;
import mclub.tracker.protocol.helper.UnitsConverter;

import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author shawn
 *
 */
public class Gt06TrackerServer extends TrackerServer {
	private static Logger log = LoggerFactory.getLogger(Gt06TrackerServer.class);

	/**
	 * @param bootstrap
	 * @param protocol
	 * @param trackerDataService
	 */
	public Gt06TrackerServer(Bootstrap bootstrap, String protocol,
			TrackerDataService trackerDataService, ConfigService configService) {
		super(bootstrap, protocol, trackerDataService, configService);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * mclub.tracker.TrackerServer#addProtocolHandlers(org.jboss.netty.channel
	 * .ChannelPipeline)
	 */
	@Override
	protected void addProtocolHandlers(ChannelPipeline pipeline) {
		pipeline.addLast("frameDecoder", new Gt06FrameDecoder());
		pipeline.addLast("objectDecoder", new Gt06ProtocolDecoder());
	}

	static class Gt06ProtocolDecoder extends OneToOneDecoder {
		// TODO - configurable timezone
		private boolean forceTimeZone = true;
		private final TimeZone timeZone = TimeZone.getTimeZone("GMT+8:00");

		private String udid;

		/*
		 * Decode the incoming tracker messages
		 */
		@Override
		protected Object decode(ChannelHandlerContext ctx, Channel channel,
				Object msg) throws Exception {
			ChannelBuffer buf = (ChannelBuffer) msg;

			if (buf.readByte() != 0x78 || buf.readByte() != 0x78) {
				return null;
			}

			int length = buf.readUnsignedByte(); // size
			int dataLength = length - 5;

			int type = buf.readUnsignedByte();
			try {
				if (type == MSG_LOGIN) {

					String imei = ChannelBuffers.hexDump(buf.readBytes(8))
							.substring(1);
					buf.readUnsignedShort(); // type

					// Timezone offset
					if (dataLength > 10) {
						int extensionBits = buf.readUnsignedShort();
						int hours = (extensionBits >> 4) / 100;
						int minutes = (extensionBits >> 4) % 100;
						int offset = (hours * 60 + minutes) * 60;
						if ((extensionBits & 0x8) != 0) {
							offset = -offset;
						}
						if (!forceTimeZone) {
							timeZone.setRawOffset(offset);
						}
					}

					if (identify(imei, channel)) {
						log.info("Device " + imei + " logged in");
						buf.skipBytes(buf.readableBytes() - 6);
						sendResponse(channel, type, buf.readUnsignedShort());
					}
					return null;
					
				}
				
				// Bail out if device id is unknown
				if (!hasDeviceId()) {
					return null;
				}
				
				if (isSupported(type)) {
					PositionData position = new PositionData();
					position.setUdid(getDeviceId());

					position.addExtendedInfo("protocol", "GT-06");

					if (hasGps(type)) {
						decodeGps(position, buf);
					}
					// else {
					// getLastLocation(position, null);
					// }

					if (hasLbs(type)) {
						decodeLbs(position, buf, hasStatus(type));
					}

					if (hasStatus(type)) {
						decodeStatus(position, buf);
					}

					if (type == MSG_GPS_LBS_1
							&& buf.readableBytes() == 4 + 6) {
						position.addExtendedInfo(Event.KEY_ODOMETER,
								buf.readUnsignedInt());
					}

					if (buf.readableBytes() > 6) {
						buf.skipBytes(buf.readableBytes() - 6);
					}
					int index = buf.readUnsignedShort();
					position.addExtendedInfo(Event.KEY_INDEX, index);
					sendResponse(channel, type, index);

					return position;

				} else {
					// unsupported packet
					log.info("Unsupported packet: " + ChannelBuffers.hexDump(buf));
					buf.skipBytes(dataLength);
					if (type != MSG_COMMAND_0 && type != MSG_COMMAND_1 && type != MSG_COMMAND_2) {
						sendResponse(channel, type, buf.readUnsignedShort());
					}
				}

			} catch (Exception e) {
				if (log.isDebugEnabled())
					log.warn("Error parse message", e);
				else
					log.warn("Error parse message: " + e.getMessage());
			}

			return null;
		}

		public static final int MSG_LOGIN = 0x01;
		public static final int MSG_GPS = 0x10;
		public static final int MSG_LBS = 0x11;
		public static final int MSG_GPS_LBS_1 = 0x12;
		public static final int MSG_GPS_LBS_2 = 0x22;
		public static final int MSG_STATUS = 0x13;
		public static final int MSG_SATELLITE = 0x14;
		public static final int MSG_STRING = 0x15;
		public static final int MSG_GPS_LBS_STATUS_1 = 0x16;
		public static final int MSG_GPS_LBS_STATUS_2 = 0x26;
		public static final int MSG_GPS_LBS_STATUS_3 = 0x27;
		public static final int MSG_LBS_PHONE = 0x17;
		public static final int MSG_LBS_EXTEND = 0x18;
		public static final int MSG_LBS_STATUS = 0x19;
		public static final int MSG_GPS_PHONE = 0x1A;
		public static final int MSG_GPS_LBS_EXTEND = 0x1E;
		public static final int MSG_COMMAND_0 = 0x80;
		public static final int MSG_COMMAND_1 = 0x81;
		public static final int MSG_COMMAND_2 = 0x82;

		private static boolean isSupported(int type) {
			return hasGps(type) || hasLbs(type) || hasStatus(type);
		}

		private static boolean hasGps(int type) {
			return type == MSG_GPS || type == MSG_GPS_LBS_1
					|| type == MSG_GPS_LBS_2 || type == MSG_GPS_LBS_STATUS_1
					|| type == MSG_GPS_LBS_STATUS_2
					|| type == MSG_GPS_LBS_STATUS_3 || type == MSG_GPS_PHONE
					|| type == MSG_GPS_LBS_EXTEND;
		}

		private static boolean hasLbs(int type) {
			return type == MSG_LBS || type == MSG_LBS_STATUS
					|| type == MSG_GPS_LBS_1 || type == MSG_GPS_LBS_2
					|| type == MSG_GPS_LBS_STATUS_1
					|| type == MSG_GPS_LBS_STATUS_2
					|| type == MSG_GPS_LBS_STATUS_3;
		}

		private static boolean hasStatus(int type) {
			return type == MSG_STATUS || type == MSG_LBS_STATUS
					|| type == MSG_GPS_LBS_STATUS_1
					|| type == MSG_GPS_LBS_STATUS_2
					|| type == MSG_GPS_LBS_STATUS_3;
		}

		private static void sendResponse(Channel channel, int type, int index) {
			if (channel != null) {
				ChannelBuffer response = ChannelBuffers.directBuffer(10);
				response.writeByte(0x78);
				response.writeByte(0x78); // header
				response.writeByte(0x05); // size
				response.writeByte(type);
				response.writeShort(index);
				response.writeShort(Checksum.crc16(Checksum.CRC16_X25,
						response.toByteBuffer(2, 4)));
				response.writeByte(0x0D);
				response.writeByte(0x0A); // ending
				channel.write(response);
			}
		}

		private void decodeGps(PositionData position, ChannelBuffer buf) {

			DateBuilder dateBuilder = new DateBuilder(timeZone).setDate(
					buf.readUnsignedByte(), buf.readUnsignedByte(),
					buf.readUnsignedByte()).setTime(buf.readUnsignedByte(),
					buf.readUnsignedByte(), buf.readUnsignedByte());
			position.setTime(dateBuilder.getDate());

			int length = buf.readUnsignedByte();
			position.addExtendedInfo(Event.KEY_SATELLITES,
					BitUtil.to(length, 4));
			length = BitUtil.from(length, 4);

			double latitude = buf.readUnsignedInt() / 60.0 / 30000.0;
			double longitude = buf.readUnsignedInt() / 60.0 / 30000.0;
			//position.setSpeed(UnitsConverter.knotsFromKph(buf.readUnsignedByte()));
			position.setSpeed((double)buf.readUnsignedByte()); // we're using km/h metric

			int flags = buf.readUnsignedShort();
			position.setCourse(Double.parseDouble("" + BitUtil.to(flags, 10)));
			position.setValid(BitUtil.check(flags, 12));

			position.setAltitude(-1d);
			
			if (!BitUtil.check(flags, 10)) {
				latitude = -latitude;
			}
			if (BitUtil.check(flags, 11)) {
				longitude = -longitude;
			}

			position.setLatitude(latitude);
			position.setLongitude(longitude);

			if (BitUtil.check(flags, 14)) {
				position.addExtendedInfo(Event.KEY_IGNITION,
						BitUtil.check(flags, 15));
			}

			buf.skipBytes(length - 12); // skip reserved
		}

		private void decodeLbs(PositionData position, ChannelBuffer buf,
				boolean hasLength) {

			int lbsLength = 0;
			if (hasLength) {
				lbsLength = buf.readUnsignedByte();
			}

			position.addExtendedInfo(Event.KEY_MCC, buf.readUnsignedShort());
			position.addExtendedInfo(Event.KEY_MNC, buf.readUnsignedByte());
			position.addExtendedInfo(Event.KEY_LAC, buf.readUnsignedShort());
			position.addExtendedInfo(Event.KEY_CELL, buf.readUnsignedMedium());

			if (lbsLength > 0) {
				buf.skipBytes(lbsLength - 9);
			}
		}

		private void decodeStatus(PositionData position, ChannelBuffer buf) {

			position.addExtendedInfo(Event.KEY_ALARM, true);

			int flags = buf.readUnsignedByte();

			position.addExtendedInfo(Event.KEY_IGNITION,
					BitUtil.check(flags, 1));
			// decode other flags

			position.addExtendedInfo(Event.KEY_POWER, buf.readUnsignedByte());
			position.addExtendedInfo(Event.KEY_GSM, buf.readUnsignedByte());
		}

		private boolean hasDeviceId() {
			return udid != null;
		}

		String getDeviceId() {
			return udid;
		}

		public boolean identify(String uniqueId, Channel channel) {
			this.udid = uniqueId;
			return true;
		}

	} // end of class Gt06ProtocolDecoder
	
	/**
	 * Frame decoder
	 */
	static class Gt06FrameDecoder extends FrameDecoder {
		private static Logger log = LoggerFactory.getLogger(Gt06FrameDecoder.class);
	    @Override
	    protected Object decode(
	            ChannelHandlerContext ctx,
	            Channel channel,
	            ChannelBuffer buf) throws Exception {

	        // Check minimum length
	        if (buf.readableBytes() < 5) {
	            return null;
	        }

	        int length = 2 + 2; // head and tail
	        
	        if(log.isDebugEnabled()) log.debug("frame: " + ChannelBuffers.hexDump(buf));
	        
	        if (buf.getByte(buf.readerIndex()) == 0x78) {
	            length += 1 + buf.getUnsignedByte(buf.readerIndex() + 2);
	        } else {
	            length += 2 + buf.getUnsignedShort(buf.readerIndex() + 2);
	        }

	        // Check length and return buffer
	        if (buf.readableBytes() >= length) {
	            return buf.readBytes(length);
	        }

	        return null;
	    }
	} // end of class Gt06FrameDecoder
	
}
