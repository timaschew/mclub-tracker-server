package mclub.tracker
import grails.converters.JSON
import grails.validation.Validateable
import mclub.sys.ConfigService
import mclub.tracker.protocol.helper.DateBuilder
import mclub.user.User
import mclub.user.UserService
import mclub.user.UserService.UserSession
import mclub.util.DateUtils

import org.codehaus.groovy.runtime.InvokerHelper
import org.h2.util.MathUtils
import org.springframework.beans.PropertyEditorRegistrar
import org.springframework.beans.PropertyEditorRegistry
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.web.multipart.MultipartFile

import com.github.davidmoten.geo.*

import java.text.SimpleDateFormat;


class TrackerAPIController {
	TrackerService trackerService;
	TrackerDataService trackerDataService;
	TrackerReportService trackerReportService;

	UserService userService;
	ConfigService configService;
	
	def index(){
		render text:'Tracker API 0.1'
	}
	
	def upload_avatar() {
		MultipartFile file = request.getFile("avatar")
		file.transferTo(new File("/tmp/upload/avatar"))
	}
	
	def update_position2(PositionUpdateCommand updateCommand){
		if(updateCommand.hasErrors()){
			render APIResponse.ERROR("Invalid parameters") as JSON
			return;
		}

		// Check user session, SecurityFilter will set in request scope
		UserSession usession = request['session'];
		if(!usession){
			log.warn("Session not found in request, check SecurityFilter configurations!")
			render APIResponse.ERROR(APIResponse.SESSION_EXPIRED_ERROR,"Session expired") as JSON
			return;
		}
		String username = usession.username;
		if(usession.type < 2){
			// disabled account or guest has no permission to update position.
			log.warn("User ${username} (type=${usession.type}) is not allowed to update position!")
			render APIResponse.ERROR(APIResponse.OPERATION_FAIL_ERROR,"No allowed") as JSON
			return;
		}

		PositionData pos = new PositionData();
		InvokerHelper.setProperties(pos,updateCommand.properties);
		
		// Messages
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

		// altitude/speed/course is required by the domain constraints but not mandatory in API parameters		
		if(pos.altitude == null) pos.altitude = -1;
		if(pos.speed == null) pos.speed = -1;
		if(pos.course == null) pos.course = -1;
		
		// convert timestamp seconds to Time object
		Long timestamp = updateCommand.timestamp;
		if(timestamp!= null && (Math.abs(System.currentTimeMillis() - (timestamp * 1000)) < MAX_TIME_DRIFT) ){
			pos.time = new Date(timestamp * 1000);
		}else{
			pos.time = new Date();
		}
		
		pos.extendedInfo['protocol'] = 'http_api';
		pos.valid = true;
		
		trackerDataService.updateTrackerPosition(pos);
		
		render APIResponse.OK() as JSON
	}
	
	private static final long MAX_TIME_DRIFT = 30 * 1000;
	
	/**
	 * HTTP Interface for device position update.
	 * @param udid
	 * @param positionData
	 * @return
	 */
	def update_position(String udid, String lat, String lon,String speed, String course, Integer coordinateType, Long timestamp, String message, Integer messageType){
		if(!lat || !lon){
			render APIResponse.ERROR("Missing parameters: lat, lon") as JSON
			return;
		}
		if(!udid){
			render APIResponse.ERROR("Missing parameters: udid") as JSON
			return;
		}

		// Check user session - FIXME - use filter
		UserSession usession = request['session'];
		if(!usession){
			log.warn("Session not found in request, check SecurityFilter configurations!")
			render APIResponse.ERROR(APIResponse.SESSION_EXPIRED_ERROR,"Session expired") as JSON
			return;
		}
		
		String username = usession.username;
		if(usession.type < 2){
			// disabled account or guest has no permission to update position.
			log.warn("User ${username} (type=${usession.type}) is not allowed to update position!")
			render APIResponse.ERROR(APIResponse.OPERATION_FAIL_ERROR,"No allowed") as JSON
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
		
		if(timestamp!= null && (Math.abs(System.currentTimeMillis() - (timestamp * 1000)) < MAX_TIME_DRIFT) ){
			pos.time = new Date(timestamp * 1000);
		}else{
			pos.time = new Date();
		}
		
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
		
		pos.coordinateType = coordinateType;
		trackerDataService.updateTrackerPosition(pos);
		
		render APIResponse.OK() as JSON
	}
	
	/**
	 * Returns the geojson of device position
	 * @param udid
	 * @return FeatureCollection in GeoJSON of the desired devices latest position.
	 */
	def geojson(TrackerDeviceFilter filter){
		def allDevices = [];
		Double positionId = null;

		// Case1 - query by map id
		if(params.map){
			// load device by map id
			TrackerMap map = TrackerMap.findByUniqueId(params.map);
			if(map){
				// load the tracker filters
				def filters = map.loadFilters();
				for(TrackerDeviceFilter f : filters){
					def devs = trackerService.findTrackerDevices(f);
					if(devs?.size() > 0){
						allDevices.addAll(devs);
					}
				}
			}
		}else{
			// Case3 - query by device id(s), which including
			// - geojson/BG5XYZ-9 - full id match
			// - geojson/BG5    - fuzzy id match
			// - geojson/BG5XYZ-9#ID - full id match, with specific position id
			// - geojson/all

			// read geojson/$id as udid
			if(filter.udid == null && params.id){
				filter.udid = params.id;
			}

			// udid == all means null, just for compatible
			if("all".equalsIgnoreCase(filter.udid)){
				filter.udid = null;
			}

			if(filter.udid != null){
				// query by udid, if udid contains '$', means load specific position id
				String originalUdid = filter.udid;
				if(filter.udid.indexOf('$') > 0){
					String[] s = filter.udid.split('\\$');
					filter.udid = s[0];
					try{
						positionId = Double.parseDouble(s[1])
					}catch(Exception e){
						// ignore the parse error
						log.info("Error parsing position id from parameter: ${e.message}");
					}
				}
				// load devices according to the filters
				allDevices = trackerService.findTrackerDevices(filter);
				if(positionId && allDevices.size() > 1){
					log.warn("Invalid udid/pos_id ${originalUdid}, expect one device but ${allDevices.size()} devices returned")
					allDevices.clear();
					positionId = null;
				}
			}else{
				// no udid specified, so check the bounds parameter
				if(filter.bounds == null){
					log.warn("geojson API received requst for ALL device/data, THIS IS SLOW and should to be avoided!");
					allDevices = trackerService.findTrackerDevices(filter);
				}else{
					def bounds = filter.getBoundsCoordinate();
					if(bounds) {
						/*
						DateBuilder db = new DateBuilder();
						def time = db.setYear(2016).setMonth(1).setDay(1).getDate()
						*/
						allDevices = trackerService.findTrackerDevicesInBounds(bounds[0], bounds[1], bounds[2], bounds[3],null,filter.type);
						if(log.debugEnabled && (allDevices?.size() == 0)){
							log.debug "No device (type=${filter.type})found in bound ${bounds[0]},${bounds[1]},${bounds[2]},${bounds[3]}"
						}
					}
				}
			}
		}

		// Load features by devices
		def featureCollectionOfGeoJSON = [:];

		if(positionId && allDevices?.size() == 1){
			// just load device with specific position data
			TrackerDevice dev = allDevices[0]; // make sure (allDevices.length == 1)
			TrackerPosition pos = TrackerPosition.findById(positionId);
			if(pos && dev.id.equals(pos.device?.id)){
				featureCollectionOfGeoJSON = trackerService.buildDeviceFeatureCollection(dev,pos,false);
			}else{
				log.info("Device ${dev.udid} does not contain position #${positionId}");
			}
		}else if(filter.historyTime && allDevices?.size() == 1){
			// load historical data of a device
			TrackerDevice dev = allDevices[0];
			featureCollectionOfGeoJSON = trackerService.buildHistoricalDeviceFeatureCollection(dev,filter.historyTime);

		}else if(allDevices.size() > 0) {
			featureCollectionOfGeoJSON = trackerService.buildGeojsonFeatureCollection(allDevices);
		}

		// Allow browser XSS
		//response.setHeader('Access-Control-Allow-Origin',"*")
		response.setHeader('X-Powered-By', "BG5HHP(shawn.chain@gmail.com)")
		render featureCollectionOfGeoJSON as JSON;
	}

//	/**
//	 * Returns an array of device positions.
//	 * @param udid
//	 * @return an array of device positions
//	 */
//	def live_positions(String udid){
//		if(!udid){
//			udid = params.id
//		}
//		if(!udid){
//			render text:"Missing udid";
//			return;
//		}
//
//		def allDevicePositions = [];
//
//		if(udid.equals("*")){
//			// load all tracker's latest positions
//			def devices = TrackerDevice.list();
//			devices?.each{ dev->
//				allDevicePositions << trackerService.getDeviceJsonData(dev);
//			}
//		}else{
//			TrackerDevice dev = TrackerDevice.findByUdid(udid);
//			if(dev){
//				allDevicePositions << trackerService.getDeviceJsonData(dev);
//			}
//		}
//
//		render allDevicePositions as JSON;
//	}
	
	def clean_aprs_data(){
		Integer daysToPreserve = configService.getConfigInt("tracker.aprs.data.daysToPreserve");
		if(!daysToPreserve){
			daysToPreserve = 3; // by default will keep 7 days data
		}
		def r = [result:trackerDataService.deleteAprsPosition(daysToPreserve)];
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
		
		// check whether phone number is registered.
		User user;
		user = User.findByPhone(phone);
		if(user){
			// 目前只允许 一个手机对应一个号码对应一个用户。
			//TODO - To support one phone number to many devices, 
			// perform the authentication here and returns the existing user 
			result = APIResponse.ERROR("Phone number is occupied");
			render result as JSON;
			return;
		}
		
		// generate user name uXXXXYYYY, where XXXX is last 4 digi of udid and YYYY is last 4 digi of phone
		String username = "u" + udid.substring(udid.length() - 4) + phone.substring(phone.length() - 4);
		// check whether user exists
		user = User.findByName(username);
		if(user){
			// user exists, bail out
			result = APIResponse.ERROR("User already exists");
			render result as JSON;
			return;
		}
		
		// create user but disabled by default.
		Integer regUserType = configService.getConfigInt("user.register.default.type");
		if(regUserType == null)regUserType = User.USER_TYPE_DISABLED;

		User u = new User(name:username, displayName:display_name, phone:phone, type:regUserType, creationDate:new java.util.Date(),avatar:'',settings:'');
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
		
		result = APIResponse.SUCCESS("Device register success");
		result['username'] = username;
		log.info("Device ${udid}/${phone} registered OK, username: ${username}");
		
		/*
		//  Note: for hobby system, we could by default enable user and perform login here;
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
		*/
		
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
		def uSession;
		String authErrMsg;
		try{
			if(mclub.user.AuthUtils.isMobilePhoneNumber(username)){
				uSession = userService.loginByPhone(username, password,false);
			}else{
				uSession = userService.login(username,password,false);
			}
		}catch(Exception e){
			authErrMsg = e.getMessage();
		}
		if(uSession && uSession.token){
			//NOTE: param:username might by phone number!!
			// if udid is specified, save it and associate with current user.
			if(udid){
				TrackerDevice device = TrackerDevice.findByUdid(udid);
				if(!device){
					// create a new one
					device = new TrackerDevice(udid:udid, username:uSession.username, status:0);
					if(device.save(flush:true)){
						log.info("New device ${udid} registered by logged in user ${username}/${uSession.username}")
					}else{
						log.warn("Failed to save device, ${device.errors}");
					}
				}else{
					// device already exists, need to check whether it's the same user that associated with
					if(!uSession.username.equals(device.username)){
						// the device is associated with other users
						result = APIResponse.ERROR("Device is binded to another user");
						render result as JSON;
						return;
					}
					// No need to to associate here, because later when data reported, it will associate automatically
				}
			}
			result = APIResponse.SUCCESS("Login success");
			result['token'] = uSession.token;
			result['username'] = uSession.username;
			log.info("User ${username}/${uSession.username} login ok");
		}else{
			if(!authErrMsg) authErrMsg = "user not found";
			result = APIResponse.ERROR(APIResponse.AUTH_DENIED_ERROR,"Login failed, ${authErrMsg}");
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

	def test(String bound){
		if(!bound){
			render text:"missing bound";
			return;
		}

		String[] bounds = bound.split(",");
		if(bounds.length != 4){
			render text:"invalid bound param";
			return;
		}

		try{
			double lat1 = Double.parseDouble(bounds[0]);
			double lon1 = Double.parseDouble(bounds[1]);
			double lat2 = Double.parseDouble(bounds[2]);
			double lon2 = Double.parseDouble(bounds[3]);

			Coverage coverage = GeoHash.coverBoundingBox(lat1,lon1,lat2,lon2);
            String h1 = GeoHash.encodeHash(lat1,lon1);
            String h2 = GeoHash.encodeHash(lat2,lon2);
            def hash = coverage.getHashes();
            render text:"hash1: ${h1}, hash2: ${h2}, geohash: ${hash}"
		}catch(Exception e){
            render text:"error: ${e.message}"
		}
	}
}

@Validateable(nullable=true)
class PositionUpdateCommand{
	static constraints = {
		udid blank:false, nullable:false
		latitude blank:false, nullable:false
		longitude blank:false, nullable:false
	};

	String udid;		// Unique device ID
	Double latitude;
	Double longitude;
	
	Double altitude;
	Double speed;		// Speed in KM/h
	Double course;		// Course from 0 ~ 360
	
	Integer coordinateType;	// Coordinate type 0 means WGS84 1 means GCJ02
	String message;			// Associated message
	Integer messageType;
	Long	timestamp;		// Timestamp in seconds
}


//class CustomDateEditorRegistrar implements PropertyEditorRegistrar {
//	public void registerCustomEditors(PropertyEditorRegistry registry) {
//		String dateFormat = 'yyyy-MM-dd'
//		registry.registerCustomEditor(Date, new CustomDateEditor(new SimpleDateFormat(dateFormat), true))
//	}
//}