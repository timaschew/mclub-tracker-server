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

import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mclub.tracker.PositionData;
import mclub.tracker.TrackerDataService;
import mclub.tracker.TrackerServer;

/**
 * @author shawn
 *
 */
public class Tk103TrackerServer extends TrackerServer{
	private Logger log = LoggerFactory.getLogger(Tk103TrackerServer.class);
	
	/**
	 * @param bootstrap
	 * @param protocol
	 * @param trackerDataService
	 */
    public Tk103TrackerServer(Bootstrap bootstrap, String protocol, TrackerDataService trackerDataService) {
	    super(bootstrap, protocol, trackerDataService);
    }

	/* (non-Javadoc)
	 * @see mclub.tracker.TrackerServer#addProtocolHandlers(org.jboss.netty.channel.ChannelPipeline)
	 */
    @Override
    protected void addProtocolHandlers(ChannelPipeline pipeline) {
    	byte delimiter[] = { (byte) ')' };
        pipeline.addLast("frameDecoder",
                new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
        pipeline.addLast("stringDecoder", new StringDecoder());
        pipeline.addLast("stringEncoder", new StringEncoder());
        pipeline.addLast("objectDecoder", new OneToOneDecoder(){
			protected Object decode(ChannelHandlerContext ctx, Channel channel,
					Object msg) throws Exception {
				return Tk103TrackerServer.this.decode(ctx, channel, msg);
			}
        });	    
    }

    /*
     * decode received setence to object
     */
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        String sentence = (String) msg;

        // Find message start
        int beginIndex = sentence.indexOf('(');
        if (beginIndex != -1) {
            sentence = sentence.substring(beginIndex + 1);
        }

        // TODO: Send answer?
        //(090411121854AP05)

        // Parse message
        Matcher parser = pattern.matcher(sentence);
        if (!parser.matches()) {
            return null;
        }

        try{
            // Create new position
            PositionData position = new PositionData();
            Integer index = 1;

            // Get device by IMEI
            String imei = parser.group(index++);
            position.setUdid(imei);
            position.setImei(imei);
            
            // Date
            Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            time.clear();
            time.set(Calendar.YEAR, 2000 + Integer.valueOf(parser.group(index++)));
            time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
            time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));

            // Validity
            position.setValid(parser.group(index++).compareTo("A") == 0 ? true : false);

            // Latitude
            Double latitude = Double.valueOf(parser.group(index++));
            latitude += Double.valueOf(parser.group(index++)) / 60;
            if (parser.group(index++).compareTo("S") == 0) latitude = -latitude;
            position.setLatitude(latitude);

            // Longitude
            Double lonlitude = Double.valueOf(parser.group(index++));
            lonlitude += Double.valueOf(parser.group(index++)) / 60;
            if (parser.group(index++).compareTo("W") == 0) lonlitude = -lonlitude;
            position.setLongitude(lonlitude);

            // Altitude
            position.setAltitude(0.0);

            // Speed
            position.setSpeed(Double.valueOf(parser.group(index++)));

            // Time
            time.set(Calendar.HOUR, Integer.valueOf(parser.group(index++)));
            time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
            time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
            position.setTime(time.getTime());

            // Course
            position.setCourse(Double.valueOf(parser.group(index++)));
            
            Map<String,Object> extendedInfo = position.getExtendedInfo();
            extendedInfo.put("protocol", "tk103");
            // State
            extendedInfo.put("state",parser.group(index++));

            // Mileage
            extendedInfo.put("milage",Integer.parseInt(parser.group(index++), 16));
            return position;
        }catch(Exception e){
        	log.info("Failed decode tk103 message: " + sentence,e);
        	return null;
        }
    }
    
    /**
     * Regular expressions pattern
     */
    static private Pattern pattern = Pattern.compile(
            "(\\d{12})" +                // Device ID
            ".{4}" +                     // Command
            "\\d*" +                     // IMEI (?)
            "(\\d{2})(\\d{2})(\\d{2})" + // Date (YYMMDD)
            "([AV])" +                   // Validity
            "(\\d{2})(\\d{2}\\.\\d{4})" + // Latitude (DDMM.MMMM)
            "([NS])" +
            "(\\d{3})(\\d{2}\\.\\d{4})" + // Longitude (DDDMM.MMMM)
            "([EW])" +
            "(\\d+\\.\\d)" +             // Speed
            "(\\d{2})(\\d{2})(\\d{2})" + // Time (HHMMSS)
            "(\\d+\\.\\d+)" +            // Course
            "(\\d{8})" +                 // State
            "L([0-9a-fA-F]+)");          // Milage
}
