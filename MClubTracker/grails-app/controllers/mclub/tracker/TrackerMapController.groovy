package mclub.tracker

import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import mclub.sys.ConfigService
import mclub.tracker.TrackerDevice;
import grails.util.Environment;

class TrackerMapController {
	// Inject link generator
	LinkGenerator grailsLinkGenerator
	ConfigService configService;
	
	private String generateMapLiveServiceURL(Map<String,Object> params){
		String link = grailsLinkGenerator.link(uri:'/live0',id:'all', params:params, absolute:true);
		boolean secure = Boolean.TRUE.equals(configService.getConfig('tracker.map.secure'));
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
		}else{
			mapConfig.serviceURL = generateMapLiveServiceURL([type:TrackerDevice.DEVICE_TYPE_APRS]);
			mapConfig.dataURL = grailsLinkGenerator.link(controller:'trackerAPI',action:'geojson',params:[udid:'all',type:mclub.tracker.TrackerDevice.DEVICE_TYPE_APRS]);
		}
			
		render view:"map", model:[mapConfig:mapConfig];
	}
	
	def mclub(String id, String lat, String lon){
		MapConfig mapConfig = new MapConfig(title:"mClub Map");
		if(id){
			mapConfig.serviceURL = generateMapLiveServiceURL([udid:id, type:TrackerDevice.DEVICE_TYPE_ACTIVED]);
			mapConfig.dataURL = grailsLinkGenerator.link(controller:'trackerAPI',action:'geojson',params:[udid:id, type:mclub.tracker.TrackerDevice.DEVICE_TYPE_ACTIVED]);
		}else{
			mapConfig.serviceURL = generateMapLiveServiceURL([type:TrackerDevice.DEVICE_TYPE_ACTIVED]);
			mapConfig.dataURL = grailsLinkGenerator.link(controller:'trackerAPI',action:'geojson',params:[udid:'all', type:mclub.tracker.TrackerDevice.DEVICE_TYPE_ACTIVED]);
		}
		
		render view:"map", model:[mapConfig:mapConfig];
	}
}

class MapConfig{
	String title;
	String serviceURL;
	String dataURL;
}
