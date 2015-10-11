package mclub.tracker

import grails.util.Environment
import mclub.sys.ConfigService;
import mclub.user.User;
import mclub.sys.IpService;
import org.codehaus.groovy.grails.web.mapping.LinkGenerator

class TrackerMapController {
	// Inject link generator
	LinkGenerator grailsLinkGenerator
	ConfigService configService;
	TrackerService trackerService;
	IpService ipService;
	
	private String generateMapLiveServiceURL(Map<String,Object> params){
		String link = grailsLinkGenerator.link(uri:'/live0',id:'all', params:params, absolute:true);
		boolean secure = configService.getConfigBool('tracker.map.secure');
		if (Environment.current == Environment.DEVELOPMENT || Environment.current == Environment.TEST) {
			secure = false;
		}
		
		String wsLink = "";
		if(secure){
			if(link.indexOf("https://") != -1){
				wsLink = link.replace("https://","wss://");
			} else if(link.indexOf("http://") != -1) {
				wsLink = link.replace("http://","wss://");
			}
			if(wsLink.indexOf(":20880") != -1){
				wsLink = wsLink.replace(":20880", ":20843");
			}
		}else{
			// unsure wss only works under development enviromnent;
			if(link.indexOf("https://") != -1){
				wsLink = link.replace("https://","ws://");
			} else if(link.indexOf("http://") != -1) {
				wsLink = link.replace("http://","ws://");
			}
			if(wsLink.indexOf(":20843") != -1){
				wsLink = wsLink.replace(":20843", ":20880");
			}
		}
		
		return wsLink;
		
//		if(link.indexOf("https://") != -1){
//			return link.replace("https://","wss://");
//		} else {
//			return link.replace("http://","ws://");	
//		}
	}

	/**
	 * Tricky index that redirect according to hard-coded domain name	
	 * @return
	 */
    def index() {
		String serverName = request.getServerName();
		if(serverName && serverName.indexOf("nc.semitno".reverse()) != -1){
			// for on times domain, forward to the mclub map
			forward action:'mclub'
		}else{
			forward action:'aprs'
		}
	}
	
	def aprs(String id, String lat, String lon){
		MapConfig mapConfig = new MapConfig(title:"APRS Map");
			
		if(id){
			mapConfig.serviceURL = generateMapLiveServiceURL([udid:id,type:TrackerDevice.DEVICE_TYPE_APRS]);
			mapConfig.dataURL = grailsLinkGenerator.link(controller:'trackerAPI',action:'geojson',params:[udid:id,type:mclub.tracker.TrackerDevice.DEVICE_TYPE_APRS]);
			
			DeviceFilterCommand filter = new DeviceFilterCommand(udid:id);
			def devices = trackerService.filterTrackerDevices(filter);
			if(devices?.size() > 0){
				def dev = devices[0];
				if(dev.latestPositionId){
					TrackerPosition pos = TrackerPosition.load(dev.latestPositionId);
					if(pos){
						mapConfig.centerCoordinate = [pos.longitude,pos.latitude];
						mapConfig.mapZoomLevel = 10;
					}
				}
			}
			
		}else{
			// detect remote location by address
			mapConfig.serviceURL = generateMapLiveServiceURL([type:TrackerDevice.DEVICE_TYPE_APRS]);
			mapConfig.dataURL = grailsLinkGenerator.link(controller:'trackerAPI',action:'geojson',params:[udid:'all',type:mclub.tracker.TrackerDevice.DEVICE_TYPE_APRS]);
		}
		
		// load the default center point
		/*
		if(!mapConfig.centerCoordinate){
			mapConfig.centerCoordinate = detectRemoteClientLocation(); // center of hangzhou
			mapConfig.mapZoomLevel = 8;
		}
		*/
		if(log.isInfoEnabled()){
			// detect remote client location;
			detectRemoteClientLocation();
		}
		mapConfig.copyrights = "BG5HHP@HAMCLUB.net ©2015";
		render view:"map", model:[mapConfig:mapConfig];
	}
	
	private List<Float> detectRemoteClientLocation(){
		String ip = request.getHeader("Client-IP");
		if (!ip){
			ip = request.getHeader("X-Forwarded-For")
		}
		if (!ip){
			ip = request.remoteAddr
		}
		//FIXME - Move to IPService
		List<Float> ipLoc = null;
		if(ip){
			try{
				String ipAddr = ipService.lookupIpAddress(ip);
				ipLoc = ipService.addressToLocation(ipAddr);
				if(ipLoc){
					log.info "remote ip: ${ip} resolved to ${ipAddr} ${ipLoc}";
				}
			}catch(Exception e){
				// do nothing;
			}
		} else{
			log.info "detect remote ip failed"
		}
		if(!ipLoc){
			ipLoc = [120.20,30.24];
		}
		return ipLoc;
	}
	
	def mclub(String id, String lat, String lon){
		// Need user login
		User user = session['user'];
		if(!user){
			String returnURL = grailsLinkGenerator.link(action:'mclub',params:params,absolute:true);
			redirect(controller:'admin', action:'login',params:[returnURL:returnURL]);
			return ;
		}
		
		MapConfig mapConfig = new MapConfig(title:"mClub Map");
		if(id){
			mapConfig.serviceURL = generateMapLiveServiceURL([udid:id, type:TrackerDevice.DEVICE_TYPE_ACTIVED]);
			mapConfig.dataURL = grailsLinkGenerator.link(controller:'trackerAPI',action:'geojson',params:[udid:id, type:mclub.tracker.TrackerDevice.DEVICE_TYPE_ACTIVED]);
		}else{
			mapConfig.serviceURL = generateMapLiveServiceURL([type:TrackerDevice.DEVICE_TYPE_ACTIVED]);
			mapConfig.dataURL = grailsLinkGenerator.link(controller:'trackerAPI',action:'geojson',params:[udid:'all', type:mclub.tracker.TrackerDevice.DEVICE_TYPE_ACTIVED]);
		}
		mapConfig.mapZoomLevel = 11;
		render view:"map", model:[mapConfig:mapConfig];
	}
}

class MapConfig{
	String title;
	String serviceURL;
	String dataURL;
	List<Float> centerCoordinate;
	int mapZoomLevel = 8
	String copyrights;
}
