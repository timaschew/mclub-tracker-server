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

import mclub.sys.ConfigService;
import mclub.tracker.PositionData;
import mclub.tracker.TrackerDataService;
import mclub.tracker.TrackerServer;

/**
 * @author shawn
 *
 */
public class Gps103TrackerServer extends TrackerServer{
	private Logger log = LoggerFactory.getLogger(Gps103TrackerServer.class);
	
	/**
	 * @param bootstrap
	 * @param protocol
	 * @param trackerDataService
	 */
    public Gps103TrackerServer(Bootstrap bootstrap, String protocol, TrackerDataService trackerDataService, ConfigService configService) {
	    super(bootstrap, protocol, trackerDataService,configService);
    }

	/* (non-Javadoc)
	 * @see mclub.tracker.TrackerServer#addProtocolHandlers(org.jboss.netty.channel.ChannelPipeline)
	 */
    @Override
    protected void addProtocolHandlers(ChannelPipeline pipeline) {
    	 byte delimiter[] = { (byte) ';' };
         pipeline.addLast("frameDecoder",
                 new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
         pipeline.addLast("stringDecoder", new StringDecoder());
         pipeline.addLast("stringEncoder", new StringEncoder());
         pipeline.addLast("objectDecoder", new OneToOneDecoder(){
             protected Object decode(
                     ChannelHandlerContext ctx, Channel channel, Object msg)
                     throws Exception {
             	return Gps103TrackerServer.this.decode(ctx, channel, msg);
             }
         });
    }


    /*
     * Decode the incoming tracker messages
     */
    protected Object decode(
            ChannelHandlerContext ctx, Channel channel, Object msg)
            throws Exception {

        String sentence = (String) msg;

        // Send response #1
        if (sentence.contains("##")) {
            if (channel != null) {
                channel.write("LOAD");
            }
            return null;
        }

        // Send response #2
        if (sentence.length() == 15 && Character.isDigit(sentence.charAt(0))) {
            if (channel != null) {
                channel.write("ON");
            }
            return null;
        }

        // Parse message
        Matcher parser = pattern.matcher(sentence);
        if (!parser.matches()) {
            log.info("Parsing error, message: " + sentence);
            return null;
        }

        // Create new position
        PositionData position = new PositionData();
        
        Integer index = 1;

        // Get device by IMEI
        String imei = parser.group(index++);
        position.setUdid(imei);

        String alarm =  parser.group(index++);
        
        // Date
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        time.clear();
        time.set(Calendar.YEAR, 2000 + Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MONTH, Integer.valueOf(parser.group(index++)) - 1);
        time.set(Calendar.DAY_OF_MONTH, Integer.valueOf(parser.group(index++)));
        
        int localHours = Integer.valueOf(parser.group(index++));
        int localMinutes = Integer.valueOf(parser.group(index++));
        
        int utcHours = Integer.valueOf(parser.group(index++));
        int utcMinutes = Integer.valueOf(parser.group(index++));

        // Time
        time.set(Calendar.HOUR, localHours);
        time.set(Calendar.MINUTE, localMinutes);
        time.set(Calendar.SECOND, Integer.valueOf(parser.group(index++)));
        time.set(Calendar.MILLISECOND, Integer.valueOf(parser.group(index++)));
        
        // Timezone calculation
        int deltaMinutes = (localHours - utcHours) * 60 + localMinutes - utcMinutes;
        if (deltaMinutes <= -12 * 60) {
            deltaMinutes += 24 * 60;
        } else if (deltaMinutes > 12 * 60) {
            deltaMinutes -= 24 * 60;
        }
        time.add(Calendar.MINUTE, -deltaMinutes);
        position.setTime(time.getTime());

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
        String hemisphere = parser.group(index++);
        if (hemisphere != null) {
            if (hemisphere.compareTo("W") == 0) lonlitude = -lonlitude;
        }
        position.setLongitude(lonlitude);

        // Altitude
        position.setAltitude(-1d);

        // Speed
        position.setSpeed(Double.valueOf(parser.group(index++)));

        // Course
        String course = parser.group(index++);
        if (course != null) {
            position.setCourse(Double.valueOf(course));
        } else {
            position.setCourse(-1d);
        }

        // Extended info from GPS103 protocol
        Map<String,Object> extendedInfo = position.getExtendedInfo();
        extendedInfo.put("protocol", "gps103");
        // Alarm message
        if(alarm != null)
        	extendedInfo.put("alarm", alarm);
        
        return position;
    }
    
    
    /**
     * Regular expressions pattern
     */
    static private Pattern pattern = Pattern.compile(
            "imei:" +
            "(\\d+)," +                         // IMEI
            "([^,]+)," +                        // Alarm
            "(\\d{2})/?(\\d{2})/?(\\d{2})\\s?" + // Local Date
            "(\\d{2}):?(\\d{2})," +             // Local Time
            "[^,]*," +
            "[FL]," +                           // F - full / L - low
            "(\\d{2})(\\d{2})(\\d{2})\\.(\\d{3})," + // Time UTC (HHMMSS.SSS)
            "([AV])," +                         // Validity
            "(\\d{2})(\\d{2}\\.\\d{4})," +      // Latitude (DDMM.MMMM)
            "([NS])," +
            "(\\d{3})(\\d{2}\\.\\d{4})," +      // Longitude (DDDMM.MMMM)
            "([EW])?," +
            "(\\d+\\.?\\d*)," +                 // Speed
            "(\\d+\\.\\d+)?" +                  // Course
            ".*");
    
}
