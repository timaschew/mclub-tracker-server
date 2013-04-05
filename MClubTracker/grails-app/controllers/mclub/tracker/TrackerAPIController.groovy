package mclub.tracker
import grails.converters.JSON
import mclub.util.DateUtils

class TrackerAPIController {
	TrackerService trackerService;
	def index(){
		render text:'Tracker API 0.1'
	}
	
	/**
	 * 
	 * @param date of millisecond to search
	 * @return
	 */
    def list_positions() {
		// the device id
		def deviceId = params.id;
		if(!deviceId){
			deviceId = '353451048729261';// test purpose
		}
		
		// Get the query date
		Date date;
		def dateLong = params.long('date');
		if(!dateLong){
			date = DateUtils.today();
		}else{
			date = new Date(dateLong);
		}
		
		if(date){
			render text:"${date}/${date.time}"
		} else{
			render text:'OK'
		}
		def results = trackerService.listDevicePositionOfDay(deviceId, date)
		/*
		.collect{ p->
			//TODO: null value and date time
			return [
				'id':p.id, 
				'address':p.address,
				'altitude':p.altitude,
				'course':p.course,
				'deviceId':p.deviceId,
				'extendedInfo':p.extendedInfo,
				'latitude':p.latitude,
				'longitude':p.longitude,
				'power':p.power,
				'speed':p.speed,
				'time':p.time,
			];
		};
		*/
		render results as JSON
	}
}
