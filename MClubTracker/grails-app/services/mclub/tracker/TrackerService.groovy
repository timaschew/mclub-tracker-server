package mclub.tracker

import java.text.SimpleDateFormat
import java.util.Map;

import grails.converters.JSON;
import grails.validation.Validateable

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

import mclub.sys.ConfigService
import mclub.user.User
import mclub.util.MapShiftUtils;

/**
 * The tracker service that saves data from tracker
 * @author shawn
 *
 */
class TrackerService {
	ConfigService configService;
	def trackerDataService;
	TrackerCacheService trackerCacheService;
	
	int mapCoordType = TrackerPosition.COORDINATE_TYPE_GCJ02;
	
	@PostConstruct
	public void start(){
		// load initial data
		// add test devices
//		if(TrackerPosition.count() > 0){
//			TrackerPosition.withTransaction {
//				TrackerPosition.deleteAll();
//			}
//		}

		if(TrackerDevice.count() == 0){
			def device;
			TrackerDevice.withTransaction{
				device = new TrackerDevice(udid:'353451048729261',status:1,username:'test');
				device.save(flush:true);
			}
			
			//30.28022, 120.11774
			// mock device position
			trackerDataService.addPosition(new TrackerPosition(
					//deviceId:1,
					device:device,
					time:new Date(),
					valid:true,
					latitude:30.28022,
					longitude:120.11774,
					altitude:0,
					speed:28,
					course:180,
					));

			trackerDataService.addPosition(new TrackerPosition(
					//deviceId:1,
					device:device,
					time:new Date(),
					valid:true,
					latitude:30.28122,
					longitude:120.11774,
					altitude:0,
					speed:29,
					course:181
					));
			trackerDataService.addPosition(new TrackerPosition(
					//deviceId:1,
					device:device,
					time:new Date(),
					valid:true,
					latitude:30.28222,
					longitude:120.11774,
					altitude:0,
					speed:30,
					course:182
					));
		}
	}

	@PreDestroy
	public void stop(){
	}

	/**
	 * List device position of day
	 * @param deviceId
	 * @param date
	 * @return
	 */
	public List<TrackerPosition> listDevicePositionOfDay(String udid, Date date){
		TrackerDevice device = TrackerDevice.findByUdid(udid);
		if(device){
			Date startTime = date;
			Date endTime = new Date(date.getTime() + mclub.util.DateUtils.TIME_OF_ONE_DAY);
			// query db for the date
			def results = TrackerPosition.findAll("FROM TrackerPosition p WHERE p.device=:dev AND p.time >=:startTime AND p.time <= :endTime",[dev:device, startTime:startTime, endTime:endTime, max:1500]);
			return results
		}else{
			log.info("Unknow device: ${udid}");
		}
		return [];
	}

	/**
	 * List daily tracks. 
	 * We will save device positions into tracks automatically in daily basis.
	 * @param deviceId
	 * @param beginDay
	 * @param endDay
	 * @return
	 */
	public List<TrackerTrack> listTracksBetweenDays(String udid, Date beginDay, Date endDay){
		def device = TrackerDevice.findByUdid(udid);
		if(!device){
			return [];
		}

		// List all tracks between that days
		def tracksInThatDays = TrackerTrack.findAll("FROM TrackerTrack tt WHERE tt.deviceId = :dev AND tt.beginDate >=:begin AND tt.beginDate <=:end",[dev:device.id, begin:beginDay, end:endDay]);
		return tracksInThatDays
	}
	
	/////////////////////////////////////////////////////////////////
	// GEOJSON/JSON Data builder
	/////////////////////////////////////////////////////////////////
	/**
	 * Facade call for controllers to build the whole GEOJSON
	 * @param devices
	 * @return
	 */
	public Map<String,Object> buildGeojsonFeatureCollection(Collection<TrackerDevice> devices){
		def featureCollection = [:];
		def features = [];
		devices?.each{ dev->
			def dfeatures = buildDeviceFeatures(dev);
			if(dfeatures)
				features.addAll(dfeatures);
		}
		if(!features.isEmpty()){
			featureCollection['type'] = 'FeatureCollection';
			featureCollection['features'] = features;
			featureCollection['id'] = 'mclub_tracker_livepositions';
		}
		return featureCollection;
	}
	
	public Collection<TrackerDevice> findTrackerDevices(TrackerDeviceFilter cmd){
		if("all".equalsIgnoreCase(cmd.udid)){
			cmd.udid = null;
		}
		
		if(cmd.activeTime == null){
			// by default we'll only show active positions in last 30 minutes
			Integer maximumShowPositionInterval = configService.getConfigInt("tracker.maximumShowPositionInterval");
			if(!maximumShowPositionInterval) maximumShowPositionInterval = mclub.util.DateUtils.TIME_OF_HALF_HOUR;
			cmd.activeTime = new java.util.Date(System.currentTimeMillis() - maximumShowPositionInterval);
		}
		
		def c = TrackerDevice.createCriteria();
		def devs = c.list{
			gt('latestPositionTime',cmd.activeTime)
			and{
				if(cmd.udid){
					ilike('udid',"${cmd.udid}%")
				}
				if(cmd.type != null){
					eq('status', cmd.type)
				}
			}
		}
		
		//TODO - filter on lat/lon
		return devs;
	}
	
	/**
	 * 
	 * @param pos
	 * @return
	 */
	public Map<String,Object> getDeviceFeatureCollection(String udid,boolean includeLine){
		// TODO - optimize out the query
		TrackerDevice device = TrackerDevice.findByUdid(udid);
		if(!device){
			// no such device
			return [:];
		}
		return getDeviceFeatureCollection(device,includeLine);
	}
	
//	/**
//	 * Build device position geojson data. Example: https://github.com/shawnchain/mclub-tracker-app/wiki/api-data-examples
//	 * @param device
//	 * @return
//	 */
//	public Map<String,Object> getDeviceFeatureCollection(TrackerDevice device){
//		def featureCollection = [:];
//		
//		featureCollection['type'] = 'FeatureCollection';
//		featureCollection['id'] = 'mclub_tracker_livepositions';
//		
//		def features = [];
//		def dfeatures = buildDeviceFeatures(device);
//		if(dfeatures)features.addAll(dfeatures);
//		featureCollection['features'] = features;
//		
//		return featureCollection;
//	}
	
	/**
	 * Build device position geojson data. Example: https://github.com/shawnchain/mclub-tracker-app/wiki/api-data-examples
	 * @param device
	 * @param includeLine
	 * @return
	 */
	public Map<String,Object> getDeviceFeatureCollection(TrackerDevice device, boolean includeLine){
		def featureCollection = [:];
		
		featureCollection['type'] = 'FeatureCollection';
		featureCollection['id'] = 'mclub_tracker_livepositions';
		
		def features = [];
		if(includeLine){
			def dfeatures = buildDeviceFeatures(device);
			if(dfeatures) features.addAll(dfeatures);
		}else{
			def mf = loadDeviceMarkerFeature(device);
			if(mf) features.add(mf);
		}
		featureCollection['features'] = features;
		
		return featureCollection;
	}

	
	private Map<String,Object> loadDeviceMarkerFeature(TrackerDevice device){
		def markerFeatureProperties = [
			//'id':"fp_${device.id}",
			'title':"tk-${device.udid}",
			'udid':"${device.udid}",
			'description':"",
			'marker-color':"#00bcce",
			'marker-size': "medium",
			'marker-symbol': "circle",
			"marker-zoom": ""
			]
		
		// Load user name and mobile phone TODO optimization of loading user's phone number.
		if(device.username){
			User user = User.findByName(device.username);
			if(user){
				markerFeatureProperties['username'] = user.displayName?user.displayName:user.name;
				if(user.phone){
					markerFeatureProperties['phone'] = user.phone;
				}
			}else{
				markerFeatureProperties['username'] = device.username; // no such user record, so just use the device field info
			}
			
			if(device.status == TrackerDevice.DEVICE_TYPE_APRS){
				// for APRS device, the marker feature title is the CALL-ID(device.udid)
				markerFeatureProperties['title'] = device.udid;
			}else{
				markerFeatureProperties['title'] = markerFeatureProperties['username'];
			}
		}else{
			markerFeatureProperties['username'] = "unknown"; // no user associated ?
		}
		
		
		// Load marker symbol
		if(device.icon){
			markerFeatureProperties['marker-symbol'] = device.icon;
			if(device.status == TrackerDevice.DEVICE_TYPE_APRS){
				//NOTE: Workaround for existing APRS devices with incorrect icon filename
				if(device.icon.startsWith("1_") || device.icon.startsWith("2_")){
					markerFeatureProperties['marker-symbol'] = "aprs_${device.icon}";
				}
			}
		}else{
			// check if device contains emergency messages
			def t = new Date(System.currentTimeMillis() - mclub.util.DateUtils.TIME_OF_QUARTER_OF_AN_HOUR);
			def r = TrackerPosition.executeQuery("SELECT COUNT(*) FROM TrackerPosition tp WHERE tp.device=:dev AND tp.time>:time AND tp.messageType=:msgType",[dev:device,time:t,msgType:TrackerPosition.MESSAGE_TYPE_EMERGENCY]);
			if(r[0] > 0){
				// we have emergency messages in last 30 minutes, mark the logo
				markerFeatureProperties['marker-symbol'] = "sos1";
			}
		}
		
		// load speed/course from latest position
		TrackerPosition pos = TrackerPosition.get(device.latestPositionId);
		if(!pos) return null; // empty result
		
		if(pos.speed && pos.speed >=0){
			markerFeatureProperties['speed'] = pos.speed;
		}
		if(pos.course && pos.course >=0){
			markerFeatureProperties['course'] = pos.course;
		}
		
		// Add extended info
		if(pos.getExtendedInfo()){
			def extendedInfo = JSON.parse(pos.getExtendedInfo());
			if(extendedInfo instanceof Map){
				markerFeatureProperties.putAll(extendedInfo);
				def aprs = extendedInfo['aprs'];
				if(aprs){
					if(aprs['comment']){
						markerFeatureProperties['description'] = aprs['comment'];
					}
				}
			}
		}
		
		// Description for non APRS devices
		if(pos.message){
			markerFeatureProperties['message'] = pos.message;
		}
		// Timestamp is formatted in Chinese style with GMT+8.
		String sTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(pos.time);
		markerFeatureProperties['timestamp'] = sTime;
		
		// Shifting coordinates for chinese map!
		def coordinate;
		switch(mapCoordType){
			case TrackerPosition.COORDINATE_TYPE_GCJ02:
				if(pos.coordinateType == null || pos.coordinateType == 0){
					coordinate = MapShiftUtils.WGSToGCJ(pos.longitude,pos.latitude);
				}else{
					coordinate = [pos.longitude, pos.latitude];
				}
				break;
			case TrackerPosition.COORDINATE_TYPE_BD09:
			default:
				coordinate = [pos.longitude, pos.latitude];
				break;
		}
		
		// round to xx.yyyyyy
		coordinate[0] = Math.round(coordinate[0] * 1000000)/1000000.0
		coordinate[1] = Math.round(coordinate[1] * 1000000)/1000000.0
		
		def markerFeatureGeometry = [
			"type": "Point",
			"coordinates": coordinate
			]
		
		def markerFeature = [
			'type':"Feature",
			'properties':markerFeatureProperties,
			'geometry' : markerFeatureGeometry,
			];

		return markerFeature;
	}
	
	private Map<String,Object> loadDeviceLineFeature(TrackerDevice device/*, Date time, Integer limit*/){
		// Add line string feature, by default will load points that in 30 minutes ago and not exceeding 360 in total.
		Integer minimalPositionUpdateInterval = configService.getConfigInt("tracker.minimalPositionUpdateInterval");
		if(!minimalPositionUpdateInterval) minimalPositionUpdateInterval = 5000L;
		Integer maximumShowPositionInterval = configService.getConfigInt("tracker.maximumShowPositionInterval");
		if(!maximumShowPositionInterval) maximumShowPositionInterval = mclub.util.DateUtils.TIME_OF_HALF_HOUR;
		int maxPointsOfLine = maximumShowPositionInterval / minimalPositionUpdateInterval; // (30 * 60 * 1000 / 5000 = 360)
		
		Date lineTime = new Date(System.currentTimeMillis() - maximumShowPositionInterval /*(30 * 60 * 1000)*/);
		def positions = TrackerPosition.findAll("FROM TrackerPosition p WHERE p.device=:dev AND p.time>:lineTime ORDER BY p.time DESC",[dev:device, lineTime:lineTime, max:maxPointsOfLine]);
		positions = shrinkTrackPositions(positions);
		int positionCount = positions?.size();
		
		def lineFeature = [:];
		// Add line only when positions count >=4
		if(positionCount >=4){
			def lineCoordinates = [];
			// build line string features if positions count >=4
			for(TrackerPosition p in positions){
				def c;
				if(mapCoordType){
					c = MapShiftUtils.WGSToGCJ(p.longitude,p.latitude);
				}else{
					c = [p.longitude, p.latitude];
				}
				// round to xx.yyyyyy
				c[0] = Math.round(c[0] * 1000000)/1000000.0
				c[1] = Math.round(c[1] * 1000000)/1000000.0
				lineCoordinates.add(c);
			}
			def lineFeatureGeometry = [
				'type': 'LineString',
				'coordinates': lineCoordinates
			];
			def lineFeatureProperties = [
				'udid':"${device.udid}"
			];
		
			lineFeature = [
				'type':'Feature',
				'geometry':lineFeatureGeometry,
				'properties':lineFeatureProperties
			];
		}
		return lineFeature;
	}
	
	/**
	 * Load device features from db
	 * @param device
	 * @return
	 */
	private Collection<Object> loadDeviceFeatures(TrackerDevice device){
		def deviceFeatures = [];
		
		if(!device || !device.latestPositionId){
			return deviceFeatures;
		}
		
		def markerFeature = loadDeviceMarkerFeature(device);
		if(!markerFeature){
			return deviceFeatures;
		}
		deviceFeatures.add(markerFeature);
		
		def lineFeature = loadDeviceLineFeature(device);
		if(lineFeature){
			deviceFeatures.add(lineFeature);
		}
		return deviceFeatures;
	}
	
	private static double[] roundCoordinate(double x, double y){
		double[] coord = new double[2];
		coord[0] = Math.round(x * 1000000)/1000000.0;
		coord[1] = Math.round(y * 1000000)/1000000.0;
		return coord;
	}
	
	/**
	 * Shrink a list of positions by removing duplicated(same lat/lon) in slibing positions
	 * @param positions
	 * @return
	 */
	private Collection<TrackerPosition> shrinkTrackPositions(Collection<TrackerPosition> positions){
		def shrinked = [];
		TrackerPosition prev = null;
		if(positions){
			for(TrackerPosition pos in positions){
				if(prev != null){
					if(prev.latitude == pos.latitude && prev.longitude == pos.longitude){
						continue;
					}
				}
				shrinked.add(pos);
				prev = pos;
			}
		}
		return shrinked;
	}
	
	/**
	 * Build geojson data of the device
	 * 
	 * @TODO Use feature builder or feature template
	 * 
	 * @param device
	 * @return
	 */
	private Collection buildDeviceFeatures(TrackerDevice device){
		// check whether position is expired - DUPLICATED!
		Integer maximumShowPositionInterval = configService.getConfigInt("tracker.maximumShowPositionInterval");
		if(!maximumShowPositionInterval) maximumShowPositionInterval = mclub.util.DateUtils.TIME_OF_HALF_HOUR;
		if((System.currentTimeMillis() - device.latestPositionTime?.time) > maximumShowPositionInterval){
			// evict expired device features from cache and returns null;
			trackerCacheService.removeDeviceFeature(device.udid);
			return null;
		}
		
		//load from cache first
		Collection<Object> features = trackerCacheService.getDeviceFeature(device.udid);
		if(!features){
			Collection<Object> f = loadDeviceFeatures(device);
			if(f){
				// the cache returns the true features, see implementations inside.
				features = trackerCacheService.cacheDeviceFeature(device.udid, f);
			}
		}
		
		return features;
	}
	
	/**
	 * build map data for json rendering from a device udid
	 * @param position
	 * @return
	 */
	public Map<String,Object> getDeviceJsonData(String udid){
		// TODO - optimize out the query
		TrackerDevice device = TrackerDevice.findByUdid(udid);
		if(!device){
			// no such device
			return [:];
		}
		return getDeviceJsonData(device);
	}
	
	/**
	 * 
	 * @param udid
	 * @return
	 */
	public Map<String,Object> getDeviceJsonData(TrackerDevice device){
		def values = [:]

		values['udid'] = device.udid;
		values['name'] = getDeviceName(device);
		def positions = [];
		values['positions'] = positions;
		TrackerPosition pos = TrackerPosition.get(device.latestPositionId);
		//TODO - load more positions
		if(pos){
			// check last update time
			positions << convertToPositionValues(device,pos);
		}
		return values
	}
	
	private Map<String,Object> convertToPositionValues(TrackerDevice device, TrackerPosition pos){
		def	values = [
			//id:device.id,
			//udid:device.udid,
			latitude:pos.latitude,
			longitude:pos.longitude,
			altitude:pos.altitude,
			speed:pos.speed,
			course:pos.course,
			time:pos.time
		];
		return values;
	}
	
	private String getDeviceName(TrackerDevice device){
		if(device.username)
			return device.username;
		if(device.udid){
			String s = device.udid;
			int len = s.length();
			if(len > 4)
				s = s.substring(len - 4 ,len);
			return "tk-${s}"
		}
		return "tk-${device.id}"
	}
}

