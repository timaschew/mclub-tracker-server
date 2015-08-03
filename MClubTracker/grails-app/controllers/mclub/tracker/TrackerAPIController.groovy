package mclub.tracker
import java.util.Map;

import grails.converters.JSON
import mclub.util.DateUtils

class TrackerAPIController {
	TrackerService trackerService;
	TrackerDataService trackerDataService;
	
	def index(){
		render text:'Tracker API 0.1'
	}
	
	/**
	 * List device positions of a specific date
	 *
	 * @param id - the device unique id or imei
	 * @param date - the date to query positions
	 * 
	 * @return
	 */
    def daily_positions() {
		// the device id
		def deviceUniqueId = params.id;
		if(!deviceUniqueId){
			deviceUniqueId = '353451048729261';// test purpose
		}
		
		// Get the query date
		Date date;
		def dateLong = params.long('date');
		if(!dateLong){
			date = DateUtils.today();
		}else{
			date = new Date(dateLong);
		}

		def results = trackerService.listDevicePositionOfDay(deviceUniqueId, date)
		.collect{ p->
			//TODO: null value and date time
			return [
				//'rowid':p.id, 
				'address':p.address,
				'altitude':p.altitude,
				'course':p.course,
				//'deviceId':p.deviceId,
				'extendedInfo':p.extendedInfo,
				'latitude':p.latitude,
				'longitude':p.longitude,
				'power':p.power,
				'speed':p.speed,
				'time':p.time.getTime()
			];
		};
		render results as JSON
	}
	
	/**
	 * List tracks in daily basis
	 * 
	 * @param begin - the begin date of track
	 * @param end - the end date of track
	 * 
	 * @return Array of tracks if found or empty
	 */
	def daily_tracks(){
		// the device id
		def deviceUniqueId = params.id;
		if(!deviceUniqueId){
			deviceUniqueId = '353451048729261';// test purpose
		}
		
		Date begin,end;
		def beginLong = params.long('begin');
		def endLong = params.long('end');
		if(beginLong!=null){
			begin = new Date(beginLong);			
		}
		if(endLong!=null){
			end = new Date(endLong)
		}
		
		if(begin && !end){
			// after 15 days
			end = new Date(begin.getTime() + 30 * DateUtils.TIME_OF_ONE_DAY);
		}else if(!begin && end){
			// before 15 days
			begin = new Date(end.getTime() - 30 * DateUtils.TIME_OF_ONE_DAY);
		}else if(!begin && !end){
			// from today and 15 days ago
			end = DateUtils.today();
			begin = new Date(end.getTime() - 30 * DateUtils.TIME_OF_ONE_DAY);
		}
		
		def tracks = trackerService.listTracksBetweenDays(deviceUniqueId, begin, end);
	
		def results = tracks.collect{
			return convertToTrackValues(deviceUniqueId, it);
		}
		render results as JSON;
	}
	
	/**
	 * List tracks in monthly basis
	 * 
	 * @param id - device id
	 * @param month - The 'yyyymm' string yearmonth, eg:201301. current month will be used if not specified. 
	 * 
	 * @return
	 */
	def monthly_tracks(){
		// the device id
		def deviceUniqueId = params.id;
		if(!deviceUniqueId){
			deviceUniqueId = '353451048729261';// test purpose
		}
		
		// get the begin/end of that month
		Calendar cal = DateUtils.getCalendar();
		int y,m;
		String monthStr = params['month'];
		if(monthStr != null){
			// parse the month
			try{
				y = Integer.parseInt(monthStr.substring(0,4));
				m = Integer.parseInt(monthStr.substring(4,6)) - 1;
			}catch(Exception e){
				// noop
				render text:'[]'
				return;
			}
		}else{
			y = cal.get(Calendar.YEAR);
			m = cal.get(Calendar.MONTH);
		}
		
		Date begin,end;
		cal.clear();
		cal.set(Calendar.YEAR,y);
		cal.set(Calendar.MONTH,m);
		begin = cal.getTime();
		cal.add(Calendar.MONTH, 1);
		cal.add(Calendar.MILLISECOND, -1);
		end = cal.getTime();
		
		if(log.infoEnabled){
			log.info("begin: ${begin} / end: ${end}");
		}
		
		def tracks = trackerService.listTracksBetweenDays(deviceUniqueId, begin,end);
		def results = tracks.collect{
			return convertToTrackValues(deviceUniqueId, it);
		}
		render results as JSON
	}
	
	private Map<String,Object> convertToTrackValues(String deviceUniqueId, TrackerTrack t){
		return [
			//FIXME - use deviceId may cause XSS !!!
			'deviceId':deviceUniqueId.encodeAsHTML(),
			'title':t.title,
			'beginDate':t.beginDate.time,
			'endDate':t.endDate.time,
			'description':t.description?t.description:''
			];
	}
	
	/**
	 * HTTP Interface for device position update.
	 * @param udid
	 * @param positionData
	 * @return
	 */
	def update_position(String udid, String lat, String lon,String speed, String course /*PositionData positionData*/){
		if(!udid || !lat || !lon){
			render text:"Missing parameters: udid, lat, lon"
			return;
		}
		PositionData pos = new PositionData();
		pos.latitude = Long.parseLong(lat);
		pos.longitude = Long.parseLong(lon);
		pos.altitude = 0.0;
		pos.speed = speed?Long.parseLong(speed):0.0;
		pos.course = course?Long.parseLong(course):0.0;
		pos.time = new Date();
		pos.valid = true;
		pos.extendedInfo['protocol'] = 'http_api';
		trackerDataService.updateTrackerPosition(udid, pos);
		
		render text:'OK'
	}
	
	/**
	 * Returns the geojson of device position
	 * @param udid
	 * @return
	 */
	def geojson(String udid){
		if(!udid){
			udid = params.id
		}
		if(!udid){
			render text:"Missing udid";
			return;
		}
		
		def devices = [];
		if(udid.equals("*")){
			devices  = TrackerDevice.list();
		}else{
			def dev = TrackerDevice.findByUdid(udid);
			if(dev) devices << dev; 
		}
		
		def featureCollection = [:];
		
		def features = [];
		devices?.each{ dev->
			features << buildGeojsonFeature(dev);
		}
		if(!features.isEmpty()){
			featureCollection['type'] = 'FeatureCollection';
			featureCollection['features'] = features; 
			featureCollection['id'] = 'mclub_tracker_livepositions';
		}
		
		response.setHeader('Access-Control-Allow-Origin',"*")
		response.setHeader('X-Powered-By', "BG5HHP")
		render featureCollection as JSON;
	}
	
	/*
	 {
	   "type": "Feature",
	   "properties": {
		 "id": "ire3k295",
		 "title": "Cornell University",
		 "description": "<div style=\"width:330px;height:240px;overflow:auto\"><div class=\"googft-info-window\" style=\"font-family:sans-serif\"> <b>Governmental Body/Entity:</b> Cornell University<br><p> <b>Type of Drone:</b> University Built (One-Third Scale Piper Cub) UAS</p><p><b>Status:</b> Expired</p><p> <b>General Location of Drone Activity:</b> Aurora, NY</p><p> <b>Stated objective/purpose of COA:</b> The purpose of the proposed UAV flights in this COA is to develop and use experimentally, a system to vertically profile the atmosphere from 300 ft agl to 3000 ft agl.  The UAV payload will be instrumentation to continuously measure temperature, relative humidity, wind speed and wind direction as the UAV spirals vertically from 300 ft agl to 3000 ft agl and back down to 300 ft agl.  The UAV will maintain a GPS controlled circle with a 1500 ft diameter utilizing a Micropilot 1028 autopilot.  The climb rate will be 150 ft/min and the duration of the flight will be approximately 40 min. This system will replace the release of helium filled balloons with radio equipped weather packages and the use of tethered blimps utilized to lift weather instrumentation into the lower atmosphere.   An UAV mounted system is superior because the helium filled free flying balloons rise through the lower 3000 ft of the atmosphere too quickly for accurate weather data.  The tethered blimps have a FAA imposed altitude restriction of 1000-1500 ft due to their danger to aircraft and the inability of the operators to react quickly enough to avoid full scale aircraft.  In contrast, a UAV mounted system can spiral up to 3000 ft in a relatively short time period, yielding high quality weather measurements and can quickly be diverted to avoid full scale aircraft which enter into the area of UAV flights.</p><p> <b>Effective Dates:</b> 3/1/2010-2/28/2011</p><p> <b>Comments:</b> </p><p><b>Link to Records:</b> <a href=\"https://www.eff.org/document/cornell-university-drone-records\" target=\"_blank\">https://www.eff.org/document/cornell-university-drone-records</a></p> </div></div>",
		 "marker-color": "#00bcce",
		 "marker-size": "medium",
		 "marker-symbol": "airport",
		 "marker-zoom": ""
	   },
	   "geometry": {
		 "type": "Point",
		 "coordinates": [
		   -76.702448,
		   42.753959
		 ]
	   },
	   "id": "ci9bzhum74haejrlvrn7i8fep"
	 }
	 
	 */
	private Map<String,Object> buildGeojsonFeature(TrackerDevice device){
		TrackerPosition pos = TrackerPosition.get(device.latestPositionId);
		if(!pos){
			return null;
		}
		def feature_properties = [
			'id':"fp_${device.id}",
			'title':"tk-${device.udid}",
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
		
		def feature = [
			'type':"Feature",
			'properties':feature_properties,
			'geometry' : feature_geometry,
			'id' : "${device.id}"
			];
		return feature;
	}
	
	/**
	 * Returns an array of device positions.
	 * @param udid
	 * @return
	 */
	def live_positions(String udid){
		if(!udid){
			udid = params.id
		}
		if(!udid){
			render text:"Missing udid";
			return;
		}
		
		def allDevicePositions = [];
		
		if(udid.equals("*")){
			// load all tracker's latest positions
			def devices = TrackerDevice.list();
			devices?.each{ dev->
				allDevicePositions << trackerService.buildDevicePositionValues(dev);
			}
		}else{
			TrackerDevice dev = TrackerDevice.findByUdid(udid);
			if(dev){
				allDevicePositions << trackerService.buildDevicePositionValues(dev);
			}
		}
		
		render allDevicePositions as JSON;
	}
}
