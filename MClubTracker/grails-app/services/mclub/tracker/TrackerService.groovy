package mclub.tracker

import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentHashMap

import org.codehaus.groovy.grails.commons.GrailsApplication

/**
 * The tracker service that saves data from tracker
 * @author shawn
 *
 */
class TrackerService {
	GrailsApplication grailsApplication;
	ConcurrentHashMap<String,Long> idCache = new ConcurrentHashMap<String,Long>();
	
	/**
	 * Get device db id by imei number or unique device id
	 * @param imei
	 * @return
	 */
	public Long getIdByUniqueDeviceId(String imeiOrUdid){
		// first read from cache
		// hope one day the cache blows up for too much devices ;)
		Long id = idCache.get(imeiOrUdid);
		if(id == null){
			// We store the imei in deviceId property
			id = TrackerDevice.findByDeviceId(imeiOrUdid)?.id;
			if(id){
				idCache.putIfAbsent(imeiOrUdid, id);
			}
		}
		return id;
	}
	
	public Long addPosition(TrackerPosition position) throws Exception{
		// save position
		if(!position.save(flush:true)){
			if(log.isWarnEnabled()){
				log.warn("Erro save position: ${position.errors}" );
			}
			return null;
		}
		
		// create dialy track if necessary
		Calendar cal = Calendar.getInstance();
		cal.setTime(position.getTime());
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		Date theDay = cal.getTime();
		
		Date begin = theDay;
		Date end = new Date(theDay.getTime() + mclub.util.DateUtils.TIME_OF_ONE_DAY);
		def c = TrackerTrack.executeQuery("SELECT count(*) FROM TrackerTrack tt WHERE tt.deviceId=:did AND tt.beginDate=:begin AND tt.endDate=:end",[did:position.deviceId,begin:begin,end:end]);
		 
		if(c.size() == 1 && 0 == c[0]){
			// create one
			TrackerTrack t = new TrackerTrack();
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			t.title = "Daily Track of " + format.format(begin);
			t.deviceId = position.deviceId;
			t.beginDate = begin;
			t.endDate = end;
			if(!t.save(flush:true)){
				if(log.isWarnEnabled()){
					log.warn("Error save track ${t.title}, error: ${t.errors}");
				}
			}
		}
		// ...
		return position.id;
	}
	
	public void updateLatestPosition(Long deviceId, Long positionId) throws Exception{
		// direct associate the position id to the device
		TrackerDevice.executeUpdate("UPDATE TrackerDevice AS d SET d.latestPositionId=:pid WHERE d.id=:did",[did:deviceId,pid:positionId]);
	}
	
	/**
	 * Bridge methods for Java POJO to access the grails configuration
	 * @return
	 */
	public Map<String,Object> getConfig(){
		return grailsApplication.getFlatConfig();
	}
	
	/**
	 * Get config by key
	 * @param key
	 * @return
	 */
	public Object getConfig(String key){
		return grailsApplication.getFlatConfig().get(key);
	}
	
	/**
	 * List device position of day
	 * @param deviceId
	 * @param date
	 * @return
	 */
	public List<TrackerPosition> listDevicePositionOfDay(String deviceId, Date date){
		def dbId = this.getIdByUniqueDeviceId(deviceId);
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
		Long dbid = this.getIdByUniqueDeviceId(deviceId);
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
