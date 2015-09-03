/**
 * Project: MClubTracker
 * 
 * File Created at 2013-8-10
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
package mclub.tracker

//import groovy.json.JsonBuilder
//import groovy.json.JsonSlurper
//
//import java.util.concurrent.ConcurrentHashMap
//
//import javax.annotation.PostConstruct
//import javax.annotation.PreDestroy
//
//import org.atmosphere.config.service.Get
//import org.atmosphere.config.service.ManagedService
//import org.atmosphere.config.service.Message
//import org.atmosphere.config.service.WebSocketHandlerService
//import org.atmosphere.cpr.AtmosphereResource
//import org.atmosphere.cpr.AtmosphereResourceEvent
//import org.atmosphere.cpr.AtmosphereResourceEventListenerAdapter
//import org.atmosphere.nettosphere.Config
//import org.atmosphere.nettosphere.Nettosphere
//import org.atmosphere.websocket.WebSocket
//import org.atmosphere.websocket.WebSocketHandler
//import org.atmosphere.websocket.WebSocketProcessor.WebSocketException
//import org.codehaus.groovy.grails.web.context.ServletContextHolder
//import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
//import org.slf4j.Logger
//import org.slf4j.LoggerFactory
//import org.springframework.context.ApplicationContext



/**
 * The live position service that stores all live position data for each device
 * 
 * @author shawn
 *
 */
class LivePositionService {
//	private Nettosphere server;
//	boolean lazyInit = false
//	boolean debug = true;
//
//	//@PostConstruct
//	public void start(){
//		// Start websocket server for realtime position push service
//		Config.Builder b = new Config.Builder();
//				b.port(20883)
//				.host("0.0.0.0")
//				.resource(LivePositionAtmosphereService.class)
//				.resource(LivePositionWebSocketHandler.class);
//
//		server = new Nettosphere.Builder().config(b.build()).build();
//		server.start();
//		log.info("Live Position Service started");
//		
//		Runnable r = new Runnable(){
//			public void run(){
//				while(true){
//					Thread.sleep(5000);
//					broadcastPositionUpdates();
//				}
//			}
//		};
//		new Thread(r,"LivePosition Updater").start();
//	}
//
//	//@PreDestroy
//	public void stop(){
//		server.stop();
//		log.info("Live Position Service stopped");
//	}
//	
//	private ConcurrentHashMap<String,Date> lastUpdateTimeStamp = new ConcurrentHashMap<String,Date>();
//	
//	/**
//	 * Load all tracker latest position and push back to client
//	 * 
//	 * TODO optimize1 - oneshot SQL to retrieve the device data and position
//	 * TODO optimize2 - only push those changed devices
//	 */
//	public void broadcastPositionUpdates(){
//		
//		if(id2sock.values().size() == 0){
//			// no client to broadcast
//			return;
//		}
//				
//		// query database for all devices
//		def devices = TrackerDevice.list();
//		if(devices?.size() == 0){
//			return;
//		}
//		
//		// construct the device current position data
//		def data = [];
//		devices.each{dev->
//			TrackerPosition pos = TrackerPosition.get(dev.latestPositionId);
//			if(pos){
//				// check last update time
//				Date last = lastUpdateTimeStamp[dev.udid];
//				if(debug || (!last || pos.time.time > last.time)){
//					lastUpdateTimeStamp[dev.udid] = pos.time;
//					data << [
//						id:dev.id,
//						udid:dev.udid,
//						latitude:pos.latitude,
//						longitude:pos.longitude,
//						altitude:pos.altitude,
//						speed:pos.speed,
//						course:pos.course,
//						time:pos.time
//					];
//				}
//			}
//		}
//		
//		// bail out if nothing found
//		if(data.size() == 0){
//			return;
//		}
//		
//		// build up the json string and write to client
//		def builder = new JsonBuilder()
//		builder(data)
//		def json = builder.toString()
//		
//		def failedSocks = [];
//		for(WebSocket ws in id2sock.values()){
//			try{
//				ws.write(json);
//			}catch(Exception e){
//				failedSocks << ws;
//			}
//		}
//		for(WebSocket ws in failedSocks){
//			log.info("Closing broken wsockets: ${ws}");
//			removeClient(ws);
//			try{ws.close();}catch(Exception e){}
//		}
//	}
//	
//	//===========================================================
//	//FIXME - LRU Cache to avoid OOM!!!
//	private ConcurrentHashMap<String,WebSocket> id2sock = new ConcurrentHashMap<String,WebSocket>();
//	private ConcurrentHashMap<WebSocket,String> sock2id = new ConcurrentHashMap<WebSocket,String>();
//	
//	/**
//	 * Add client
//	 * @param clientId
//	 * @param ws
//	 * @return
//	 */
//	WebSocket addClient(String clientId, WebSocket ws){
//		WebSocket prev = id2sock.put(clientId, ws);
//		sock2id[ws] = clientId;
//		if(prev != ws){
//			return prev
//		}else{
//			return null;
//		}
//	}
//	
//	/**
//	 * Remove client
//	 * @param ws
//	 * @return
//	 */
//	String removeClient(WebSocket ws){
//		String clientId = sock2id.remove(ws);
//		if(clientId){
//			id2sock.remove(clientId);
//		}
//		return clientId;
//	}
//	
//	private static LivePositionService getInstance(){
//		ApplicationContext context = (ApplicationContext) ServletContextHolder.getServletContext().
//		getAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT);
//		return context.getBean(LivePositionService.class);
//	}
	
	
	////////////////////////////////////////////////////////////////////////
	// Live position 
	////////////////////////////////////////////////////////////////////////
	// 1. each device has a cached of positions(in latest 300s, but not exceeding max 10 positions)
	// 2. everytime received position update, it will publish the changes according to the pubsubservice 
	
}

//@WebSocketHandlerService(path="/live")
//class LivePositionWebSocketHandler implements WebSocketHandler{
//	
//	public void onByteMessage(WebSocket ws, byte[] data, int offset, int length) throws IOException {
//		// Unsupported binary message
//	}
//
//	public void onClose(WebSocket ws) {
//		String did = LivePositionService.getInstance().removeClient(ws);
//		log.info("client ${did} disconnected");
//	}
//
//	public void onError(WebSocket ws, WebSocketException e) {
//		log.info("${ws} error ${e}");
//		String did = LivePositionService.getInstance().removeClient(ws);
//		if(did){
//			log.info("disconnecting client ${did}");
//		}
//	}
//
//	public void onOpen(WebSocket ws) throws IOException {
//		log.info("${ws} opened");
//	}
//
//	public void onTextMessage(WebSocket ws, String message) throws IOException {
//		// message should be a valid json string.
//		// currently only {"deviceId"="123456"} supported 
//		def slurper = new JsonSlurper()
//		try{
//			def m = slurper.parseText(message);
//			if(m){
//				LivePositionService lps = LivePositionService.getInstance();
//				String cid = m['clientId'];
//				if(cid){
//					WebSocket prev = lps.addClient(cid, ws);
//					if(prev){
//						// close the previously associated one
//						try{prev.close();}catch(Exception e){};
//					}
//					// response with OK
//					ws.write("ok");
//					log.info("client ${cid} connected for live position update");
//					return;
//				}
//				boolean debug = m['debug'];
//				if(debug != lps.debug){
//					lps.debug = debug;
//				}
//			}
//		}catch(Exception e){
//			log.info("Error parsing message: ${e.message}");
//		}
//		
//		log.info("Unknow message: ${message}");
//	}
//}

//@ManagedService(path = "/live2")
//class LivePositionAtmosphereService{
//	//	private final ObjectMapper mapper = new ObjectMapper();
//	private final Logger logger = LoggerFactory.getLogger(LivePositionAtmosphereService.class);
//
//	@Get
//	public void onOpen(final AtmosphereResource r) {
//		r.addEventListener(new AtmosphereResourceEventListenerAdapter() {
//					@Override
//					public void onSuspend(AtmosphereResourceEvent event) {
//						logger.info("User {} connected.", r.uuid());
//					}
//
//					@Override
//					public void onDisconnect(AtmosphereResourceEvent event) {
//						if (event.isCancelled()) {
//							logger.info("User {} unexpectedly disconnected", r.uuid());
//						} else if (event.isClosedByClient()) {
//							logger.info("User {} closed the connection", r.uuid());
//						}
//					}
//				});
//	}
//
//	@Message
//	public String onMessage(String message) throws IOException {
//		//return mapper.writeValueAsString(mapper.readValue(message, Data.class));
//		logger.info("Received ${message}");
//		return "OK";
//	}
//
//}

