package mclub.tracker

import grails.converters.JSON
import grails.util.Environment

import java.util.concurrent.ConcurrentHashMap

import javax.servlet.ServletContext
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener
import javax.servlet.annotation.WebListener
import javax.websocket.CloseReason
import javax.websocket.EndpointConfig
import javax.websocket.OnClose
import javax.websocket.OnError
import javax.websocket.OnMessage
import javax.websocket.OnOpen
import javax.websocket.RemoteEndpoint
import javax.websocket.SendHandler
import javax.websocket.Session
import javax.websocket.server.ServerContainer
import javax.websocket.server.ServerEndpoint

import mclub.sys.MessageListener
import mclub.sys.MessageService

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes as GA
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@WebListener
@ServerEndpoint("/live0")
public class LivePositionWebsocketServer implements ServletContextListener, MessageListener{
	private final Logger log = LoggerFactory.getLogger(getClass().name);
	private static GrailsApplication grailsApplication;
	
	//FIXME - use session store instead of the static hash set
	private static ConcurrentHashMap<String,SessionEntry> sessions = new ConcurrentHashMap<String,Session>();
	
	private TrackerService getTrackerService(){
		return grailsApplication?.getMainContext().getBean(TrackerService.class);
	}
	
	private TrackerDataService getTrackerDataService(){
		return grailsApplication?.getMainContext().getBean(TrackerDataService.class);
	}
	
	private PubSubService getPubSubService(){
		return grailsApplication?.getMainContext().getBean(PubSubService.class);
	}
	
	private MessageService getMessageService(){
		return grailsApplication?.getMainContext().getBean(MessageService.class);
	}
	
	@Override
	public void contextInitialized(ServletContextEvent event) {
		ServletContext servletContext = event.servletContext
		final ServerContainer serverContainer = servletContext.getAttribute("javax.websocket.server.ServerContainer")
		try {

			if (Environment.current == Environment.DEVELOPMENT) {
				serverContainer.addEndpoint(LivePositionWebsocketServer)
			}

			def ctx = servletContext.getAttribute(GA.APPLICATION_CONTEXT)
			grailsApplication = ctx.grailsApplication

			def config = grailsApplication.config
			long sessionIdleTimeout = config.liveposition.session_idle_timeout ?: 15000 // idle timeout is 15s
			serverContainer.defaultMaxSessionIdleTimeout = sessionIdleTimeout
			
			// register data changes
			getMessageService()?.addListener(this);
			
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
		getMessageService()?.removeListener(this);
		grailsApplication = null;
		log.info "LivePositionWebsocketServer destroyed";
	}
	

	@OnOpen
	public void handleOpen(Session clientSession, EndpointConfig config) {
		SessionEntry sessionEntry = new SessionEntry();
		sessionEntry.session = clientSession;
		TrackerDeviceFilter  filter;
		Map<String,List<String>> params = clientSession.getRequestParameterMap();
		if(params.size() > 0){
			//TODO - check token for excessive usage
			String token = null;
			if(params.get('token')){
				token = params.get('token')[0];
			}
			
			//Process the id and token parameters from URL
			String mapId = null;
			if(params.get('map')){
				mapId = params.get('map')[0];
			}
			if(mapId){
				// Load map by id and construct the filter;
				TrackerMap map = TrackerMap.findByUniqueId(mapId);
				if(map){
					def filters = map.loadFilters();
					filter = new CompisiteTrackerDeviceFilter(filters:filters);
					sessionEntry.filter = filter;
				}else{
					log.info("No map filter found for id: ${mapId}");
				}
			}else{
				// compatible with old behavior
				if(params.get("udid")){
					filter = new TrackerDeviceFilter();
					filter.udid = params["udid"][0];
					if(params.get("type")){
						String t = params["type"][0];
						try{filter.type = Integer.parseInt(t);}catch(Exception e){}
					}
				}
				if(filter){
					sessionEntry.filter = filter;
				}
			}
		}
		sessions.put(clientSession.getId(), sessionEntry);
		log.debug "websocket session[${clientSession.id}] opened"
	}
	
	@OnMessage
	public void handleMessage(String message,Session clientSession) throws IOException {
		if(log.isDebugEnabled()){
			log.debug "Received: " + message
		}
		String resp = null;
		if("PING".equals(message)){
			// this is a ping message;
			resp = "PONG";
		}else{
			// trying parse as JSON {"filter":{"type":1,"udid":"xxxx"}};
			try{
				def req = JSON.parse(message);
				if(req instanceof Map) {
					def val = req['filter']
					if(val){
						TrackerDeviceFilter filter = null;
						String mapId = val['mapId'];
						String udid = val['udid'];
						double[] bounds = val['bounds'];

						if(mapId && mapId.length() > 0){
							// Load map by id and construct the filter;
							TrackerMap map = TrackerMap.findByUniqueId(val['mapId']);
							if(map){
								def filters = map.loadFilters();
								filter = new CompisiteTrackerDeviceFilter(filters:filters);
							}else{
								log.info("No map filter found for id: ${mapId}");
							}
						}else if(udid && udid.length() >0 && !"all".equalsIgnoreCase(udid)){
							filter = new TrackerDeviceFilter(udid:udid,type:val['type']);
						}else if(bounds && bounds.length == 4){
							String s = bounds.join(',');
							filter = new TrackerDeviceFilter(bounds:s);
							log.debug("websock filter bounds: ${s}");
						}
						if(filter){
							// replace the filter with current one
							SessionEntry se = sessions[clientSession.id];
							if(se){
								se.filter = filter;
							}else{
								se = new SessionEntry(session:clientSession,filter:filte);
								sessions[clientSession.id] = se;
							}
						}
					}
				}
			}catch(Exception e ){
				resp = "Parse filter failed, message: " + message;
				log.info(resp);
			}
			//resp = "echo [" + clientSession.getId() + "]"  + message;
		}

		if(resp){
			try{
				RemoteEndpoint.Async remote = clientSession.getAsyncRemote();
				remote.setSendTimeout(5000);
				remote.sendText(resp);
//			remote.sendText(message, new SendHandler(){
//				public void onResult(javax.websocket.SendResult result){
//
//				}
//			});
			}catch(Exception e){
				// ignore
				log.warn("Error send message to client[${clientSession.getId()}], ${e.getMessage()}.");
			}
		}

		//TODO - read input as JSON and parse to {"filter":{"type":1,"udid":"xxxx"}};
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
		log.error("websocket error, " + t.getMessage());
		closeSession(clientSession);
	}

	private void closeSession(Session clientSession){
		try{
			if(clientSession.isOpen())
				clientSession.close();
		}catch(Exception e){
			// noop;
		}
		sessions.remove(clientSession.getId());
	}

	static long ts = 0;
	@Override
	public void onMessageReceived(Object message){
		if(!(message instanceof PositionData)){
			return;
		}
		
		PositionData position = (PositionData)message;
		//log.trace("onPositionChange called, sessions: ${sessions}");
		// TODO filter out sessions according to the subscription record
		if(sessions.isEmpty()){
			return;
		}
		
		//def val = getTrackerService().buildDevicePositionValues(position);
		//def val = getTrackerService().getDeviceJsonData(position.udid);
		def val = getTrackerService().buildDeviceFeatureCollection(position.udid);
		def txt = val as JSON;
		
		String str = txt.toString();
		
		int i = 0;
		for(SessionEntry sessionEntry : sessions.values()){
			TrackerDeviceFilter filter = sessionEntry.filter;
			if(filter!= null && filter.accept(position)){
				i++;
				Session clientSession = sessionEntry.session;
				try{
					RemoteEndpoint.Async remote = clientSession.getAsyncRemote();
					remote.setSendTimeout(5000);
					remote.sendText(str);
				}catch(Exception e){
					log.warn("Error pushing position changes, ${e.getMessage()}. Remote ip: UNSUPPORTED");
					// For IllegalStateException, see https://bz.apache.org/bugzilla/show_bug.cgi?id=56026
					if(e instanceof IllegalStateException){
						// ignore the error
					}else{
						// or close the session
						closeSession(clientSession);
					}
				}
			}
		}// end of for
		if(i > 0 && System.currentTimeMillis() - ts > 15000){
			log.info("Pushed position changes to ${i} of ${sessions.size()} clients");
			ts = System.currentTimeMillis();
		}
	}
	
	public static class SessionEntry{
		Session session;
		TrackerDeviceFilter filter;
	}
		
}
