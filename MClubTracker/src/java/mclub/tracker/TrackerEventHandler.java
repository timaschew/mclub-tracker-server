/*
 * Copyright 2012 Anton Tananaev (anton.tananaev@gmail.com)
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
package mclub.tracker;

import java.util.List;

import mclub.tracker.TrackerPosition;
import mclub.tracker.TrackerService;

import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.jboss.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tracker message handler
 */
@ChannelHandler.Sharable
public class TrackerEventHandler extends IdleStateAwareChannelHandler {
	private Logger log = LoggerFactory.getLogger(TrackerEventHandler.class);
	private TrackerService trackerService;
	
    TrackerEventHandler(TrackerService tracService) {
        super();
        this.trackerService = tracService;
        assert tracService != null;
    }
    
    public void setTracService(TrackerService tracService){
    	this.trackerService = tracService;
    }

    private void processSinglePosition(TrackerPosition position) {
        if (position == null) {
        	if(log.isInfoEnabled())
        		log.info("null message");
        } else {
        	if(log.isInfoEnabled()){
        		log.info(
                    "deviceId: " + position.getDeviceId() +
                    ", valid: " + position.getValid() +
                    ", time: " + position.getTime() +
                    ", latitude: " + position.getLatitude() +
                    ", longitude: " + position.getLongitude() +
                    ", altitude: " + position.getAltitude() +
                    ", speed: " + position.getSpeed() +
                    ", course: " + position.getCourse() +
                    ", power: " + position.getPower());
        	}
        }

        // Write position to database
        try {
            Long id = trackerService.addPosition(position);
            if (id != null) {
            	trackerService.updateLatestPosition(position.getDeviceId(), id);
            }
        } catch (Exception error) {
            log.warn("save postion failed, " + error.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        if (e.getMessage() instanceof TrackerPosition) {
            processSinglePosition((TrackerPosition) e.getMessage());
        } else if (e.getMessage() instanceof List) {
            List<TrackerPosition> positions = (List<TrackerPosition>) e.getMessage();
            for (TrackerPosition position : positions) {
                processSinglePosition(position);
            }
        }
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        log.info("Closing connection by disconnect");
        e.getChannel().close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        log.info("Closing connection by exception");
        e.getChannel().close();
    }

    @Override
    public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e) {
        log.info("Closing connection by timeout");
        e.getChannel().close();
    }
}
