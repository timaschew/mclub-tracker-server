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
public class T55TrackerServer extends TrackerServer{
	/**
	 * @param bootstrap
	 * @param protocol
	 * @param trackerDataService
	 */
    public T55TrackerServer(Bootstrap bootstrap, String protocol, TrackerDataService trackerDataService) {
	    super(bootstrap, protocol, trackerDataService);
    }

	/* (non-Javadoc)
	 * @see mclub.tracker.TrackerServer#addProtocolHandlers(org.jboss.netty.channel.ChannelPipeline)
	 */
    @Override
    protected void addProtocolHandlers(ChannelPipeline pipeline) {
    	byte delimiter[] = { (byte) '\r', (byte) '\n' };
        pipeline.addLast("frameDecoder",
                new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
        pipeline.addLast("stringDecoder", new StringDecoder());
        pipeline.addLast("stringEncoder", new StringEncoder());
        pipeline.addLast("objectDecoder", new T55ProtocolDecoder());	    
    }

    /**
     * Regular expressions pattern
     */
    static private Pattern pattern = Pattern.compile(
            "\\$GPRMC," +
            "(\\d{2})(\\d{2})(\\d{2})\\.(\\d+)," + // Time (HHMMSS.SSS)
            "([AV])," +                    // Validity
            "(\\d{2})(\\d{2}\\.\\d+)," +   // Latitude (DDMM.MMMM)
            "([NS])," +
            "(\\d{3})(\\d{2}\\.\\d+)," +   // Longitude (DDDMM.MMMM)
            "([EW])," +
            "(\\d+\\.?\\d*)?," +           // Speed
            "(\\d+\\.?\\d*)?," +           // Course
            "(\\d{2})(\\d{2})(\\d{2})" +   // Date (DDMMYY)
            ".+");                         // Other (Checksumm)

    /**
     *  T55ProtocolDecoder for netty
     */
    static class T55ProtocolDecoder extends OneToOneDecoder{
    	private Logger log = LoggerFactory.getLogger(T55TrackerServer.class);
    
    	private String imei;
    	
        @Override
        protected Object decode(
                ChannelHandlerContext ctx, Channel channel, Object msg)
                throws Exception {

            String sentence = (String) msg;

            try{
            	 // Detect device unique ID
                if (sentence.contains("$PGID")) {
                    imei = sentence.substring(6, sentence.length() - 3);
                }

                // Parse message
                else if (sentence.contains("$GPRMC") && imei != null) {
                    // Send response
                    if (channel != null) {
                        channel.write("OK1\r\n");
                    }

                    // Parse message
                    Matcher parser = pattern.matcher(sentence);
                    if (!parser.matches()) {
                        return null;
                    }

                    // Create new position
                    PositionData position = new PositionData();
                    position.setUdid(imei);

                    Integer index = 1;

                    // Time
                    Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    time.clear();
                    time.set(Calendar.HOUR, Integer.valueOf(parser.group(index++)));
                    time.set(Calendar.MINUTE, Integer.valueOf(parser.group(index++)));
                    time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
                    index += 1; // Skip milliseconds

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

                    // Speed
                    String speed = parser.group(index++);
                    if (speed != null) {
                        position.setSpeed(Double.valueOf(speed));
                    } else {
                        position.setSpeed(new Double(-1));
                    }

                    // Course
                    String course = parser.group(index++);
                    if (course != null) {
                        position.setCourse(Double.valueOf(course));
                    } else {
                        position.setCourse(0.0);
                    }

                    // Date
                    time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
                    time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
                    time.set(Calendar.YEAR, 2000 + Integer.valueOf(parser.group(index++)));
                    position.setTime(time.getTime());

                    // Altitude
                    position.setAltitude(0.0);

                    return position;
                }
            }catch(Exception e){
            	log.info("Failed to decode message: " + sentence, e);
            }
            return null;
        }

    }
}
