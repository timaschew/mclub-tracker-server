package mclub.tracker
import java.util.Map;

import grails.converters.JSON
import mclub.util.DateUtils

class TrackerAPIController {
	TrackerService trackerService;
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
			return toTrackValues(deviceUniqueId, it);
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
			return toTrackValues(deviceUniqueId, it);
		}
		render results as JSON
	}
	
	def toTrackValues(String deviceUniqueId, TrackerTrack t){
		return [
			//FIXME - use deviceId may cause XSS !!!
			'deviceId':deviceUniqueId.encodeAsHTML(),
			'title':t.title,
			'beginDate':t.beginDate.time,
			'endDate':t.endDate.time,
			'description':t.description?t.description:''
			];
	}
}
