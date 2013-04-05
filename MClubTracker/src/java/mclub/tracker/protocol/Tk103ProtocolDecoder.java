/*
 * Copyright 2012 - 2013 Anton Tananaev (anton.tananaev@gmail.com)
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

import mclub.tracker.TrackerPosition;
import mclub.tracker.TrackerService;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tk103ProtocolDecoder extends OneToOneDecoder{
	TrackerService trackerService;
	
	private Logger log = LoggerFactory.getLogger(Tk103ProtocolDecoder.class);
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

    public Tk103ProtocolDecoder(TrackerService tracService){
    	this.trackerService = tracService;
    }
    
    @Override
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

        // Create new position
        TrackerPosition position = new TrackerPosition();
        StringBuilder extendedInfo = new StringBuilder("<protocol>tk103</protocol>");
        Integer index = 1;

        // Get device by IMEI
        String imei = parser.group(index++);
        Long deviceRecordId = trackerService.getIdByUniqueDeviceId(imei);
        if(deviceRecordId == null){
        	log.warn("Unknown device - " + imei);
        	return null;
        }
        position.setDeviceId(deviceRecordId);

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
        
        // State
        extendedInfo.append("<state>");
        extendedInfo.append(parser.group(index++));
        extendedInfo.append("</state>");

        // Milage
        extendedInfo.append("<milage>");
        extendedInfo.append(Integer.parseInt(parser.group(index++), 16));
        extendedInfo.append("</milage>");

        position.setExtendedInfo(extendedInfo.toString());
        return position;
    }

}
