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

import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mclub.sys.ConfigService;
import mclub.tracker.PositionData;
import mclub.tracker.TrackerDataService;
import mclub.tracker.TrackerServer;
import mclub.tracker.protocol.helper.ChannelBufferTools;

import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
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
public class H02TrackerServer extends TrackerServer {
	private static Logger log = LoggerFactory.getLogger(H02TrackerServer.class);

	/**
	 * @param bootstrap
	 * @param protocol
	 * @param trackerDataService
	 */
	public H02TrackerServer(Bootstrap bootstrap, String protocol,
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
		pipeline.addLast("frameDecoder", new H02FrameDecoder());
		pipeline.addLast("objectDecoder", new H02ProtocolDecoder());
	}

	static class H02ProtocolDecoder extends OneToOneDecoder {
		String udid;

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

	    private static double readCoordinate(ChannelBuffer buf, boolean lon) {

	        int degrees = ChannelBufferTools.readHexInteger(buf, 2);
	        if (lon) {
	            degrees = degrees * 10 + (buf.getUnsignedByte(buf.readerIndex()) >> 4);
	        }

	        double result = 0;
	        if (lon) {
	            result = buf.readUnsignedByte() & 0x0f;
	        }
	        result = result * 10 + ChannelBufferTools.readHexInteger(buf, lon ? 5 : 6) * 0.0001;

	        result /= 60;
	        result += degrees;

	        return result;
	    }

	    private void processStatus(PositionData position, long status) {
	    	position.addExtendedInfo("status", status);
	    	
//	        if (!BitUtil.check(status, 0) || !BitUtil.check(status, 1) || !BitUtil.check(status, 3) || !BitUtil.check(status, 4)) {
//	            position.set(Event.KEY_ALARM, true);
//	        	//position.setMessageType(1);
//	        }
//	        position.set(Event.KEY_IGNITION, !BitUtil.check(status, 10));
//	        position.set(Event.KEY_STATUS, status);
	    }

	    private PositionData decodeBinary(ChannelBuffer buf, Channel channel) {

	        // Create new position
	        PositionData position = new PositionData();
	        position.addExtendedInfo("protocol", "H02");

	        buf.readByte(); // marker

	        // Identification
	        if (!identify(ChannelBufferTools.readHexString(buf, 10), channel)) {
	            return null;
	        }
	        position.setUdid(getDeviceId());

	        // Time
	        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	        time.clear();
	        time.set(Calendar.HOUR_OF_DAY, ChannelBufferTools.readHexInteger(buf, 2));
	        time.set(Calendar.MINUTE, ChannelBufferTools.readHexInteger(buf, 2));
	        time.set(Calendar.SECOND, ChannelBufferTools.readHexInteger(buf, 2));
	        time.set(Calendar.DAY_OF_MONTH, ChannelBufferTools.readHexInteger(buf, 2));
	        time.set(Calendar.MONTH, ChannelBufferTools.readHexInteger(buf, 2) - 1);
	        time.set(Calendar.YEAR, 2000 + ChannelBufferTools.readHexInteger(buf, 2));
	        position.setTime(time.getTime());

	        // Location
	        double latitude = readCoordinate(buf, false);
	        //position.setPower(Double...);
	        //position.set(Event.KEY_POWER, buf.readByte());
	        double longitude = readCoordinate(buf, true);
	        int flags = buf.readUnsignedByte() & 0x0f;
	        position.setValid((flags & 0x02) != 0);
	        if ((flags & 0x04) == 0) latitude = -latitude;
	        if ((flags & 0x08) == 0) longitude = -longitude;
	        position.setLatitude(latitude);
	        position.setLongitude(longitude);

	        position.setAltitude(-1d);
	        // Speed and course
	        Integer iSpd = ChannelBufferTools.readHexInteger(buf, 3);
	        position.setSpeed(iSpd.doubleValue());
	        position.setCourse((buf.readUnsignedByte() & 0x0f) * 100.0 + ChannelBufferTools.readHexInteger(buf, 2));

	        processStatus(position, buf.readUnsignedInt());
	        return position;
	    }

	    private static final Pattern PATTERN = Pattern.compile(
	            "\\*..," +                          // Manufacturer
	            "(\\d+)," +                         // IMEI
	            "V\\d," +                           // Version?
	            ".*" +
	            "(\\d{2})(\\d{2})(\\d{2})," +       // Time (HHMMSS)
	            "([AV])?," +                        // Validity
	            "-?(\\d+)-?(\\d{2}.\\d+)," +        // Latitude (DDMM.MMMM)
	            "([NS])," +
	            "-?(\\d+)-?(\\d{2}.\\d+)," +        // Longitude (DDMM.MMMM)
	            "([EW])," +
	            "(\\d+.?\\d*)," +                   // Speed
	            "(\\d+.?\\d*)?," +                  // Course
	            "(\\d{2})(\\d{2})(\\d{2})," +       // Date (DDMMYY)
	            "(\\p{XDigit}{8})" +                // Status
	            ".*");

	    private PositionData decodeText(String sentence, Channel channel) {

	        // Parse message
	        Matcher parser = PATTERN.matcher(sentence);
	        if (!parser.matches()) {
	            return null;
	        }

	        // Create new position
	        PositionData position = new PositionData();
	        position.addExtendedInfo("protocol", "H02");

	        Integer index = 1;

	        // Get device by IMEI
	        if (!identify(parser.group(index++), channel)) {
	            return null;
	        }
	        position.setUdid(getDeviceId());

	        // Time
	        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	        time.clear();
	        time.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parser.group(index++)));
	        time.set(Calendar.MINUTE, Integer.parseInt(parser.group(index++)));
	        time.set(Calendar.SECOND, Integer.parseInt(parser.group(index++)));

	        // Validity
	        String valid = parser.group(index++);
	        if (valid != null) {
	            position.setValid(valid.compareTo("A") == 0);
	        }

	        // Latitude
	        Double latitude = Double.parseDouble(parser.group(index++));
	        latitude += Double.parseDouble(parser.group(index++)) / 60;
	        if (parser.group(index++).compareTo("S") == 0) latitude = -latitude;
	        position.setLatitude(latitude);

	        // Longitude
	        Double longitude = Double.parseDouble(parser.group(index++));
	        longitude += Double.parseDouble(parser.group(index++)) / 60;
	        if (parser.group(index++).compareTo("W") == 0) longitude = -longitude;
	        position.setLongitude(longitude);

	        position.setAltitude(-1d);
	        
	        // Speed
	        position.setSpeed(Double.parseDouble(parser.group(index++)));

	        // Course
	        String course = parser.group(index++);
	        if (course != null) {
	            position.setCourse(Double.parseDouble(course));
	        }

	        // Date
	        time.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parser.group(index++)));
	        time.set(Calendar.MONTH, Integer.parseInt(parser.group(index++)) - 1);
	        time.set(Calendar.YEAR, 2000 + Integer.parseInt(parser.group(index++)));
	        position.setTime(time.getTime());

	        processStatus(position, Long.parseLong(parser.group(index++), 16));
	        return position;
	    }

		@Override
		protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
			ChannelBuffer buf = (ChannelBuffer) msg;
			String marker = buf.toString(0, 1, Charset.defaultCharset());

			// TODO X mode?

			if (marker.equals("*")) {
				return decodeText(buf.toString(Charset.defaultCharset()),
						channel);
			} else if (marker.equals("$")) {
				return decodeBinary(buf, channel);
			}

			return null;
		}

	} // end of class H02ProtocolDecoder
	
	/**
	 * Frame decoder
	 */
	static class H02FrameDecoder extends FrameDecoder {
		//private static Logger log = LoggerFactory.getLogger(H02FrameDecoder.class);
	    private static final int MESSAGE_LENGTH = 32;
	    @Override
	    protected Object decode(
	            ChannelHandlerContext ctx,
	            Channel channel,
	            ChannelBuffer buf) throws Exception {

	        String marker = buf.toString(buf.readerIndex(), 1, Charset.defaultCharset());

	        while (!marker.equals("*") && !marker.equals("$") && buf.readableBytes() > 0) {
	            buf.skipBytes(1);
	            if (buf.readableBytes() > 0) {
	                marker = buf.toString(buf.readerIndex(), 1, Charset.defaultCharset());
	            }
	        }

	        if (marker.equals("*")) {

	            // Return text message
	            Integer index = ChannelBufferTools.find(buf, buf.readerIndex(), buf.readableBytes(), "#");
	            if (index != null) {
	                return buf.readBytes(index + 1 - buf.readerIndex());
	            }

	        } else if (marker.equals("$") && buf.readableBytes() >= MESSAGE_LENGTH) {

	            // Return binary message
	            return buf.readBytes(MESSAGE_LENGTH);

	        }

	        return null;
	    }

	} // end of class H02FrameDecoder
	
}
