package mclub.tracker

import grails.converters.JSON
import grails.core.support.GrailsApplicationAware
import grails.util.Environment
import mclub.util.GrailsApplicationHolder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.embedded.ServletContextInitializer
import org.springframework.context.annotation.Bean

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import javax.servlet.ServletException
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

import grails.core.GrailsApplication;
import org.grails.web.util.GrailsApplicationAttributes as GA
import org.slf4j.Logger
import org.slf4j.LoggerFactory

public class LivePositionWebsocketServer implements ServletContextListener, MessageListener{
	private final Logger log = LoggerFactory.getLogger(getClass().name);
    private static LivePositionWebsocketServer instance = null;

	GrailsApplication grailsApplication;
    TrackerService trackerService;
    TrackerDataService trackerDataService;
    MessageService messageService;

    public LivePositionWebsocketServer(){
        log.info("Constructing ${this}");
    }

	//Use session store instead of the static hash set
	private ConcurrentHashMap<String,SessionEntry> sessions = new ConcurrentHashMap<String,Session>();
	
	@Override
	public void contextInitialized(ServletContextEvent event) {
		ServletContext servletContext = event.servletContext
        ServerContainer serverContainer = servletContext.getAttribute("javax.websocket.server.ServerContainer")
        assert serverContainer != null;
        try {
            if (Environment.current == Environment.DEVELOPMENT) {
                log.info("Adding endpoint to container under [development mode]");
                serverContainer.addEndpoint(LivePositionWebsocketServerEndpoint)
            }

            assert grailsApplication != null;
            def config = grailsApplication.config;
            long sessionIdleTimeout = config.liveposition.session_idle_timeout ?: 15000 // idle timeout is 15s
            serverContainer.defaultMaxSessionIdleTimeout = sessionIdleTimeout

            // register data changes
            getMessageService()?.addListener(this);

            instance = this;
            log.info("LivePositionWebsocketServer ${this} ready");
        }
        catch (IOException e) {
            log.error e.message, e
        }
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
        instance = null;
		log.info "LivePositionWebsocketServer ${this} destroyed";
	}

    @PostConstruct
    public void start(){
        sessions.clear();
        log.info "LivePositionWebsocketServer ${this} started"
    }

    @PreDestroy
    public void stop(){
        getMessageService()?.removeListener(this);
        log.info "LivePositionWebsocketServer ${this} stopped";
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
		def val = getTrackerService().getDeviceFeatureCollection(position.udid, false);
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

@ServerEndpoint("/live0")
public class LivePositionWebsocketServerEndpoint{
    private final Logger log = LoggerFactory.getLogger(getClass().name);

    @OnOpen
    public void handleOpen(Session clientSession, EndpointConfig config) {
        LivePositionWebsocketServer.SessionEntry sessionEntry = new LivePositionWebsocketServer.SessionEntry();
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
                filter = new TrackerDeviceFilter();
                if(params.get("udid")){
                    filter.udid = params["udid"][0];
                }
                if(params.get("type")){
                    String t = params["type"][0];
                    try{filter.type = Integer.parseInt(t);}catch(Exception e){}
                }

                if(filter.udid || filter.type){
                    sessionEntry.filter = filter;
                }
            }
        }
        LivePositionWebsocketServer.instance.sessions.put(clientSession.getId(), sessionEntry);
        log.debug "websocket session[${clientSession.id}] opened"
    }

    @OnMessage
    public void handleMessage(String message,Session clientSession) throws IOException {
        if(log.isDebugEnabled()){
            log.debug "Received: " + message
        }
        String resp;
        if("PING".equals(message)){
            // this is a ping message;
            resp = "PONG";
        }else{
            resp = "echo [" + clientSession.getId() + "]"  + message;
        }
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
        LivePositionWebsocketServer.instance.sessions.remove(clientSession.getId());
        log.debug "websocket session[${clientSession.id}] closed"
    }

    @OnError
    public void handleError(Session clientSession, Throwable t) {
        log.error("websocket error, " + t.getMessage());
        LivePositionWebsocketServer.instance.closeSession(clientSession);
    }
}