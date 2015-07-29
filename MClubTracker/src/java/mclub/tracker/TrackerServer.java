/**
 * Project: CarTracServer
 * 
 * File Created at Apr 5, 2013
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
package mclub.tracker;

import java.net.InetSocketAddress;
import java.nio.ByteOrder;

import mclub.tracker.geocode.GoogleReverseGeocoder;
import mclub.tracker.geocode.ReverseGeocoder;
import mclub.tracker.geocode.ReverseGeocoderHandler;

import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.jboss.netty.channel.Channel;
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
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author shawn
 *
 */
public abstract class TrackerServer {
    private Bootstrap bootstrap;
    private String protocol;
    private Logger log = LoggerFactory.getLogger(getClass());
    
    private Boolean loggerEnabled, reverseGeocoderEnabled;
    private Integer resetDelay;
    private ReverseGeocoder reverseGeocoder;
    
    private TrackerDataService trackerDataService;

    /********************************************************************************/
    /* Dependencies */
    /********************************************************************************/
    public String getProtocol() {
        return protocol;
    }    
    public TrackerDataService getTrackerDataService(){
    	return trackerDataService;
    }
    

    public TrackerServer(Bootstrap bootstrap, String protocol, TrackerDataService trackerDataService) {
    	
        this.bootstrap = bootstrap;
        this.protocol = protocol;
        this.trackerDataService = trackerDataService;
        
        // Set appropriate channel factory
        if (bootstrap instanceof ServerBootstrap) {
            bootstrap.setFactory(NettyResource.getChannelFactory());
        } else if (bootstrap instanceof ConnectionlessBootstrap) {
            bootstrap.setFactory(NettyResource.getDatagramChannelFactory());
        }

        address = (String)trackerDataService.getConfig("tracker." + protocol + ".address");
        port = (Integer)trackerDataService.getConfig("tracker." + protocol + ".port");
        if(port == null){
        	port = 5000;
        }

        // enable logger
        loggerEnabled = Boolean.TRUE.equals(trackerDataService.getConfig().get("tracker.logger.enabled"));
        // enable geocoder        
        reverseGeocoderEnabled = Boolean.TRUE.equals(trackerDataService.getConfig().get("tracker.geocoder.enabled")); 
        if(reverseGeocoderEnabled){
        	reverseGeocoder = new GoogleReverseGeocoder();
        }
        
        // enable tracker reset delay
        String resetDelayProperty = (String)trackerDataService.getConfig().get("tracker." + protocol + ".resetDelay");
        if (resetDelayProperty != null) {
            resetDelay = Integer.valueOf(resetDelayProperty);
        }
        
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = Channels.pipeline();
		        if (resetDelay != null) {
		            pipeline.addLast("idleHandler", new IdleStateHandler(NettyResource.getTimer(), resetDelay, 0, 0));
		        }
		        pipeline.addLast("openHandler", new OpenChannelHandler());
		        if (loggerEnabled) {
		            pipeline.addLast("logger", new StandardLoggingHandler());
		        }
		        addProtocolHandlers(pipeline);
		        if (reverseGeocoder != null) {
		            pipeline.addLast("geocoder", new ReverseGeocoderHandler(reverseGeocoder));
		        }
		        pipeline.addLast("handler", new TrackerEventHandler(getTrackerDataService()));
		        return pipeline;
			}
        });
    }

    protected abstract void addProtocolHandlers(ChannelPipeline pipeline);

    /********************************************************************************/
    /* Netty PipelineFactory and Handlers */
    /********************************************************************************/
    
    /**
     * Open channel handler
     */
    protected class OpenChannelHandler extends SimpleChannelHandler {
        @Override
        public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
            getChannelGroup().add(e.getChannel());
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
    
    
    /********************************************************************************/
    /* Properties */
    /********************************************************************************/
    /*
     * Server socket port
     */
    private Integer port;

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    /*
     * Server socket address
     */
    private String address;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Set endianness
     */
    void setEndianness(ByteOrder byteOrder) {
        bootstrap.setOption("child.bufferFactory", new HeapChannelBufferFactory(byteOrder));
    }

    /**
     * Opened channels
     */
    private ChannelGroup allChannels = new DefaultChannelGroup();

    public ChannelGroup getChannelGroup() {
        return allChannels;
    }

    public void setPipelineFactory(ChannelPipelineFactory pipelineFactory) {
        bootstrap.setPipelineFactory(pipelineFactory);
    }

    /********************************************************************************/
    /* Server life cycle */
    /********************************************************************************/
    
    /**
     * Start server
     */
    public void start() {
        InetSocketAddress endpoint;
        if (address == null) {
            endpoint = new InetSocketAddress(port);
        } else {
            endpoint = new InetSocketAddress(address, port);
        }

        Channel channel = null;
        if (bootstrap instanceof ServerBootstrap) {
            channel = ((ServerBootstrap) bootstrap).bind(endpoint);
        } else if (bootstrap instanceof ConnectionlessBootstrap) {
            channel = ((ConnectionlessBootstrap) bootstrap).bind(endpoint);
        }

        if (channel != null) {
            getChannelGroup().add(channel);
        }
        
        log.info(" " + this + " started");
    }

    /**
     * Stop server
     */
    public void stop() {
        ChannelGroupFuture future = getChannelGroup().close();
        future.awaitUninterruptibly();
        
        log.info(" " + this + " stopped");
    }
    
    public String toString(){
    	return "[" + protocol + "] server@" + Integer.toHexString(System.identityHashCode(this)); 
    }
}
