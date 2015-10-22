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

import java.util.Map;
import java.util.regex.Pattern;

import mclub.sys.ConfigService;
import mclub.tracker.PositionData;
import mclub.tracker.TrackerDataService;
import mclub.tracker.TrackerServer;
import mclub.tracker.protocol.helper.CharacterDelimiterFrameDecoder;
import mclub.tracker.protocol.helper.DateBuilder;
import mclub.tracker.protocol.helper.Parser;
import mclub.tracker.protocol.helper.PatternBuilder;

import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author shawn
 *
 */
public class Tk102TrackerServer extends TrackerServer {
	private static Logger log = LoggerFactory.getLogger(Tk102TrackerServer.class);

	/**
	 * @param bootstrap
	 * @param protocol
	 * @param trackerDataService
	 */
	public Tk102TrackerServer(Bootstrap bootstrap, String protocol,
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
		pipeline.addLast("frameDecoder", new CharacterDelimiterFrameDecoder(1024, ']'));
		pipeline.addLast("stringDecoder", new StringDecoder());
		pipeline.addLast("stringEncoder", new StringEncoder());
		pipeline.addLast("objectDecoder", new Tk102ProtocolDecoder());
	}

	static class Tk102ProtocolDecoder extends OneToOneDecoder{
    	private String udid;
    	
    	/*
    	 * Decode the incoming tracker messages
    	 */
		protected Object decode(ChannelHandlerContext ctx, Channel channel,
				Object msg) throws Exception {
			String sentence = (String) msg;

			if (sentence.startsWith("[!")) {
				udid = sentence.substring(14, 14 + 15);
				// TODO - should check the device id
				if (channel != null) {
					channel.write("[”0000000001" + sentence.substring(13) + "]");
				}
			} else if (hasDeviceId()) {
				Parser parser = new Parser(PATTERN, sentence);
				if (!parser.matches()) {
					log.info("Parsing error, message: " + sentence);
					return null;
				}
				try {
					PositionData position = new PositionData();

					// Extended info from GPS103 protocol
					Map<String, Object> extendedInfo = position
							.getExtendedInfo();
					extendedInfo.put("protocol", "tk102");
					// position.setProtocol("tk102");

					position.setUdid(getDeviceId());

					DateBuilder dateBuilder = new DateBuilder().setTime(
							parser.nextInt(), parser.nextInt(),
							parser.nextInt());

					position.setValid(parser.next().equals("A"));
					position.setLatitude(parser.nextCoordinate());
					position.setLongitude(parser.nextCoordinate());
					position.setSpeed(parser.nextDouble());

					dateBuilder.setDateReverse(parser.nextInt(),
							parser.nextInt(), parser.nextInt());
					position.setTime(dateBuilder.getDate());

					return position;
				} catch (Exception e) {
					log.info("Failed decode tk102 message: " + sentence, e);
				}
			}
			return null;
		}
    	
    	private boolean hasDeviceId(){
    		return udid != null;
    	}
    	private String getDeviceId(){
    		return udid;
    	}
	}
	

	/**
	 * Regular expressions pattern
	 */
    private static final Pattern PATTERN = new PatternBuilder()
	    .txt("[")
	    .xpr(".")
	    .num("d{10}")
	    .xpr(".")
	    .txt("(")
	    .xpr("[A-Z]+")
	    .num("(dd)(dd)(dd)")     // Time (HHMMSS)
	    .xpr("([AV])")                     // Validity
	    .num("(dd)(dd.dddd)([NS])")  // Latitude (DDMM.MMMM)
	    .num("(ddd)(dd.dddd)([EW])")  // Longitude (DDDMM.MMMM)
	    .num("(ddd.ddd)")          // Speed
	    .num("(dd)(dd)(dd)")   // Date (DDMMYY)
	    .num("d+")
	    .any()
	    .txt(")")
	    .opt("]")
	    .compile();
}
