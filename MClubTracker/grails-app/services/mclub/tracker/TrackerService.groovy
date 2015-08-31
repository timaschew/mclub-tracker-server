package mclub.tracker

import java.util.Map;

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

/**
 * The tracker service that saves data from tracker
 * @author shawn
 *
 */
class TrackerService {
	def trackerDataService;

	@PostConstruct
	public void start(){
		// load initial data
		// add test devices
		if(TrackerDevice.count() == 0){
			TrackerDevice.withTransaction {
				// load initial data
				new TrackerDevice(udid:'353451048729261').save(flush:true);
			}
			//30.28022, 120.11774
			// mock device position
			trackerDataService.addPosition(new TrackerPosition(
					deviceId:1,
					time:new Date(),
					valid:true,
					latitude:30.28022,
					longitude:120.11774,
					altitude:0,
					speed:28,
					course:180
					));

			trackerDataService.addPosition(new TrackerPosition(
					deviceId:1,
					time:new Date(),
					valid:true,
					latitude:30.28122,
					longitude:120.11774,
					altitude:0,
					speed:29,
					course:181
					));
			trackerDataService.addPosition(new TrackerPosition(
					deviceId:1,
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
	public List<TrackerPosition> listDevicePositionOfDay(String deviceUniqueId, Date date){
		def dbId = trackerDataService.lookupDeviceId(deviceUniqueId);
		if(dbId){
			Date startTime = date;
			Date endTime = new Date(date.getTime() + mclub.util.DateUtils.TIME_OF_ONE_DAY);
			// query db for the date
			def results = TrackerPosition.findAll("FROM TrackerPosition p WHERE p.deviceId=:dbId AND p.time >=:startTime AND p.time <= :endTime",[dbId:dbId, startTime:startTime, endTime:endTime, max:1500]);
			return results
		}else{
			log.info("Unknow device: ${deviceUniqueId}");
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
	public List<TrackerTrack> listTracksBetweenDays(String deviceUniqueId, Date beginDay, Date endDay){
		Long dbid = trackerDataService.lookupDeviceId(deviceUniqueId);
		if(!dbid){
			// device not found
			return [];
		}

		// List all tracks between that days
		def tracksInThatDays = TrackerTrack.findAll("FROM TrackerTrack tt WHERE tt.deviceId = :dbd AND tt.beginDate >=:begin AND tt.beginDate <=:end",[dbd:dbid, begin:beginDay, end:endDay]);
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
		if(pos.time.time - System.currentTimeMillis() > mclub.util.DateUtils.TIME_OF_HALF_HOUR){
			// is expired position
			return null;
		}
		
		def deviceFeatures = [];
		
		def feature_properties = [
			//'id':"fp_${device.id}",
			'title':"tk-${device.udid}",
			'udid':"${device.udid}",
			'username':'testuser',
			'phone':'12345678',
			'description':"tracker demo",
			'marker-color':"#00bcce",
			'marker-size': "medium",
			'marker-symbol': "airport",
			"marker-zoom": ""
			]
		def feature_geometry = [
			"type": "Point",
			"coordinates": [pos.longitude, pos.latitude]
			]
		
		def feature1 = [
			'type':"Feature",
			'properties':feature_properties,
			'geometry' : feature_geometry,
			//'id' : "${device.id}"
			];
		
		deviceFeatures.add(feature1)
		
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
		// Add line string feature
		int MAX_LINE_POINTS = 15;
		Date lineTime = new Date(System.currentTimeMillis() - mclub.util.DateUtils.TIME_OF_AN_HOUR);
		def positions = TrackerPosition.findAll("FROM TrackerPosition p WHERE p.deviceId=:dbId AND p.time>:lineTime ORDER BY p.time DESC",[dbId:device.id, lineTime:lineTime, max:MAX_LINE_POINTS]);
		if(positions?.size() >=4){
			
			def lineCoordinates = [];
			// build line string features if positions count >=4
			for(TrackerPosition p in positions){
				lineCoordinates.add([p.longitude,p.latitude]);
			}
			def line_feature_geometry = [
				'type': 'LineString',
				'coordinates': lineCoordinates
			];
			def line_feature_properties = [
				'udid':"${device.udid}"
			];
			def lineFeature = [
				'type':'Feature',
				'geometry':line_feature_geometry,
				'properties':line_feature_properties
			];
			deviceFeatures.add(lineFeature);
		}
		return deviceFeatures;
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
		if(device.phoneNumber)
			return device.phoneNumber;
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
