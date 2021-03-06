package mclub.tracker
import com.github.davidmoten.geo.Coverage
import com.github.davidmoten.geo.GeoHash
import com.github.davidmoten.geo.util.Preconditions
import mclub.util.DateUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
	private Logger log = LoggerFactory.getLogger(getClass());
	
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
			def devFeatures = _buildDeviceFeaturesIncludingMarkerAndLineUsingCacheIfPossible(dev);
			if(devFeatures) {
				features.addAll(devFeatures);
			}
		}
		if(!features.isEmpty()){
			featureCollection['type'] = 'FeatureCollection';
			featureCollection['features'] = features;
			featureCollection['id'] = 'mclub_tracker_positions_live';
		}
		return featureCollection;
	}

	/**
	 * Find tracker in bounds
	 * @param lat1
	 * @param lon1
	 * @param lat2
	 * @param lon2
	 * @param activeTime
	 * @return
	 */
	public Collection<TrackerDevice> findTrackerDevicesInBounds(Double lat1,Double lon1, Double lat2, Double lon2,java.util.Date activeTime,Integer type){
		if(activeTime == null){
			// by default we'll only show active positions in last 30 minutes
			Integer maximumShowPositionInterval = configService.getConfigInt("tracker.maximumShowPositionInterval");
			if(!maximumShowPositionInterval) maximumShowPositionInterval = mclub.util.DateUtils.TIME_OF_HALF_HOUR;
			activeTime = new java.util.Date(System.currentTimeMillis() - maximumShowPositionInterval);
		}
		// Calculate the geo hashes covers the bounds
		def coverHashes = GeoHash.coverBoundingBox(lat1,lon1,lat2,lon2).hashes;
		log.debug("Bound [${lat1},${lon1},${lat2},${lon2}] cover hashes: ${coverHashes}")
		def criteria = TrackerDevice.createCriteria();
		def results = criteria.list{
			gt('latestPositionTime',activeTime)
			if(type){
				and {
					eq('status', type)
				}
			}
			and{
				or{
					coverHashes.each{ hash ->
						like('locationHash',"${hash}%")
					}
				}
			}
		}
		return results;
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
					//ilike('udid',"${cmd.udid}%")
					like('udid',"${cmd.udid}%")
				}
				if(cmd.type != null){
					eq('status', cmd.type)
				}
			}
		}
		
		//TODO - filter on lat/lon
		return devs;
	}

	public Map<String,Object> buildHistoricalDeviceFeatureCollection(TrackerDevice device,String historyTimeString){

		def featureCollection = [:];
		if(!device || !historyTimeString){
			return featureCollection;
		}

		def deviceFeatures = [];

		// load the 'latest' position in the historyDate;
		Date[] beginEndDate = DateUtils.getBeginEndTimeOfDay(historyTimeString);
		def s1 = System.currentTimeMillis();
		String tpHQL = 'FROM TrackerPosition AS tp WHERE tp.device=:device AND tp.time >= :begin AND tp.time <= :end ORDER BY tp.time DESC';
		def position = TrackerPosition.find(tpHQL,[device:device, begin:beginEndDate[0],end:beginEndDate[1]/*,max:1*/])
		if(log.isInfoEnabled())
			log.info("Historical query on [${device.udid}] date(${historyTimeString}) elapsed ${System.currentTimeMillis() - s1} ms");
		if(!position){
			// no position found, returns empty result;
			return featureCollection;
		}

		def markerFeature = loadDeviceMarkerFeature(device,position);
		if(markerFeature){
			deviceFeatures.add(markerFeature);
		}

		// load line in the time period
		def lineFeature = loadDeviceLineFeature(device,beginEndDate[0],beginEndDate[1]);
		if(lineFeature){
			deviceFeatures.add(lineFeature);
		}

		if(deviceFeatures && deviceFeatures.size() > 0){
			featureCollection['type'] = 'FeatureCollection';
			featureCollection['id'] = 'mclub_tracker_positions_historical';
			featureCollection['features'] = deviceFeatures;
		}
		return featureCollection;
	}

	/**
	 * Build device feature collection with udid
	 * @param udid
	 * @return The FeatureCollection map
	 */
	public Map<String,Object> buildDeviceFeatureCollection(String udid){
		// TODO - optimize out the query
		TrackerDevice device = TrackerDevice.findByUdid(udid);
		if(!device){
			// no such device
			return [:];
		}
		return buildDeviceFeatureCollection(device,null/*position*/,false /*includeLine*/);
	}

	/**
	 * Build device position geojson data. Example: https://github.com/shawnchain/mclub-tracker-app/wiki/api-data-examples
	 * @param device - the device
	 * @param position - the position object used to read speed/lat/lon from, if null, will load position from device.latestPositionId
	 * @param includeLine - including line feature
	 * @return
	 */
	public Map<String,Object> buildDeviceFeatureCollection(TrackerDevice device,TrackerPosition position, boolean includeLine){
		def featureCollection = [:];
		
		def features = _buildDeviceFeatures(device,position,includeLine);

		if(features && features.size() > 0){
			featureCollection['type'] = 'FeatureCollection';
			featureCollection['id'] = 'mclub_tracker_positions_live';
			featureCollection['features'] = features;
		}
		return featureCollection;
	}

	/**
	 * Read device data and build the device marker feature
	 * @param device
	 * @param position
	 * @return
	 */
	private Map<String,Object> loadDeviceMarkerFeature(TrackerDevice device, TrackerPosition position){
		def markerFeatureProperties = [
			//'id':"fp_${device.id}",
			'title':"tk-${device.udid}",
			'udid':"${device.udid}",
			'description':"",
			'marker-color':"#00bcce",
			'marker-size': "medium",
			'marker-symbol': "circle",
			"marker-zoom": "",
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
		
		// load speed/course from latest position if no position specified
		if(position == null) {
			position = TrackerPosition.get(device.latestPositionId);
		}
		if(position == null) {
			// we could not continue without a valid position
			return null
		};
		markerFeatureProperties["position_id"] = position.id;
		if(position.speed && position.speed >=0){
			markerFeatureProperties['speed'] = position.speed;
		}
		if(position.course && position.course >=0){
			markerFeatureProperties['course'] = position.course;
		}
		if(position.altitude && position.altitude >=0){
			markerFeatureProperties['altitude'] = position.altitude; // altitude in meter
		}

		
		// Add extended info
		if(position.getExtendedInfo()){
			def extendedInfo = JSON.parse(position.getExtendedInfo());
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
		if(position.message){
			markerFeatureProperties['message'] = position.message;
		}
		// Timestamp is formatted in Chinese style with GMT+8.
		String sTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(position.time);
		markerFeatureProperties['timestamp'] = sTime;
		
		// Shifting coordinates for chinese map!
		def coordinate;
		switch(mapCoordType){
			case TrackerPosition.COORDINATE_TYPE_GCJ02:
				if(position.coordinateType == null || position.coordinateType == 0){
					coordinate = MapShiftUtils.WGSToGCJ(position.longitude,position.latitude);
				}else{
					coordinate = [position.longitude, position.latitude];
				}
				break;
			case TrackerPosition.COORDINATE_TYPE_BD09:
			default:
				coordinate = [position.longitude, position.latitude];
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
	
	private Map<String,Object> loadDeviceLineFeature(TrackerDevice device, Date timeBegin, Date timeEnd/*, Date time, Integer limit*/){
		// Add line string feature, by default will load points that in 30 minutes ago and not exceeding 360 in total.
		Integer minimalPositionUpdateInterval = configService.getConfigInt("tracker.minimalPositionUpdateInterval");
		if(!minimalPositionUpdateInterval) minimalPositionUpdateInterval = 5000L;

		def positions;
		if(timeBegin == null && timeEnd == null){
			// TODO - optimize the query logic
			Integer maximumShowPositionInterval = configService.getConfigInt("tracker.maximumShowPositionInterval");
			if(!maximumShowPositionInterval) maximumShowPositionInterval = mclub.util.DateUtils.TIME_OF_HALF_HOUR;
			int maxPointsOfLine = maximumShowPositionInterval / minimalPositionUpdateInterval; // (30 * 60 * 1000 / 5000 = 360)
			Date lineTime = new Date(System.currentTimeMillis() - maximumShowPositionInterval /*(30 * 60 * 1000)*/);
			positions = TrackerPosition.findAll("FROM TrackerPosition p WHERE p.device=:dev AND p.time>:lineTime ORDER BY p.time DESC",[dev:device, lineTime:lineTime, max:maxPointsOfLine]);
		}else{
			positions = TrackerPosition.findAll("FROM TrackerPosition p WHERE p.device=:dev AND p.time>=:begin AND p.time<=:end ORDER BY p.time DESC",[dev:device, begin:timeBegin,end:timeEnd]);
		}
		positions = shrinkTrackPositions(positions);
		int positionCount = positions?.size();
		
		def lineFeature = [:];
		// Add line only when positions count >=4
		if(positionCount >=4){
			def lineCoordinates = [];
			def positionIds = [];
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
				positionIds.add(p.id);
			}
			def lineFeatureGeometry = [
				'type': 'LineString',
				'coordinates': lineCoordinates
			];
			def lineFeatureProperties = [
				'udid':"${device.udid}",
				'position_ids':positionIds
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
	 *
	 * @param device
	 * @param position
	 * @param includeLine
	 * @return
	 */
	private Collection<Object> _buildDeviceFeatures(TrackerDevice device, TrackerPosition position, boolean includeLine){
		def deviceFeatures = [];

		if(!device || !device.latestPositionId){
			return deviceFeatures;
		}

		def markerFeature = loadDeviceMarkerFeature(device,position);
		if(markerFeature){
			deviceFeatures.add(markerFeature);
		}

		// if position is specified outside, don't include the line features
		if(position == null && includeLine){
			def lineFeature = loadDeviceLineFeature(device,null,null);
			if(lineFeature){
				deviceFeatures.add(lineFeature);
			}
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
	private Collection _buildDeviceFeaturesIncludingMarkerAndLineUsingCacheIfPossible(TrackerDevice device){
		// check whether position is expired - DUPLICATED!
		Integer maximumShowPositionInterval = configService.getConfigInt("tracker.maximumShowPositionInterval");
		if(!maximumShowPositionInterval) maximumShowPositionInterval = mclub.util.DateUtils.TIME_OF_HALF_HOUR;
		if((System.currentTimeMillis() - device.latestPositionTime?.time) > maximumShowPositionInterval){
			// evict expired device features from cache and returns null;
			trackerCacheService.removeDeviceFeature(device.udid);
			return null;
		}

		Collection<Object> features;

		//load from cache first
		features = trackerCacheService.getDeviceFeature(device.udid);
		if(!features){
			// build device feature with line
			Collection<Object> f = _buildDeviceFeatures(device,null,true);
			if(f){
				// the cache returns the true features, see implementations inside.
				features = trackerCacheService.cacheDeviceFeature(device.udid, f);
			}
		}

		return features;
	}

//	/**
//	 * build map data for json rendering from a device udid
//	 * @param position
//	 * @return
//	 */
//	public Map<String,Object> getDeviceJsonData(String udid){
//		// TODO - optimize out the query
//		TrackerDevice device = TrackerDevice.findByUdid(udid);
//		if(!device){
//			// no such device
//			return [:];
//		}
//		return getDeviceJsonData(device);
//	}
	
//	/**
//	 *
//	 * @param udid
//	 * @return
//	 */
//	public Map<String,Object> getDeviceJsonData(TrackerDevice device){
//		def values = [:]
//
//		values['udid'] = device.udid;
//		values['name'] = getDeviceName(device);
//		def positions = [];
//		values['positions'] = positions;
//		TrackerPosition pos = TrackerPosition.get(device.latestPositionId);
//		//TODO - load more positions
//		if(pos){
//			// check last update time
//			positions << convertToPositionValues(device,pos);
//		}
//		return values
//	}
	
//	private Map<String,Object> convertToPositionValues(TrackerDevice device, TrackerPosition pos){
//		def	values = [
//			//id:device.id,
//			//udid:device.udid,
//			latitude:pos.latitude,
//			longitude:pos.longitude,
//			altitude:pos.altitude,
//			speed:pos.speed,
//			course:pos.course,
//			time:pos.time
//		];
//		return values;
//	}
	
//	private String getDeviceName(TrackerDevice device){
//		if(device.username)
//			return device.username;
//		if(device.udid){
//			String s = device.udid;
//			int len = s.length();
//			if(len > 4)
//				s = s.substring(len - 4 ,len);
//			return "tk-${s}"
//		}
//		return "tk-${device.id}"
//	}
}

