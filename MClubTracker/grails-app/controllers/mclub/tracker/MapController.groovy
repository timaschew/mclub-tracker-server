package mclub.tracker

import com.github.davidmoten.geo.GeoHash
import com.github.davidmoten.geo.LatLong
import grails.converters.JSON
import grails.util.Environment
import mclub.ham.repeater.util.TextHelper
import mclub.sys.ConfigService
import mclub.sys.ConfigServiceKeys;
import mclub.user.User;
import mclub.sys.IpService;

import org.codehaus.groovy.grails.web.mapping.LinkGenerator

import java.util.regex.Pattern

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

	// Read from tracker.map.amapApiUrl
	private String generateMapAPIURL(){
		String amapApiUrl = configService.getConfigString("tracker.map.amapApiUrl");
		if(amapApiUrl == null){
			throw new RuntimeException("AMap API URL is NOT set!");
		}
		if(isSecureHttpEnabled()){
			amapApiUrl = amapApiUrl.replace("http://","https://");
		}
		return amapApiUrl;
	}

	private String generateMapLiveServiceURL(Map<String,Object> params){
		String base = configService.getConfigString(ConfigService.LIVE_DATA_SERVICE_URL);
		if(!base) base = '/live0';
		String link = grailsLinkGenerator.link(uri:base,id:'all', params:params, absolute:true);
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

		if(!wsLink){
			wsLink = link;
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
     * Build MapConfig with default values
     * @return
     */
	private MapConfig buildDefaultMapConfig(){
		MapConfig mapConfig = new MapConfig();
		// setup default values
		mapConfig.title = "mClub Map";
		mapConfig.mapApiURL = generateMapAPIURL();
		mapConfig.serviceURL = generateMapLiveServiceURL([:]);
		mapConfig.dataURL = grailsLinkGenerator.link(controller:'trackerAPI',action:'geojson');
		mapConfig.queryURL = grailsLinkGenerator.link(controller:'map',action:'query');
		mapConfig.deviceActiveDaysApi = grailsLinkGenerator.link(controller:'trackerReportAPI',action:'device_active_days');
		mapConfig.showLineDots = detectShowLineDots();
		mapConfig.copyrights = "BG5HHP@HAMCLUB.net ©2016";
		mapConfig.siteLicense = configService.getConfig(KEY_SITE_LICENSE);
		mapConfig.siteLicenseLink = configService.getConfig(KEY_SITE_LICENSE_LINK);
		mapConfig.mapZoomLevel = 10;
		mapConfig.aprsMarkerImagePath = asset.assetPath(src: 'aprs/aprs-fi-sym');
		mapConfig.standardMakerImagePath = asset.assetPath(src: 'map/');
		mapConfig.historyEnabled = configService.getConfigBool(ConfigService.MAP_HISTORY_ENABLED);

		if (Environment.current == Environment.PRODUCTION) {
			mapConfig.debug = false;
		}else{
			mapConfig.debug = true;
		}
		return mapConfig;
	}

    /**
     * Detect whether show line dots or not according to device type(via UA) or configuration
     * @return
     */
    private boolean detectShowLineDots(){
        String ua = request.getHeader("User-Agent");
        if(log.isDebugEnabled()) log.debug("User Agent: " + ua);
        if(ua != null && (ua.indexOf('iPhone') >0 || ua.indexOf('Android') > 0)){
            return false;
        }
        return configService.getConfigBool("tracker.map.showLineDots");
    }


    /**
	 * Index page that forward/redirect according to the configuration
	 * @return
	 */
    def index(String id) {
        // Forward to APRS if configured or host name containing it.
        if('aprs'.equalsIgnoreCase(configService.getConfigString(ConfigServiceKeys.MAP_DEFAULT_MAP))){
            forward(action:'aprs');
            return;
        }
        String serverName = request.getServerName();
        if(serverName.toLowerCase().indexOf('aprs') >=0){ // aprs.foobar.com
            forward(action:'aprs');
            return;
        }

		// special case
		if(id == null){
			render(text:"The mClub Map")
			return;
		}

		forward(action:'mclub');
	}

	/**
	 * The mclub map
	 * @param id
	 * @param q
	 */
	def mclub(String id, String q){

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
			MapConfig mapConfig = buildDefaultMapConfig();
            MapFilter mapFilter = new MapFilter(type:TrackerDevice.DEVICE_TYPE_ACTIVED);
			if(map.name){
				mapConfig.title = "${map.name} Map";
			}
			mapConfig.serviceURL = generateMapLiveServiceURL([map:map.uniqueId]);// generate the websocket url with live0?map=xxx
			mapConfig.dataURL = grailsLinkGenerator.link(controller:'trackerAPI',action:'geojson',params:[map:map.uniqueId]);// generate the geojson?map=xxx
			// TODO center the map ?
			if(log.isInfoEnabled()){
				// detect remote client location;
				detectRemoteClientLocation();
			}
			render view:"map", model:[mapConfig:mapConfig,mapFilter:mapFilter];
		}else{
			render(text:'map not found', status:404)
		}
	}

    private doQuery(String q, Integer deviceType){
        MapConfig mapConfig = buildDefaultMapConfig();
        def mapFilter = new MapFilter(type:deviceType);
        def result = [mapConfig:mapConfig,mapFilter:mapFilter];

        if(!q){
            // returns the default map config
            return result;
        }

        q = q.toUpperCase();

        if(q.length() >=2 && StringUtils.isChinese(q)){
            // input is chinese location name
            List<Double> lonlat = ipService.addressToLocation(q);
            if(lonlat){
                mapConfig.centerCoordinate = [lonlat[0],lonlat[1]];
            }else{
                result['errorMessage'] = "Nothing found, check your query please!";
            }
        }else if("all".equalsIgnoreCase(q)){
            mapFilter.udid = q;
            // same as the default map config
//			// query all
//			mapConfig['serviceURL'] = generateMapLiveServiceURL([type:TrackerDevice.DEVICE_TYPE_APRS]);
//			mapConfig['dataURL'] = grailsLinkGenerator.link(controller:'trackerAPI',action:'geojson',params:[udid:'all',type:mclub.tracker.TrackerDevice.DEVICE_TYPE_APRS]);
        }else if(q.indexOf(",") > 0) {
            /*FIXME use patterns*/
            // query bounds
            def bounds = q;
            mapFilter.bounds = q.split(',').each{
                Double.parseDouble(it);
            }
            //mapConfig.serviceURL= generateMapLiveServiceURL([type:TrackerDevice.DEVICE_TYPE_APRS/*,bounds:bounds*/ /*TODO - websocket server not supported yet*/]);
            //mapConfig.dataURL = grailsLinkGenerator.link(controller:'trackerAPI',action:'geojson',params:[udid:'all',bounds:bounds,type:mclub.tracker.TrackerDevice.DEVICE_TYPE_APRS]);
        }else{
            // query by id prefix
            def id = q;
            mapFilter.udid = id;
            //mapConfig.serviceURL = generateMapLiveServiceURL([udid:id,type:TrackerDevice.DEVICE_TYPE_APRS]);
            //mapConfig.dataURL = grailsLinkGenerator.link(controller:'trackerAPI',action:'geojson',params:[udid:id,type:mclub.tracker.TrackerDevice.DEVICE_TYPE_APRS]);

            // Calculate the map range of the devices
            TrackerDeviceFilter filter = new TrackerDeviceFilter(udid:id,type:deviceType);
            def devices = trackerService.findTrackerDevices(filter);
            if(devices?.size() > 0){
                def dev = devices[0];
                if(dev.latestPositionId){
                    TrackerPosition pos = TrackerPosition.load(dev.latestPositionId);
                    if(pos){
                        mapConfig.centerCoordinate = [((int)(pos.longitude * 1000000)) / 1000000.0,(int)(pos.latitude * 1000000)/1000000.0];
                    }
					if(devices.size() == 1){
						// increase the map zoom if only 1 device found
						mapConfig.mapZoomLevel = 12;
					}
                }
                result['count'] = devices.size();
            }else{
                result['errorMessage'] = "Nothing found, check your query please!";
            }
        }
        return result;
    }

	/*
	 * AJAX interface receives the query and returns the map config
	 */
	def query(String q,Integer type){
        if(!'true'.equals(params['jquery']) &&  !'XMLHttpRequest'.equals(request.getHeader('X-Requested-With'))) {
            redirect action:'index', params:params
            return;
        }

        //TODO - honor the type parameter
        def result = doQuery(q,type/*TrackerDevice.DEVICE_TYPE_APRS*/);
        render result as JSON;
	}

	def aprs_compatible(String id){
		// redirect to the new aprs action
		if(id){
			redirect action:'aprs',params:[q:id];
		}else{
			forward action:'aprs'
		}
	}

	/*
	 * The Legacy APRS Map
	 */
	def aprs_v1(String id /*device id*/, String q, String bounds){
		if(checkAprsMapMirrorEnabled()){
			return;
		}

		MapConfig mapConfig = buildDefaultMapConfig();
		mapConfig.title = "APRS Map - hamclub.net";
		mapConfig.mapZoomLevel = 10;
        // FIXME - workaround for backward compatibility
        mapConfig.dataURL = grailsLinkGenerator.link(controller:'trackerAPI',action:'geojson',params:[udid:'all',type:mclub.tracker.TrackerDevice.DEVICE_TYPE_APRS]);
        mapConfig.queryURL = grailsLinkGenerator.link(controller:'map',action:'query');

        MapFilter mapFilter = new MapFilter(type:TrackerDevice.DEVICE_TYPE_APRS);

		if(!id && q){
			// if q=杭州, will query by that place
			if(q.length() >=2 && StringUtils.isChinese(q)){
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

			mapFilter.udid = id;

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
					if(devices.size() == 1){
						mapConfig.mapZoomLevel = 12;
					}
				}
			}
		}else if(bounds){
			// determin by map ranges
			// query by map region
			mapConfig.serviceURL = generateMapLiveServiceURL([type:TrackerDevice.DEVICE_TYPE_APRS,bounds:bounds /*TODO - websocket server not supported yet*/]);
			mapConfig.dataURL = grailsLinkGenerator.link(controller:'trackerAPI',action:'geojson',params:[udid:'all',bounds:bounds,type:mclub.tracker.TrackerDevice.DEVICE_TYPE_APRS]);
			//mapFilter.bounds = bounds.toDoubleArray();

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
		mapConfig.copyrights = "BG5HHP@HAMCLUB.net ©2016";
		mapConfig.siteLicense = configService.getConfig(KEY_SITE_LICENSE);
		mapConfig.siteLicenseLink = configService.getConfig(KEY_SITE_LICENSE_LINK);
		render view:"map", model:[mapConfig:mapConfig,mapFilter:mapFilter];
	}

    /**
     * Map version2 supports dynamic query
     * @return
     */
	def aprs(String q){
		if(checkAprsMapMirrorEnabled()){
			return;
		}

        def result = doQuery(q, TrackerDevice.DEVICE_TYPE_APRS);
		result.mapConfig.title = "APRS Map - hamclub.net";

		if(log.isInfoEnabled()){
			// detect remote client location;
			detectRemoteClientLocation();
		}
		render view:"map2", model:[mapConfig:result.mapConfig,mapFilter:result.mapFilter];
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
	def all(){
		// Need user login
		User user = session['user'];
		if(!user){
			String returnURL = grailsLinkGenerator.link(action:'all', params:params,absolute:true);
			redirect(controller:'admin', action:'login',params:[returnURL:returnURL]);
			return ;
		}
		if(user.type < User.USER_TYPE_ADMIN){
			render(text:"No permission", status:403);
			return;
		}

		/*
		def result = doQuery('all', TrackerDevice.DEVICE_TYPE_ACTIVED);
		result.mapConfig.title = "mClub Map";
		render view:"map2", model:[mapConfig:result.mapConfig,mapFilter:result.mapFilter];
		*/

		MapConfig mapConfig = buildDefaultMapConfig();
		mapConfig.title = "mClub Map";
		mapConfig.mapZoomLevel = 11;
		MapFilter mapFilter = new MapFilter(type:TrackerDevice.DEVICE_TYPE_ACTIVED);

		TrackerDeviceFilter filter = new TrackerDeviceFilter(type:TrackerDevice.DEVICE_TYPE_ACTIVED);
		def devices = trackerService.findTrackerDevices(filter);
		if(devices?.size() > 0){
			def dev = devices[0];
			if(dev.latestPositionId){
				TrackerPosition pos = TrackerPosition.load(dev.latestPositionId);
				if(pos){
					mapConfig.centerCoordinate = [((int)(pos.longitude * 1000000)) / 1000000.0,(int)(pos.latitude * 1000000)/1000000.0];
				}
			}
			if(devices?.size() == 1){
				mapConfig.mapZoomLevel = 12;
			}
		}else{
			//result['errorMessage'] = "Nothing found, check your query please!";
		}
		render view:"map2", model:[mapConfig:mapConfig,mapFilter:mapFilter];
	}
}

class MapConfig{
	String title;
	String serviceURL;
	String dataURL;
    String queryURL;
	String mapApiURL;
	String deviceActiveDaysApi;
	List<Float> centerCoordinate;
	int mapZoomLevel = 8
	String copyrights;
	String siteLicense;
	String siteLicenseLink;
	Boolean showLineDots = true; // by default will show line dots
	String defaultMarkerIcon = "https://webapi.amap.com/images/marker_sprite.png" //TODO - configurable
	String aprsMarkerImagePath;
	String standardMakerImagePath;
	Boolean debug;
	Boolean historyEnabled;
}

class MapFilter{
    String udid;
    double[] bounds;
    String mapId;
    int type;
}

class StringUtils{
	public static boolean isChinese(String str) {
		if (str == null) {
			return false;
		}
		Pattern pattern = Pattern.compile("[\\u4E00-\\u9FBF]+");
		return pattern.matcher(str.trim()).find();
	}
}
