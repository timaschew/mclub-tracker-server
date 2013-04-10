package mclub.tracker;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import mclub.tracker.protocol.Gps103ProtocolDecoder;
import mclub.tracker.protocol.T55ProtocolDecoder;
import mclub.tracker.protocol.Tk103ProtocolDecoder;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




/**
 * Manages the servers that receive data from all kind of car trackers.
 * @author shawn
 *
 */
public class TrackerServerManager {
	Logger log = LoggerFactory.getLogger(TrackerServerManager.class);
	TrackerDataService trackerDataService;
	
	private List<TrackerServer> serverList = new ArrayList<TrackerServer>();
	
	@PostConstruct
	public void startup(){
		log.info("Startup");
		
		initServers();
		
		for (TrackerServer server: serverList) {
			log.info(" Starting " + server);
            server.start();
        }
	}
	
	@PreDestroy
	public void shutdown(){
		log.info("Shutdown");
		for (TrackerServer server: serverList) {
			log.info(" Stopping " + server);
            server.stop();
        }
		
		// Release resources
		NettyResource.release();
	}
	
	private void initServers(){
		initTk103Server();
		initGps103Server();
		initT55Server();
	}
	
	private void initTk103Server() {
		String protocol = "tk103";
		if (!isProtocolEnabled(protocol)){
			return;
		}
		try{
			TrackerServer server = new TrackerServer(new ServerBootstrap(), protocol, trackerDataService) {
	            @Override
	            protected void addSpecificHandlers(ChannelPipeline pipeline) {
	                byte delimiter[] = { (byte) ')' };
	                pipeline.addLast("frameDecoder",
	                        new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
	                pipeline.addLast("stringDecoder", new StringDecoder());
	                pipeline.addLast("stringEncoder", new StringEncoder());
	                Tk103ProtocolDecoder tk103 = new Tk103ProtocolDecoder(trackerDataService);
	                pipeline.addLast("objectDecoder", tk103);
	            }
	        };
	        serverList.add(server);
		}catch(Exception e){
			log.error("Error initialize " + protocol + " server, " + e.getMessage(),e);
		}
    }
	
	private void initGps103Server(){
		String protocol = "gps103";
		if (!isProtocolEnabled(protocol)){
			return;
		}
		serverList.add(new TrackerServer(new ServerBootstrap(), protocol,trackerDataService) {
            @Override
            protected void addSpecificHandlers(ChannelPipeline pipeline) {
                byte delimiter[] = { (byte) ';' };
                pipeline.addLast("frameDecoder",
                        new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                pipeline.addLast("stringDecoder", new StringDecoder());
                pipeline.addLast("stringEncoder", new StringEncoder());
                pipeline.addLast("objectDecoder", new Gps103ProtocolDecoder(trackerDataService));
            }
        });
	}
	
	private void initT55Server(){
		String protocol = "t55";
		if (!isProtocolEnabled(protocol)){
			return;
		}
		serverList.add(new TrackerServer(new ServerBootstrap(), protocol,trackerDataService) {
            @Override
            protected void addSpecificHandlers(ChannelPipeline pipeline) {
                byte delimiter[] = { (byte) '\r', (byte) '\n' };
                pipeline.addLast("frameDecoder",
                        new DelimiterBasedFrameDecoder(1024, ChannelBuffers.wrappedBuffer(delimiter)));
                pipeline.addLast("stringDecoder", new StringDecoder());
                pipeline.addLast("stringEncoder", new StringEncoder());
                pipeline.addLast("objectDecoder", new T55ProtocolDecoder(trackerDataService));
            }
        });
	}
	
	/**
	 * read from configurations
	 * @param protocol
	 * @return
	 */
	private boolean isProtocolEnabled(String protocol){
		return Boolean.TRUE.equals(trackerDataService.getConfig("tracker."+protocol+".enabled"));
	}
	
	public void setTrackerDataService(TrackerDataService ts){
		this.trackerDataService = ts;
	}
}
