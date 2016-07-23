package mclub.tracker

import com.github.davidmoten.geo.GeoHash
import com.github.davidmoten.geo.LatLong
import grails.converters.JSON
import grails.util.Environment
import mclub.ham.repeater.util.TextHelper
import mclub.sys.ConfigService;
import mclub.user.User;
import mclub.sys.IpService;

import org.codehaus.groovy.grails.web.mapping.LinkGenerator

class MapController {
	
	private static final String KEY_APRS_MAP_MIRROR = "aprs.map.mirror";
	private static final String KEY_SITE_LICENSE = "site.license";
	private static final String KEY_SITE_LICENSE_LINK = "site.license.link";
	
	// Inject link generator
	LinkGenerator grailsLinkGenerator
	ConfigService configService;
	TrackerService trackerService;
	IpService ipService;

	private boolean isSecureHttpEnabled(){
		boolean forceSecure = configService.getConfigBool('tracker.map.forceSecure');
		if (Environment.current == Environment.DEVELOPMENT || Environment.current == Environment.TEST) {
			forceSecure = false;
		}
		 if(forceSecure || request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"))){
			 return true;
		 }
		return false;
	}

	// Read from tracker.map.amap_api_url
	private String generateMapAPIURL(){
		String amapApiUrl = configService.getConfigString("tracker.map.amap_api_url");
		if(amapApiUrl == null){
			throw new RuntimeException("AMap API URL is NOT set!");
		}
		if(isSecureHttpEnabled()){
			amapApiUrl = amapApiUrl.replace("http://","https://");
		}
		return amapApiUrl;
	}

	private String generateMapLiveServiceURL(Map<String,Object> params){
		String link = grailsLinkGenerator.link(uri:'/live0',id:'all', params:params, absolute:true);
		String wsLink = "";
		if(isSecureHttpEnabled()){
			// always use wss://20843
			if(link.indexOf("https://") != -1){
				wsLink = link.replace("https://","wss://");
			} else if(link.indexOf("http://") != -1) {
				wsLink = link.replace("http://","wss://");
			}
			if(wsLink.indexOf(":20880") != -1){
				wsLink = wsLink.replace(":20880", ":20843");
			}
		}else{
			// use wss or ws according to the request of http or https
			if(link.indexOf("https://") != -1){
				wsLink = link.replace("https://","wss://");
			} else if(link.indexOf("http://") != -1) {
				wsLink = link.replace("http://","ws://");
			}
		}
		
		return wsLink;
	}

	private boolean checkAprsMapMirrorEnabled(){
		String aprsMapMirrorLink = configService.getConfig(KEY_APRS_MAP_MIRROR); 
		if(aprsMapMirrorLink){
			redirect(url:aprsMapMirrorLink);
			return true;
		}
		return false;
	}
	
	/**
	 * Tricky index that redirect according to hard-coded domain name	
	 * @return
	 */
    def index(String id) {
		// for APRS map request
		if(id == null || id.equalsIgnoreCase('aprs')){
			// check domain name and forward
			String serverName = request.getServerName();
			if(serverName && serverName.indexOf("nc.semitno".reverse()) == -1){
				forward(action:'aprs');
				return;
			}
		}

		// special case
		if(id == null){
			render(text:"The mClub Map")
			return;
		}
		if('test'.equals(id)){
			forward(action:'test');
			return;
		}
		
		// The mClub Map part
		TrackerMap map = TrackerMap.findByUniqueId(id);
		if(map){
			// if map type is not public, check user session first
			if(map.type != TrackerMap.TRACKER_MAP_TYPE_PUBLIC){
				// check user session
				User user = session['user'];
				if(!user){
					String returnURL = grailsLinkGenerator.link(params:params,absolute:true,id:id);
					redirect(controller:'admin', action:'login',params:[returnURL:returnURL]);
					return ;
				}
				if(user.type == User.USER_TYPE_DISABLED){
					render(text:"No permission", status:403);
					return;
				}
			}
			
			// just pass the map id to action:geojson
			MapConfig mapConfig = new MapConfig(title:"Tracker Map", mapApiURL:generateMapAPIURL());
			if(map.name) mapConfig.title = "${map.name} Map";
			mapConfig.serviceURL = generateMapLiveServiceURL([map:map.uniqueId]);// generate the websocket url with live0?map=xxx
			mapConfig.dataURL = grailsLinkGenerator.link(controller:'trackerAPI',action:'geojson',params:[map:map.uniqueId]);// generate the geojson?map=xxx
			// TODO center the map ?
			if(log.isInfoEnabled()){
				// detect remote client location;
				detectRemoteClientLocation();
			}
			mapConfig.copyrights = "BG5HHP@HAMCLUB.net ©2015";
			mapConfig.siteLicense = configService.getConfig(KEY_SITE_LICENSE);
			mapConfig.siteLicenseLink = configService.getConfig(KEY_SITE_LICENSE_LINK);
			render view:"map", model:[mapConfig:mapConfig];
			//render (text:'Not implemented yet', status:501);
			
			mapConfig.showLineDots = detectShowLineDots();
		}else{
			render(text:'map not found', status:404)
		}
	}


	private MapConfig buildDefaultMapConfig(){
		MapConfig mapConfig = new MapConfig();
		// setup default values
		mapConfig.title = "mClub Map";
		mapConfig.mapApiURL = generateMapAPIURL();
		mapConfig.serviceURL = generateMapLiveServiceURL([type:TrackerDevice.DEVICE_TYPE_APRS]);
		mapConfig.dataURL = grailsLinkGenerator.link(controller:'trackerAPI',action:'geojson',params:[udid:'all',type:mclub.tracker.TrackerDevice.DEVICE_TYPE_APRS]);
		mapConfig.showLineDots = detectShowLineDots();
		mapConfig.copyrights = "BG5HHP@HAMCLUB.net ©2015";
		mapConfig.siteLicense = configService.getConfig(KEY_SITE_LICENSE);
		mapConfig.siteLicenseLink = configService.getConfig(KEY_SITE_LICENSE_LINK);

		mapConfig.aprsMarkerImagePath = asset.assetPath(src: 'aprs/aprs-fi-sym');
		mapConfig.standardMakerImagePath = asset.assetPath(src: 'map/');

		return mapConfig;
	}

	/*
	 * AJAX interface receives the query and returns the map config
	 */
	def query(String q){
		MapConfig mapConfig = buildDefaultMapConfig();
		def mapFilter = [:];
		def result = [mapConfig:mapConfig,mapFilter:mapFilter];

		if(!q){
			// returns the default map config
			render result as JSON;
			return;
		}

		q = q.toUpperCase();

		if(q.length() >=2 && TextHelper.isChinese(q)){
			// input is chinese location name
			def lonlat = ipService.addressToLocation(q);
			if(lonlat){
				mapConfig.centerCoordinate = [lonlat[0],lonlat[1]];
				mapConfig.mapZoomLevel = 10;
			}else{
				result['errorMessage'] = "NO data found";
			}
		}else if("all".equalsIgnoreCase(q)){
			mapFilter['udid'] = q;
			// same as the default map config
//			// query all
//			mapConfig['serviceURL'] = generateMapLiveServiceURL([type:TrackerDevice.DEVICE_TYPE_APRS]);
//			mapConfig['dataURL'] = grailsLinkGenerator.link(controller:'trackerAPI',action:'geojson',params:[udid:'all',type:mclub.tracker.TrackerDevice.DEVICE_TYPE_APRS]);
		}else if(q.indexOf(",") > 0) {
			/*FIXME use patterns*/
			// query bounds
			def bounds = q;
			mapConfig.serviceURL= generateMapLiveServiceURL([type:TrackerDevice.DEVICE_TYPE_APRS/*,bounds:bounds*/ /*TODO - websocket server not supported yet*/]);
			mapConfig.dataURL = grailsLinkGenerator.link(controller:'trackerAPI',action:'geojson',params:[udid:'all',bounds:bounds,type:mclub.tracker.TrackerDevice.DEVICE_TYPE_APRS]);
		}else{
			// query by id prefix
			def id = q;
			mapConfig.serviceURL = generateMapLiveServiceURL([udid:id,type:TrackerDevice.DEVICE_TYPE_APRS]);
			mapConfig.dataURL = grailsLinkGenerator.link(controller:'trackerAPI',action:'geojson',params:[udid:id,type:mclub.tracker.TrackerDevice.DEVICE_TYPE_APRS]);

			// Calculate the map range of the devices
			TrackerDeviceFilter filter = new TrackerDeviceFilter(udid:id);
			def devices = trackerService.findTrackerDevices(filter);
			if(devices?.size() > 0){
				def dev = devices[0];
				if(dev.latestPositionId){
					TrackerPosition pos = TrackerPosition.load(dev.latestPositionId);
					if(pos){
						mapConfig.centerCoordinate = [pos.longitude,pos.latitude];
						mapConfig.mapZoomLevel = 10;
					}
				}
				result['count'] = devices.size();
				mapFilter['udid'] = id;
			}else{
				result['errorMessage'] = "NO data found"
			}
		}
		render result as JSON;
	}

	/*
	 * The APRS Map
	 */
	def aprs(String id /*device id*/, String q, String lat, String lon, String bounds){
		if(checkAprsMapMirrorEnabled()){
			return;
		}
		
		MapConfig mapConfig = new MapConfig(title:"APRS Map - hamclub.net", mapApiURL:generateMapAPIURL());
		mapConfig.mapZoomLevel = 10;

		def mapFilter = [:];

		if(!id && q){
			// if q=杭州, will query by that place
			if(q.length() >=2 && TextHelper.isChinese(q)){
				// input is chinese location name
				def lonlat = ipService.addressToLocation(q);
				if(lonlat){
					mapConfig.centerCoordinate = [lonlat[0],lonlat[1]];
					mapConfig.mapZoomLevel = 10;
				}else{
					flash.message = "nothing found";
					id = 'none';
				}
			}else{
				// map/aprs?q=bg5
				// input is the call sign
				id = q.toUpperCase();
			}
		}

		if(id && !"all".equalsIgnoreCase(id)){
			mapConfig.serviceURL = generateMapLiveServiceURL([udid:id,type:TrackerDevice.DEVICE_TYPE_APRS]);
			mapConfig.dataURL = grailsLinkGenerator.link(controller:'trackerAPI',action:'geojson',params:[udid:id,type:mclub.tracker.TrackerDevice.DEVICE_TYPE_APRS]);

			mapFilter['udid'] = id;

			TrackerDeviceFilter filter = new TrackerDeviceFilter(udid:id);
			def devices = trackerService.findTrackerDevices(filter);
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
		}else if(bounds){
			// determin by map ranges
			// query by map region
			mapConfig.serviceURL = generateMapLiveServiceURL([type:TrackerDevice.DEVICE_TYPE_APRS,bounds:bounds /*TODO - websocket server not supported yet*/]);
			mapConfig.dataURL = grailsLinkGenerator.link(controller:'trackerAPI',action:'geojson',params:[udid:'all',bounds:bounds,type:mclub.tracker.TrackerDevice.DEVICE_TYPE_APRS]);
			//mapFilter['bounds'] = bounds.toDoubleArray();

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
		mapConfig.showLineDots = detectShowLineDots();
		if(log.isInfoEnabled()){
			// detect remote client location;
			detectRemoteClientLocation();
		}
		mapConfig.copyrights = "BG5HHP@HAMCLUB.net ©2015";
		mapConfig.siteLicense = configService.getConfig(KEY_SITE_LICENSE);
		mapConfig.siteLicenseLink = configService.getConfig(KEY_SITE_LICENSE_LINK);
		render view:"map", model:[mapConfig:mapConfig,mapFilter:mapFilter];
	}


	def aprs2(String id /*device id*/, String q, String lat, String lon, String bounds){
		if(checkAprsMapMirrorEnabled()){
			return;
		}

		MapConfig mapConfig = new MapConfig(title:"APRS Map - hamclub.net", mapApiURL:generateMapAPIURL());
		mapConfig.mapZoomLevel = 10;

		def mapFilter = [:];

		if(!id && q){
			// if q=杭州, will query by that place
			if(q.length() >=2 && TextHelper.isChinese(q)){
				// input is chinese location name
				def lonlat = ipService.addressToLocation(q);
				if(lonlat){
					mapConfig.centerCoordinate = [lonlat[0],lonlat[1]];
					mapConfig.mapZoomLevel = 10;
				}else{
					flash.message = "nothing found";
					id = 'none';
				}
			}else{
				// map/aprs?q=bg5
				// input is the call sign
				id = q.toUpperCase();
			}
		}

		if(id && !"all".equalsIgnoreCase(id)){
			mapConfig.serviceURL = generateMapLiveServiceURL([udid:id,type:TrackerDevice.DEVICE_TYPE_APRS]);
			mapConfig.dataURL = grailsLinkGenerator.link(controller:'trackerAPI',action:'geojson',params:[udid:id,type:mclub.tracker.TrackerDevice.DEVICE_TYPE_APRS]);

			mapFilter['udid'] = id;

			TrackerDeviceFilter filter = new TrackerDeviceFilter(udid:id);
			def devices = trackerService.findTrackerDevices(filter);
			if(devices?.size() > 0){
				if(false) {
					// Calculate the center point of devices
					double maxLat = -90, maxLon = -180, minLat = 90, minLon = 180;
					for (TrackerDevice dev in devices) {
						LatLong latlon = GeoHash.decodeHash(dev.locationHash);
						if (latlon.lat > maxLat) maxLat = latlon.lat;
						if (latlon.lon > maxLon) maxLon = latlon.lon;
						if (latlon.lat < minLat) minLat = latlon.lat;
						if (latlon.lon < minLon) minLon = latlon.lon;
					}
					log.info("${maxLat},${maxLon},${minLat},${minLon}");
					mapConfig.centerCoordinate = [(maxLon + minLon) / 2, (maxLat + minLat) / 2];
					mapConfig.mapZoomLevel = 10;
				}else{
                    def dev = devices[0];
                    if(dev.latestPositionId){
                        TrackerPosition pos = TrackerPosition.load(dev.latestPositionId);
                        if(pos){
                            mapConfig.centerCoordinate = [pos.longitude,pos.latitude];
                            mapConfig.mapZoomLevel = 10;
                        }
                    }
				}
			}
		}else if(bounds){
			// determin by map ranges
			// query by map region
			mapConfig.serviceURL = generateMapLiveServiceURL([type:TrackerDevice.DEVICE_TYPE_APRS,bounds:bounds /*TODO - websocket server not supported yet*/]);
			mapConfig.dataURL = grailsLinkGenerator.link(controller:'trackerAPI',action:'geojson',params:[udid:'all',bounds:bounds,type:mclub.tracker.TrackerDevice.DEVICE_TYPE_APRS]);
			//mapFilter['bounds'] = bounds.toDoubleArray();

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
		mapConfig.showLineDots = detectShowLineDots();
		if(log.isInfoEnabled()){
			// detect remote client location;
			detectRemoteClientLocation();
		}
		mapConfig.copyrights = "BG5HHP@HAMCLUB.net ©2015";
		mapConfig.siteLicense = configService.getConfig(KEY_SITE_LICENSE);
		mapConfig.siteLicenseLink = configService.getConfig(KEY_SITE_LICENSE_LINK);
		render view:"map2", model:[mapConfig:mapConfig,mapFilter:mapFilter];
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

	/*
	 * Display ALL non-APRS MAP
	 */
	def all(String id){
		// Need user login
		User user = session['user'];
		if(!user){
			String returnURL = grailsLinkGenerator.link(params:params,absolute:true);
			redirect(controller:'admin', action:'login',params:[returnURL:returnURL]);
			return ;
		}
		if(user.type < User.USER_TYPE_ADMIN){
			render(text:"No permission", status:403);
			return;
		}
		
		MapConfig mapConfig = new MapConfig(title:"mClub Map", mapApiURL:generateMapAPIURL());
		if(id){
			mapConfig.serviceURL = generateMapLiveServiceURL([udid:id, type:TrackerDevice.DEVICE_TYPE_ACTIVED]);
			mapConfig.dataURL = grailsLinkGenerator.link(controller:'trackerAPI',action:'geojson',params:[udid:id, type:mclub.tracker.TrackerDevice.DEVICE_TYPE_ACTIVED]);
		}else{
			mapConfig.serviceURL = generateMapLiveServiceURL([type:TrackerDevice.DEVICE_TYPE_ACTIVED]);
			mapConfig.dataURL = grailsLinkGenerator.link(controller:'trackerAPI',action:'geojson',params:[udid:'all', type:mclub.tracker.TrackerDevice.DEVICE_TYPE_ACTIVED]);
		}
		mapConfig.mapZoomLevel = 11;
		mapConfig.showLineDots = this.detectShowLineDots();
		render view:"map", model:[mapConfig:mapConfig];
	}
	
	/*
	 * Test only
	 */
	def test(){
		User user = session['user'];
		if(!user){
			String returnURL = grailsLinkGenerator.link(action:'test',params:params,absolute:true);
			redirect(controller:'admin', action:'login',params:[returnURL:returnURL]);
			return ;
		}
		if(user.type != User.USER_TYPE_ADMIN){
			render(text:"No permission", status:403);
			return;
		}
		TrackerMap map = TrackerMap.findByUniqueId('foobar');
		if(!map){
			map = new TrackerMap(uniqueId:'Zone5Ham', name:'Test Map', filterJSON:'[BG5]', type:0);
			map.save(flush:true);
			if(map.errors){
				render text:map.errors.toString();
				return;
			}
		}
		render text:'OK'
	}
	
	private boolean detectShowLineDots(){
		String ua = request.getHeader("User-Agent");
		if(log.isDebugEnabled()) log.debug("User Agent: " + ua);
		if(ua != null && (ua.indexOf('iPhone') >0 || ua.indexOf('Android') > 0)){
			return false;
		}
		return configService.getConfigBool("tracker.map.showLineDots");
	}
}

class MapConfig{
	String title;
	String serviceURL;
	String dataURL;
	String mapApiURL;
	List<Float> centerCoordinate;
	int mapZoomLevel = 8
	String copyrights;
	String siteLicense;
	String siteLicenseLink;
	Boolean showLineDots = true; // by default will show line dots
	String defaultMarkerIcon = "https://webapi.amap.com/images/marker_sprite.png" //TODO - configurable
	String aprsMarkerImagePath;
	String standardMakerImagePath;
}
