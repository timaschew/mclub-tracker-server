package mclub.tracker

import java.text.SimpleDateFormat
import java.util.Map;

import grails.converters.JSON;

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

import mclub.user.User
import mclub.util.MapShiftUtils;

/**
 * The tracker service that saves data from tracker
 * @author shawn
 *
 */
class TrackerService {
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
			TrackerDevice.withTransaction {
				// load initial data
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

			// Test the traccar database
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
	 * 
	 * @param pos
	 * @return
	 */
	public Map<String,Object> buildDevicePositionGeojsonData(String udid){
		// TODO - optimize out the query
		TrackerDevice device = TrackerDevice.findByUdid(udid);
		if(!device){
			// no such device
			return [:];
		}
		return buildDevicePositionGeojsonData(device);
	}
	
	
	/**
	 * Build device position geojson data. Example: https://github.com/shawnchain/mclub-tracker-app/wiki/api-data-examples
	 * @param device
	 * @return
	 */
	public Map<String,Object> buildDevicePositionGeojsonData(TrackerDevice device){
		def featureCollection = [:];
		
		featureCollection['type'] = 'FeatureCollection';
		featureCollection['id'] = 'mclub_tracker_livepositions';
		
		def features = [];
		def dfeatures = buildDevicePositionGeojsonFeatures(device);
		if(dfeatures)features.addAll(dfeatures);
		featureCollection['features'] = features;
		
		return featureCollection;
	}
	
	public Map<String, Object> buildAllDevicePositionGeojsonData(){
		def devices = TrackerDevice.list();
		def featureCollection = [:];
		def features = [];
		devices?.each{ dev->
			def dfeatures = buildDevicePositionGeojsonFeatures(dev);
			if(dfeatures) features.addAll(dfeatures);
		}
		if(!features.isEmpty()){
			featureCollection['type'] = 'FeatureCollection';
			featureCollection['features'] = features;
			featureCollection['id'] = 'mclub_tracker_livepositions';
		}
		return featureCollection;
	}
	
	/**
	 * Load device features from db
	 * @param device
	 * @return
	 */
	private Collection<Object> loadDeviceFeatures(TrackerDevice device, TrackerPosition pos){
		def deviceFeatures = [];
		
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
			if(device.status == TrackerDevice.DEVICE_TYPE_APRS){
				// prefix with APRS symbol
				markerFeatureProperties['marker-symbol'] = "aprs_${device.icon}";
				//markerFeatureProperties['icon'] = "aprs_${device.icon}"
			}else{
				markerFeatureProperties['marker-symbol'] = device.icon;
				//markerFeatureProperties['icon'] = device.icon;
			}
		}
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
					/* we have speed/course in feature properties. so dont need here.
					if(pos.speed >=0){
						aprs['speed'] = pos.speed;
					}
					if(pos.course >=0){
						aprs['course'] = pos.course;
					}
					*/
				}
			}
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
		
		/*
		 { "type": "Feature",
		   "geometry": {
			  "type": "LineString",
			  "coordinates": [
				[102.0, 0.0], [103.0, 1.0], [104.0, 0.0], [105.0, 1.0]
				]
			  },
			 "properties": {
				"udid": "0001"
			 }
		   }
		 */
		
		//TODO - configurable MAX_LINE_POINTS, LINE_TIME
		// Add line string feature, load points that in 45 mins ago and not exceeding 50 in total.
		int MAX_LINE_POINTS = 360; // 30 * 60 / 5
		Date lineTime = new Date(System.currentTimeMillis() - mclub.util.DateUtils.TIME_OF_HALF_HOUR + (15 * 60 * 1000));
		def positions = TrackerPosition.findAll("FROM TrackerPosition p WHERE p.device=:dev AND p.time>:lineTime ORDER BY p.time DESC",[dev:device, lineTime:lineTime, max:MAX_LINE_POINTS]);
		positions = shrinkTrackPositions(positions);
		int positionCount = positions?.size();
		 
		// Add marker only when it contains valid positions
		if(positionCount > 0){
			deviceFeatures.add(markerFeature);
		}
		
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
			def lineFeature = [
				'type':'Feature',
				'geometry':lineFeatureGeometry,
				'properties':lineFeatureProperties
			];
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
	public Collection buildDevicePositionGeojsonFeatures(TrackerDevice device /*, Map<String,Object> feature_properties_template*/){
		TrackerPosition pos = TrackerPosition.get(device.latestPositionId);
		if(!pos){
			return null;
		}
		
		// check whether position is expired
		// TODO - make the expire time configurable 
		if((System.currentTimeMillis() - pos.time.time) > mclub.util.DateUtils.TIME_OF_HALF_HOUR){
			// evict expired device features from cache and returns null;
			trackerCacheService.removeDeviceFeature(device.udid);
			return null;
		}
		
		//load from cache first
		Collection<Object> features = trackerCacheService.getDeviceFeature(device.udid);
		if(!features){
			Collection<Object> f = loadDeviceFeatures(device,pos);
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
	public Map<String,Object> buildDevicePositionJsonData(String udid){
		// TODO - optimize out the query
		TrackerDevice device = TrackerDevice.findByUdid(udid);
		if(!device){
			// no such device
			return [:];
		}
		return buildDevicePositionJsonData(device);
	}
	
	/**
	 * 
	 * @param udid
	 * @return
	 */
	public Map<String,Object> buildDevicePositionJsonData(TrackerDevice device){
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
