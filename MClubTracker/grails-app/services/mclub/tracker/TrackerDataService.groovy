package mclub.tracker

import grails.converters.JSON
import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentHashMap

import mclub.util.DateUtils

import org.codehaus.groovy.grails.commons.GrailsApplication

class TrackerDataService {
	GrailsApplication grailsApplication;
	ConcurrentHashMap<String,Long> idCache = new ConcurrentHashMap<String,Long>();

	/**
	 * Get device PK ID by unique device id（IMEI or callsign）
	 * @param imei
	 * @return
	 */
	public Long lookupDeviceId(String imeiOrUdid){
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
	
	/**
	 * 
	 * @param position
	 * @return
	 * @throws Exception
	 */
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
	
	/**
	 * 
	 * @param deviceId
	 * @param positionId
	 * @throws Exception
	 */
	public void updateLatestPosition(Long deviceId, Long positionId) throws Exception{
		// direct associate the position id to the device
		TrackerDevice.executeUpdate("UPDATE TrackerDevice AS d SET d.latestPositionId=:pid WHERE d.id=:did",[did:deviceId,pid:positionId]);
	}
	
	/**
	 * Update tracker position according to the received data object.
	 * @param udid
	 * @param positionData
	 */
	public void updateTrackerPosition(String udid, PositionData positionData){
		Long devicePK = lookupDeviceId(udid);
		if(devicePK == null){
			log.warn("Unknown device - " + udid);
			return;
		}
		
		// Convert value object to position entity
		TrackerPosition position = new TrackerPosition();
		position.properties = positionData;
		position.deviceId = devicePK;
		if(!positionData.extendedInfo.isEmpty()){
			def ext = positionData.extendedInfo as JSON
			position.extendedInfo = ext;
		}
		
		// Save to database
		try {
			Long id = addPosition(position);
			if (id != null) {
				updateLatestPosition(position.getDeviceId(), id);
			}
		} catch (Exception error) {
			log.warn("update postion failed, " + error.getMessage());
		}
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
