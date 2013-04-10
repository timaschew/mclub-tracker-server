package mclub.tracker

import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentHashMap

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

import org.codehaus.groovy.grails.commons.GrailsApplication

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
				new TrackerDevice(deviceId:'123456789012345').save(flush:true);
			}
			
			// mock device position
			trackerDataService.addPosition(new TrackerPosition(
				deviceId:1,
				time:new Date(),
				valid:true,
				latitude:20,
				longitude:130,
				altitude:0,
				speed:28,
				course:180
			));
			
			
			trackerDataService.addPosition(new TrackerPosition(
				deviceId:1,
				time:new Date(),
				valid:true,
				latitude:21,
				longitude:131,
				altitude:0,
				speed:29,
				course:181
			));
			trackerDataService.addPosition(new TrackerPosition(
				deviceId:1,
				time:new Date(),
				valid:true,
				latitude:22,
				longitude:132,
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
	public List<TrackerPosition> listDevicePositionOfDay(String deviceId, Date date){
		def dbId = trackerDataService.getIdByUniqueDeviceId(deviceId);
		if(dbId){
			Date startTime = date;
			Date endTime = new Date(date.getTime() + mclub.util.DateUtils.TIME_OF_ONE_DAY);
			// query db for the date
			def results = TrackerPosition.findAll("FROM TrackerPosition p WHERE p.deviceId=:dbId AND p.time >=:startTime AND p.time <= :endTime",[dbId:dbId, startTime:startTime, endTime:endTime, max:500]);
			return results
		}else{
			log.info("Unknow device: ${deviceId}");
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
	public List<TrackerTrack> listDailyTracks(String deviceId, Date beginDay, Date endDay){
		Long dbid = trackerDataService.getIdByUniqueDeviceId(deviceId);
		if(!dbid){
			// device not found
			return [];
		}
		
		// List all tracks between that days
		def tracksInThatDays = TrackerTrack.findAll("FROM TrackerTrack tt WHERE tt.deviceId = :dbd AND tt.beginDate >=:begin AND tt.beginDate <=:end",[dbd:dbid, begin:beginDay, end:endDay]);
		// collect tracks that: endDate - beginDate = 1Day
		def dailyTracks = tracksInThatDays.collect{
			// 00:00:00 ~ 23:59:59
			if(it.endDate.getTime() - it.beginDate.getTime() == mclub.util.DateUtils.TIME_OF_ONE_DAY){
				return it;
			}
		}
		return dailyTracks;
	}
}
