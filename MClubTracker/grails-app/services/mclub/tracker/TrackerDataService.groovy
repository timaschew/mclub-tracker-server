package mclub.tracker

import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentHashMap

import mclub.util.DateUtils

import org.codehaus.groovy.grails.commons.GrailsApplication

class TrackerDataService {
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
			id = TrackerDevice.findByUdid(imeiOrUdid)?.id;
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
		Calendar cal = DateUtils.getCalendar();
		cal.setTime(position.getTime());
		int y = cal.get(Calendar.YEAR);
		int m = cal.get(Calendar.MONTH);
		int d = cal.get(Calendar.DATE);
		cal.clear();
		cal.set(Calendar.YEAR, y);
		cal.set(Calendar.MONTH, m);
		cal.set(Calendar.DATE, d);
		
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
}
