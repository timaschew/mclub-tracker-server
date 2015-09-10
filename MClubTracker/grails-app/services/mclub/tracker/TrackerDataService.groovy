package mclub.tracker

import grails.converters.JSON
import grails.transaction.Transactional;

import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

import mclub.util.DateUtils
import mclub.tracker.aprs.AprsData;

import org.codehaus.groovy.grails.commons.GrailsApplication

@Transactional
class TrackerDataService {
	GrailsApplication grailsApplication;
	TrackerCacheService trackerCacheService;
	ConcurrentHashMap<String,Long> idCache = new ConcurrentHashMap<String,Long>();

	static {
		grails.converters.JSON.registerObjectMarshaller(AprsData) {
		return it.properties.findAll {k,v -> k != 'class' && v != null}
		}
	}
	
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
	 * Quick and dirty solution for create user and device for APRS calls.
	 */
	private TrackerDevice loadDeviceForAprsPosition(PositionData positionData){
		TrackerDevice device = TrackerDevice.findByUdid(positionData.udid);
		if(!device){
			device = new TrackerDevice(udid:positionData.udid, username:positionData.username, status:TrackerDevice.DEVICE_TYPE_APRS);
			// load device icon from APRS symbol field
			device.icon = positionData.extendedInfo['aprs']?.symbol; // see AprsData::symbol
			if(!device.save(flush:true)){
				log.warn("Error register APRS device ${positionData.udid}, ${device.errors}");
				return null;
			}else{
				log.info("Registered new APRS device ${positionData.udid}");
			}
		}
		return device;
	}
	
	/**
	 * Update tracker position according to the received data object.
	 * @param udid
	 * @param positionData
	 */
	public void updateTrackerPosition(PositionData positionData){
		// Load device
		TrackerDevice device = null;
		String udid = positionData.udid;
		if(positionData.isAprs()){
			device = loadDeviceForAprsPosition(positionData);
		}else{
			device = TrackerDevice.findByUdid(udid);
		}
		
		if(!device){
			log.warn("Update position error, unknown device: " + udid);
			return;
		}

		// check user name
		String username = positionData.username;
		if(username){
			if(!username.equals(device.username)){
				// WARN - 根据当前逻辑，如果两个用户用同一个设备udid登录的话，会导致数据错乱！
				// arbitrary update device with current username
				//TrackerDevice.executeUpdate("UPDATE TrackerDevice as d SET d.username=:un)",[un:username]);
				String oldUsername = device.username
				device.username = username;
				if(device.save(flush:true)){
					log.warn("Re-associate device ${device.udid} from  ${oldUsername} to ${username}");
				}else{
					log.warn("Failed to Re-associate device username, ${device.errors}");
					return;
				}
			}
		}else{
			log.warn("PositionData contains NO username, running for test ? " + positionData.toString());
		}
		
		// Convert value object to position entity
		TrackerPosition position = new TrackerPosition();
		position.properties = positionData;
		position.device = device;
		if(!positionData.extendedInfo.isEmpty()){
			def extJson = positionData.extendedInfo as JSON // store extended info in JSON format.
			position.extendedInfo = extJson;
		}
		
		// Flush to database
		try {
			Long id = addPosition(position);
			if (id != null) {
				updateLatestPosition(device.id, id);
				// clear the position cache
				trackerCacheService.removeDeviceFeature(device.udid);
				
				if(log.isDebugEnabled()){
					log.debug("Device ${device.udid} position changed");
				}
				// broadcast the position data change
				notifyPositionChanges(positionData);
			}
		} catch (Exception error) {
			log.warn("update postion error, " + error.getMessage());
		}
	}
	
	/**
	 * 
	 */
	public int deleteAprsPosition(int daysOfDataToSave){
		//TrackerPosition.executeUpdate("DELETE FROM TrackerPosition tp WHERE tp.type==2 AND");
		//def list = TrackerPosition.executeQuery("FROM TrackerPosition tp JOIN TrackerDevice td ON tp.device_id = td.id");
		Date timeToDelete = new java.util.Date(System.currentTimeMillis() - (daysOfDataToSave * 24 * 3600 * 1000)); 
		//def count = TrackerPosition.executeQuery("SELECT count(*) FROM TrackerPosition tp WHERE tp.device.status=2 AND tp.time < :time",[time:timeToDelete]);
		def count = TrackerPosition.executeUpdate("DELETE TrackerPosition tp WHERE tp.id IN (SELECT p.id FROM TrackerPosition p WHERE p.device.status=2 AND p.time < :time)",[time:timeToDelete]);
		return count;
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
	 * Data change event listeners
	 */
	Set<PositionChangeListener> changeListeners = new HashSet<PositionChangeListener>();
	
	public void addChangeListener(PositionChangeListener listener){
		changeListeners.add(listener);
	}
	
	public void removeChangeListener(PositionChangeListener listener){
		changeListeners.remove(listener);
	}
	
	/*
	 * notify in another thread
	 */
	void notifyPositionChanges(final PositionData position){
		notifyThread.execute(new Runnable(){
			public void run(){
				for(PositionChangeListener l : changeListeners){
					l.onPositionChanged(position);
				}
			}
		});
	}

	ExecutorService notifyThread;
	@PostConstruct
	public void start(){
		notifyThread = java.util.concurrent.Executors.newFixedThreadPool(1);
		log.info "TrackerDataSrvice initialized"
	}
	
	@PreDestroy
	public void stop(){
		try{
			notifyThread?.shutdown();
		}catch(Exception e){
		}
		notifyThread = null;
		log.info "TrackerDataSrvice destroyed"
	}
}

public interface PositionChangeListener{
	public void onPositionChanged(PositionData position);
}
