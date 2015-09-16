package mclub.tracker

import java.util.concurrent.ConcurrentHashMap;

import grails.converters.JSON
import grails.util.Environment
import grails.web.JSONBuilder

import javax.servlet.ServletContext
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener
import javax.servlet.annotation.WebListener
import javax.websocket.CloseReason
import javax.websocket.OnClose
import javax.websocket.OnError
import javax.websocket.OnMessage
import javax.websocket.OnOpen
import javax.websocket.Session
import javax.websocket.server.ServerContainer
import javax.websocket.server.ServerEndpoint

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes as GA
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@WebListener
@ServerEndpoint("/live0")
public class LivePositionWebsocketServer implements ServletContextListener, PositionChangeListener{
	private final Logger log = LoggerFactory.getLogger(getClass().name);
	private static GrailsApplication grailsApplication;
	
	//FIXME - use session store instead of the static hash set
	private static ConcurrentHashMap<String,Session> sessions = new ConcurrentHashMap<String,Session>();
	
	private TrackerService getTrackerService(){
		return grailsApplication?.getMainContext().getBean(TrackerService.class);
	}
	
	private TrackerDataService getTrackerDataService(){
		return grailsApplication?.getMainContext().getBean(TrackerDataService.class);
	}
	
	private PubSubService getPubSubService(){
		return grailsApplication?.getMainContext().getBean(PubSubService.class);
	}
	
	@Override
	public void contextInitialized(ServletContextEvent event) {
		ServletContext servletContext = event.servletContext
		final ServerContainer serverContainer = servletContext.getAttribute("javax.websocket.server.ServerContainer")
		try {

			if (true /*Environment.current == Environment.DEVELOPMENT*/) {
				serverContainer.addEndpoint(LivePositionWebsocketServer)
			}

			def ctx = servletContext.getAttribute(GA.APPLICATION_CONTEXT)
			grailsApplication = ctx.grailsApplication

			def config = grailsApplication.config
			int sessionIdleTimeout = config.liveposition.session_idle_timeout ?: 0
			serverContainer.defaultMaxSessionIdleTimeout = sessionIdleTimeout
			
			// register data changes
			getTrackerDataService()?.addChangeListener(this);
			
			sessions.clear();
			// get services
			log.info "LivePositionWebsocketServer initialized"
		}
		catch (IOException e) {
			log.error e.message, e
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		grailsApplication = null;
		log.info "LivePositionWebsocketServer destroyed";
	}
	

	@OnOpen
	public void handleOpen(Session clientSession) {
		sessions.put(clientSession.getId(), clientSession);
		log.debug "websocket session[${clientSession.id}] opened"
		
		//TODO read parameters from client
		boolean pushAllDataOnConnected = false;
		if(pushAllDataOnConnected){
			// push all tracker nodes
			def filter = new DeviceFilterCommand(udid:'all');
			def featureCollection = getTrackerService().listDeviceFeatureCollection(filter);
			def txt = featureCollection as JSON;
			try{
				clientSession.basicRemote.sendText(txt.toString());
			}catch(Exception e){
				// ignore
				log.error(e);
			}
		}
	}
	
	@OnMessage
	public String handleMessage(String message,Session clientSession) throws IOException {
		log.debug "Received: " + message
		return "echo [" + clientSession.getId() + "]"  + message;
		/*
		def myMsg=[:]
		JSONBuilder jSON = new JSONBuilder ()
		String username=(String) userSession.getUserProperties().get("username")
		if (!username) {
			userSession.getUserProperties().put("username", message)
			myMsg.put("message", "System:connected as ==>"+message)
			def aa=myMsg as JSON
			userSession.getBasicRemote().sendText(aa as String)
		}else{
			Iterator<Session> iterator=chatroomUsers.iterator()
			myMsg.put("message", "${username}:${message}")
			def aa=myMsg as JSON
			while (iterator.hasNext()) iterator.next().getBasicRemote().sendText(aa as String)
		}
		*/
	}
	
	@OnClose
	public void handeClose(Session clientSession, CloseReason reason) {
		sessions.remove(clientSession.getId());
		log.debug "websocket session[${clientSession.id}] closed"
	}
	
	@OnError
	public void handleError(Session clientSession, Throwable t) {
		try{
			clientSession.close();
		}catch(Exception e){
			// noop;
		}
		sessions.remove(clientSession.getId());
		log.error("websocket error", t);
	}

	@Override
	public void onPositionChanged(PositionData position) {
		log.trace("onPositionChange called, sessions: ${sessions}");
		// TODO filter out sessions according to the subscription record
		if(sessions.isEmpty()){
			return;
		}
		
		//def val = getTrackerService().buildDevicePositionValues(position);
		def val;
		
		if(true/*geojson*/)
			val = getTrackerService().getDeviceFeatureCollection(position.udid);
		else
			val = getTrackerService().getDeviceJsonData(position.udid);
		
		def txt = val as JSON;
		for(Session session : sessions.values()){
			try{
				session.basicRemote.sendText(txt.toString());
			}catch(Exception e){
				// ignore
				log.error("Error pushing position changes",e);
			}
		}
	}
		
}
