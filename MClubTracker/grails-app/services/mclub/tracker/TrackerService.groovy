package mclub.tracker

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
		// FIXME - may cause DB/IO performance issue for massive concurrent updates
		if(position.save(flush:true)){
			return position.id;
		}else{
			log.warn("Erro save position: ${position.errors}" )
		}
		return null;
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
			Date endTime = new Date(date.getTime() + 24 * 3600 * 1000);
			// query db for the date
			def results = TrackerPosition.findAll("FROM TrackerPosition p WHERE p.deviceId=:dbId AND p.time >=:startTime AND p.time < :endTime",[dbId:dbId, startTime:startTime, endTime:endTime, max:500]);
			return results
		}else{
			log.info("Unknow device: ${deviceId}");
		}
		return [];
	}
}
