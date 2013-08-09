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
