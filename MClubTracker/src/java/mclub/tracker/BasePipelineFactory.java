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
package mclub.tracker;

import java.net.InetSocketAddress;

import mclub.tracker.geocode.GoogleReverseGeocoder;
import mclub.tracker.geocode.ReverseGeocoder;
import mclub.tracker.geocode.ReverseGeocoderHandler;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
  * Base pipeline factory
  */
public abstract class BasePipelineFactory implements ChannelPipelineFactory {
	private Logger log = LoggerFactory.getLogger(BasePipelineFactory.class);
	
    private TrackerServer server;
    private Boolean loggerEnabled;
    private Integer resetDelay;
    private ReverseGeocoder reverseGeocoder;

    /**
     * Open channel handler
     */
    protected class OpenChannelHandler extends SimpleChannelHandler {

        private TrackerServer server;

        public OpenChannelHandler(TrackerServer server) {
            this.server = server;
        }

        @Override
        public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
            server.getChannelGroup().add(e.getChannel());
        }
    }

    /**
     * Logging using global logger
     */
    protected class StandardLoggingHandler extends LoggingHandler {

        @Override
        public void log(ChannelEvent e) {
            if (e instanceof MessageEvent) {
                MessageEvent event = (MessageEvent) e;
                StringBuilder msg = new StringBuilder();

                msg.append("[").append(((InetSocketAddress) e.getChannel().getLocalAddress()).getPort());
                msg.append((e instanceof DownstreamMessageEvent) ? " -> " : " <- ");
                msg.append(((InetSocketAddress) event.getRemoteAddress()).getAddress().getHostAddress()).append("]");

                // Append hex message
                if (event.getMessage() instanceof ChannelBuffer) {
                    msg.append(" - (HEX: ");
                    msg.append(ChannelBuffers.hexDump((ChannelBuffer) event.getMessage()));
                    msg.append(")");
                }

                log.debug(msg.toString());
            } else if (e instanceof ExceptionEvent) {
                ExceptionEvent event = (ExceptionEvent) e;
                log.warn(event.getCause().toString());
            }
        }

    }

    public BasePipelineFactory(TrackerServer server, String protocol) {
        this.server = server;
        
        // enable logger
        loggerEnabled = Boolean.TRUE.equals(server.getTrackerService().getConfig().get("tracker.logger.enabled"));
        
        // enable geocoder 
        if(Boolean.TRUE.equals(server.getTrackerService().getConfig().get("tracker.geocoder.enabled"))){
        	reverseGeocoder = new GoogleReverseGeocoder();
        }
        
        // enable tracker reset delay
        String resetDelayProperty = (String)server.getTrackerService().getConfig().get("tracker." + protocol + ".resetDelay");
        if (resetDelayProperty != null) {
            resetDelay = Integer.valueOf(resetDelayProperty);
        }
    }

    protected abstract void addSpecificHandlers(ChannelPipeline pipeline);

    @Override
    public ChannelPipeline getPipeline() {
        ChannelPipeline pipeline = Channels.pipeline();
        if (resetDelay != null) {
            pipeline.addLast("idleHandler", new IdleStateHandler(NettyResource.getTimer(), resetDelay, 0, 0));
        }
        pipeline.addLast("openHandler", new OpenChannelHandler(server));
        if (loggerEnabled) {
            pipeline.addLast("logger", new StandardLoggingHandler());
        }
        addSpecificHandlers(pipeline);
        if (reverseGeocoder != null) {
            pipeline.addLast("geocoder", new ReverseGeocoderHandler(reverseGeocoder));
        }
        pipeline.addLast("handler", new TrackerEventHandler(server.getTrackerService()));
        return pipeline;
    }

}
