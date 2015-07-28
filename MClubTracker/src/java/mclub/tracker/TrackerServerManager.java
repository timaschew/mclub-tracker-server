package mclub.tracker;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import mclub.tracker.protocol.Gps103TrackerServer;
import mclub.tracker.protocol.T55TrackerServer;
import mclub.tracker.protocol.Tk103TrackerServer;

import org.jboss.netty.bootstrap.ServerBootstrap;
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
			TrackerServer server = new Tk103TrackerServer(new ServerBootstrap(), protocol, trackerDataService);
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
		try{
			serverList.add(new Gps103TrackerServer(new ServerBootstrap(), protocol,trackerDataService));
		}catch(Exception e){
			log.error("Error initialize " + protocol + " server, " + e.getMessage(),e);
		}
	}
	
	private void initT55Server(){
		String protocol = "t55";
		if (!isProtocolEnabled(protocol)){
			return;
		}
		try{
			serverList.add(new T55TrackerServer(new ServerBootstrap(), protocol,trackerDataService));
		}catch(Exception e){
			log.error("Error initialize " + protocol + " server, " + e.getMessage(),e);
		}
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
