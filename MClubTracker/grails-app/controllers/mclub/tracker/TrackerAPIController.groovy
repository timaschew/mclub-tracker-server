package mclub.tracker
import java.util.Map;

import grails.converters.JSON
import mclub.user.User
import mclub.user.UserService;
import mclub.user.UserService.UserSession
import mclub.util.DateUtils

class TrackerAPIController {
	TrackerService trackerService;
	TrackerDataService trackerDataService;
	UserService userService;
	
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
	def update_position(String udid, String lat, String lon,String speed, String course, Integer coordinateType/*PositionData positionData*/,String token, String aprscall, String message, Integer messageType){
		if(!lat || !lon){
			render APIResponse.ERROR("Missing parameters: lat, lon") as JSON
			return;
		}
		
		// Check user session - FIXME - use filter
		UserSession usession = null;
		if(token){
			usession = userService.checkSessionToken(token);
		}
		if(!usession){
			log.debug("update_position rejected due to session expired");
			render APIResponse.ERROR(APIResponse.SESSION_EXPIRED_ERROR,"Session expired") as JSON
			return;
		}
		
		String username = usession.username;
		
		// extract aprs if found
		boolean isAprs = false;
		if(!username && aprscall){
			String[] aprs = mclub.user.AuthUtils.extractAPRSCall(aprscall);
			if(aprs != null){
				username = aprs[0];
				udid = aprscall; // replace the udid with aprscall like "BG5HHP-12"
				isAprs = true;
				log.info("APRS position ${aprscall}");
			}
		}
		if(!udid){
			render APIResponse.ERROR("Missing parameters: udid") as JSON
			return;
		}
		//token/call -> username -> device -> position
		
		PositionData pos = new PositionData();
		pos.username = username;
		pos.udid = udid;
		pos.latitude = Double.parseDouble(lat);
		pos.longitude = Double.parseDouble(lon);
		pos.altitude = 0.0;
		pos.speed = speed?Double.parseDouble(speed):0.0;
		pos.course = course?Double.parseDouble(course):0.0;
		pos.time = new Date();
		
		pos.message = message; // position may contains messages
		pos.messageType = messageType;
		if(pos.message && !pos.messageType){
			String m = pos.message.toLowerCase();
			if(m.indexOf("emergency") >= 0) 
				pos.messageType = TrackerPosition.MESSAGE_TYPE_EMERGENCY;
			else if(m.indexOf("alert") >=0)
				pos.messageType = TrackerPosition.MESSAGE_TYPE_ALERT;
			else
				pos.messageType = TrackerPosition.MESSAGE_TYPE_NORMAL;
		}
		if(pos.message && pos.messageType > TrackerPosition.MESSAGE_TYPE_NORMAL){
			log.warn("${pos.username} sends ALERT/EMERGENCY message: ${pos.message}");
		}
		pos.valid = true;
		pos.extendedInfo['protocol'] = 'http_api';
		if(isAprs){
			pos.aprs = true;
		}
		pos.coordinateType = coordinateType;
		trackerDataService.updateTrackerPosition(pos);
		
		render APIResponse.OK() as JSON
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
	/**
	 * Returns the geojson of device position
	 * @param udid
	 * @return FeatureCollection in GeoJSON of the desired devices latest position.
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
		if(udid.equals("all")){
			devices  = TrackerDevice.list();
		}else{
			def devs = TrackerDevice.findAllByUdidLike("${udid}%");
			if(devs?.size() > 0) devices.addAll(devs); 
		}
		
		def featureCollection = [:];
		
		def features = [];
		devices?.each{ dev->
			def dfeatures = trackerService.buildDevicePositionGeojsonFeatures(dev);
			if(dfeatures)
				features.addAll(dfeatures);
		}
		if(!features.isEmpty()){
			featureCollection['type'] = 'FeatureCollection';
			featureCollection['features'] = features; 
			featureCollection['id'] = 'mclub_tracker_livepositions';
		}
		
		// Allow browser XSS
		response.setHeader('Access-Control-Allow-Origin',"*")
		response.setHeader('X-Powered-By', "BG5HHP")
		render featureCollection as JSON;
	}
	
	/**
	 * Returns an array of device positions.
	 * @param udid
	 * @return an array of device positions
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
				allDevicePositions << trackerService.buildDevicePositionJsonData(dev);
			}
		}else{
			TrackerDevice dev = TrackerDevice.findByUdid(udid);
			if(dev){
				allDevicePositions << trackerService.buildDevicePositionJsonData(dev);
			}
		}
		
		render allDevicePositions as JSON;
	}
	
	def clean_aprs_data(){
		def r = [result:trackerDataService.deleteAprsPosition(7)];
		render r as JSON;
	}
	
	/**
	 * Device registration API
	 * @param udid
	 * @param phone
	 * @param password
	 * @param display_name
	 * @return
	 */
	def register(String udid, String phone, String password, String display_name){
		def result;
		
		if(!udid){
			udid = request.JSON.udid;
		}
		if(!phone){
			phone = request.JSON.phone;
		}
		if(!password){
			password = request.JSON.password;
		}
		if(!display_name){
			display_name = request.JSON.display_name;
		}
		if(udid?.length() < 4 || phone?.length() < 11 || !password || !display_name){
			result = APIResponse.ERROR("Missing or invalid parameters");
			render result as JSON;
			return;
		}
		
		// check whether device exists
		TrackerDevice device = TrackerDevice.findByUdid(udid);
		if(device){
			// device exists, bail out
			result = APIResponse.ERROR("Device already registered");
			render result as JSON
			return;
		}
		
		// generate user name uXXXXYYYY, where XXXX is last 4 digi of udid and YYYY is last 4 digi of phone
		String username = "u" + udid.substring(udid.length() - 4) + phone.substring(phone.length() - 4);
		
		// check whether user exists
		User user = User.findByName(username);
		if(user){
			// user exists, bail out
			result = APIResponse.ERROR("User already exists");
			render result as JSON;
			return;
		}
		
		// create user and device
		User u = new User(name:username, displayName:display_name, phone:phone, type:User.USER_TYPE_USER, creationDate:new java.util.Date(),avatar:'',settings:'');
		if(!userService.createUserAccount(u, password)){
			log.warn("Error creating user account" + u.errors);
			result = APIResponse.ERROR("Error creating user account");
			render result as JSON;
			return;
		}
		
		// device is created but disabled by default.
		device = new TrackerDevice(udid:udid,username:username, status:TrackerDevice.DEVICE_TYPE_DEACTIVED);
		if(!device.save(flush:true)){
			log.warn("Error creating device" + u.errors);
			result = APIResponse.ERROR("Error creating device");
			render result as JSON;
			return;
		}
		
		//  now performing login
		String token = userService.login(username, password)?.token;
		if(token){
			result = APIResponse.SUCCESS("Device registreed and login success");
			result['token'] = token;
			result['username'] = username;
			log.info("Device ${udid}/${phone} registered and login as ${username} OK");
		}else{
			// must be something wrong!
			log.error("Error login user with automatically created user account, username:${username}, phone:${phone}, udid:${udid}");
			result = APIResponse.ERROR(APIResponse.AUTH_DENIED_ERROR,"Login failed");
		}
		render result as JSON;
	}
	
	
	/**
	 * User login API
	 * @param name
	 * @param password
	 * @return
	 */
	def login(String username, String password, String udid){
		if(!username && !password){
			// try to read from post body
			username = request.JSON.username;
			password = request.JSON.password
		}
		if(!udid){
			udid = request.JSON.udid;
		}
		
		def result;
		String token = userService.login(username, password)?.token;
		if(token){
			// if udid is specified, save it and associate with current user.
			if(udid){
				TrackerDevice device = TrackerDevice.findByUdid(udid);
				if(!device){
					// create a new one
					device = new TrackerDevice(udid:udid, username:username, status:0);
					if(device.save(flush:true)){
						log.info("New device ${udid} registered by logged in user ${username}")
					}else{
						log.warn("Failed to save device, ${device.errors}");
					}
				}else{
					// No need to to associate here, because later when data reported, it will associate automatically
				}
			}
			result = APIResponse.SUCCESS("Login success");
			result['token'] = token;
			log.info("User " + username + " login ok");
		}else{
			result = APIResponse.ERROR(APIResponse.AUTH_DENIED_ERROR,"Login failed");
		}
		render result as JSON;
	}
	
	/**
	 * User API 
	 * @param token
	 * @return current user profiles
	 */
	def user(String token){
//		// Session token is checked in the apiFilter
		String username = request['session'].username;
		User user = User.findByName(username);
		if(user){
			def u = [:]; 
			u['name'] = user.name;
			u['phone'] = user.phone;
			u['displayName'] = user.displayName;
			u['avatar'] = user.avatar;
			u['settings'] = user.settings;
			u['creationDate'] = user.creationDate;
			def r = APIResponse.OK();
			r['user'] = u;
			render r as JSON;
		}else{
			render [:] as JSON;
		}
	}
	
	/**
	 * API Response wrapper
	 * @author shawn
	 *
	 */
	public static class APIResponse{
		public static final int NO_ERROR = 0
		public static final int OPERATION_FAIL_ERROR = 1;
		public static final int SESSION_EXPIRED_ERROR = 2;
		public static final int AUTH_DENIED_ERROR = 3;
		
		public static Map<String,Object> OK(){
			Map<String,Object> resp = ['code':NO_ERROR, 'message':'OK'];
			return resp;
		}
		
		public static Map<String,Object> SUCCESS(String message){
			Map<String,Object> resp = ['code':NO_ERROR, 'message':message];
			return resp;
		}

		
		public static Map<String,Object> ERROR(String message){
			Map<String,Object> resp = ['code':OPERATION_FAIL_ERROR, 'message':message];
			return resp;
		}
		
		public static Map<String,Object> ERROR(int code, String message){
			Map<String,Object> resp = ['code':code, 'message':message];
			return resp;
		}
		
	}
}
