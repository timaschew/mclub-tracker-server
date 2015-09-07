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
	
	
	private TrackerDevice loadDeviceForAprsPosition(PositionData positionData){
		// FIXME - Quick and dirty solution for create user and device for aprs calls.
		TrackerDevice device = TrackerDevice.findByUdid(positionData.udid);
		if(!device){
			device = new TrackerDevice(udid:positionData.udid, username:positionData.username, status:2);
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
		TrackerDevice device = null;
		String udid = positionData.udid;
		if(positionData.getAprs()){
			device = loadDeviceForAprsPosition(positionData);
		}else{
			device = TrackerDevice.findByUdid(udid);
		}
		
		if(!device){
			log.warn("Unknown device - " + udid);
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

		/*
		Long devicePK = lookupDeviceId(positionData.udid);
		if(devicePK == null){
			log.warn("Unknown device - " + udid);
			return;
		}
		*/
		
		Long devicePK = device.id;
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
				
				// clear the position cache
				trackerCacheService.removeDeviceFeature(device.udid);
				
				// broadcast the position data change
				notifyPositionChanges(positionData);
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
