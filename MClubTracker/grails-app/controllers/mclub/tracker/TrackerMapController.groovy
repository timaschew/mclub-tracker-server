package mclub.tracker

import org.codehaus.groovy.grails.web.mapping.LinkGenerator
import mclub.tracker.TrackerDevice;

class TrackerMapController {
	// Inject link generator
	LinkGenerator grailsLinkGenerator
	
	private String generateMapLiveServiceURL(Map<String,Object> params){
		String link = grailsLinkGenerator.link(uri:'/live0',id:'all', params:params, absolute:true);
		if(link.indexOf("https://") != -1){
			return link.replace("https://","wss://");
		} else {
			return link.replace("http://","ws://");	
		}
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
