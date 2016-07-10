package mclub.tracker

import grails.converters.JSON
import grails.transaction.Transactional;

import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

import mclub.util.DateUtils
import mclub.sys.ConfigService
import mclub.sys.MessageListener
import mclub.sys.MessageService
import mclub.tracker.aprs.AprsData;

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.transaction.TransactionDefinition;

class TrackerDataService {
	GrailsApplication grailsApplication;
	TrackerCacheService trackerCacheService;
	ConfigService configService;
	MessageService messageService;
	
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
	@Transactional
	public Long addPosition(TrackerPosition position) throws Exception{
        return saveOrUpdatePosition(position);
	}

    private Long saveOrUpdatePosition(TrackerPosition position) throws Exception{
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
	public void updateLatestPosition(Long deviceId, Long positionId, Date timestamp) throws Exception{
		// direct associate the position id to the device
		TrackerDevice.executeUpdate("UPDATE TrackerDevice AS d SET d.latestPositionId=:pid, d.latestPositionTime=:time WHERE d.id=:did",[did:deviceId,pid:positionId,time:timestamp]);
	}
	
	/**
	 * Quick and dirty solution for create user and device for APRS calls.
	 */
	private TrackerDevice loadDeviceForAprsPosition(PositionData positionData){
		TrackerDevice device = TrackerDevice.findByUdid(positionData.udid);
		String aprsSymbol = positionData.extendedInfo['aprs']?.symbol;
		if(!device){
			device = new TrackerDevice(udid:positionData.udid, username:positionData.username, status:TrackerDevice.DEVICE_TYPE_APRS);
			// load device icon from APRS symbol field
			device.icon = aprsSymbol; // see AprsData::symbol
			if(!device.save(flush:true)){
				log.warn("Error register APRS device ${positionData.udid}, ${device.errors}");
				return null;
			}else{
				log.info("Registered new APRS device ${positionData.udid}");
			}
		}else{
			// devices may change the symbol later, so check and update if necessary
			// note - that means no customized symbol supported yet for aprs devices.
			if(aprsSymbol && !(aprsSymbol.equals(device.icon))){
				device.icon = aprsSymbol;
				if(!device.save(flush:true)){
					log.warn("Error update APRS device ${positionData.udid} icon ${aprsSymbol}, ${device.errors}");
				}else{
					log.info("Updated APRS device ${positionData.udid} icon ${aprsSymbol}");
				}
			}
		}
		return device;
	}

	private boolean deviceIsBlackListed(String udid){
		if(udid == null) {
            return false;
        }
        String[] bl = configService.getConfigString('tracker.aprs.blacklist')?.trim()?.split(',');
        if(bl == null) {
            return false;
        }
        for(String b : bl){
            b = b.trim().toUpperCase();
            udid = udid.toUpperCase();
            if(b.endsWith('*')){
                b = b.substring(0,b.length()-1);
                if(udid.startsWith(b)){
                    return true;
                }
            }else if(b.equals(udid)){
                return true;
            }
        }
		return false;
	}

    /**
     * Most of the fixed-station message is same
     *
     * this should be optimized in the APRS parser by caching the last received message and compare while receiving and
     * pass with a 'changed' field in the message
     *
     * @param position
     * @param lastPosition
     * @return
     */
	private boolean isAprsDevicePositionChanged(TrackerPosition position, TrackerPosition lastPosition){
		boolean notChanged = false;
		boolean changed = true;
        if(lastPosition == null){
            return changed;
        }

        // quick and dirty check logic
        // mostly position W/O speed is a fixed station broadcast message, so...
        if(position.speed != null && !position.speed.equals(lastPosition.speed) ){
            return changed;
        }

		if(position.latitude != null && !position.latitude.equals(lastPosition.latitude) ){
            return changed;
        }
        if(position.longitude != null && !position.longitude.equals(lastPosition.longitude) ){
            return changed;
        }
        if(position.course != null && !position.course.equals(lastPosition.course) ){
            return changed;
        }
        if(position.altitude != null && !position.altitude.equals(lastPosition.altitude) ){
            return changed;
        }
        if(position.extendedInfo != null && !position.extendedInfo.equals(lastPosition.extendedInfo) ){
            return changed;
        }

        /*
        if(position.power != null && !position.power.equals(lastPosition.power) ){
            return changed;
        }
        if(position.address != null && !position.address.equals(lastPosition.address) ){
            return changed;
        }
        if(position.message != null && !position.message.equals(lastPosition.message) ){
            return changed;
        }
        */
        return notChanged;
	}

	/**
	 * Update tracker position according to the received data object.
	 * @param udid
	 * @param positionData
	 */
	@Transactional
	public void updateTrackerPosition(PositionData positionData){
		// Load device
		TrackerDevice device = null;
		String udid = positionData.udid;
		if(positionData.isAprs()){
			// for aprs devices, we have a black list
			if(deviceIsBlackListed(positionData.udid)) {
				log.info("Device ${positionData.udid} is blacklisted, position update will be ignored")
				return;
			}
			device = loadDeviceForAprsPosition(positionData);
		}else{
			device = TrackerDevice.findByUdid(udid);
		}
		
		if(!device){
			log.warn("Update position error, unknown device: " + udid);
			return;
		}
		
		// Update frequency check 
		Integer timeInterval = configService.getConfigInt("tracker.minimalPositionUpdateInterval");
		if(!timeInterval) timeInterval = 5000L;
		if(positionData.messageType!= null && positionData.messageType > TrackerPosition.MESSAGE_TYPE_NORMAL){
			timeInterval = 500; // for emergency messages, threshold set to 0.5s
		}
		//if(device.status == TrackerDevice.DEVICE_TYPE_ACTIVED){
		if(true){
			timeInterval = 1000;// for registered devices, the threshold set to 1s
		}
		
		if(device.latestPositionTime && (System.currentTimeMillis() - device.latestPositionTime.time < timeInterval)){
			// update too frequently.
			log.info("Device ${device.udid} update location too frequently, last update time: ${device.latestPositionTime}, ignored");
			return;
		}

		// check user name
		String username = positionData.username;
		if(username && !username.equals(device.username)){
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
		
		// Convert value object to position entity
		TrackerPosition newPos = new TrackerPosition();
        newPos.properties = positionData; // bulk assign
        newPos.message = positionData.message;
        newPos.messageType = positionData.messageType;
        newPos.device = device;
		if(!positionData.extendedInfo.isEmpty()){
			def extJson = positionData.extendedInfo as JSON // store extended info in JSON format.
            newPos.extendedInfo = extJson;
		}

		// For APRS station positions without SPEED, will check and update the previous record instead of insert a new one.
        boolean positionChanged = true;
		if(positionData.isAprs()){
			// check the previous position
            try{
                TrackerPosition lastPos = TrackerPosition.load(device.latestPositionId);
                if(!isAprsDevicePositionChanged(newPos,lastPos)){
                    // position is NOT CHANGED, just update the timestamp...
                    lastPos.time = positionData.time;
                    newPos = lastPos; // replace for later update/save
                    positionChanged = false;
                    log.debug("Device ${device.udid} pos is not changed,  ${newPos.latitude}/${newPos.longitude}/${newPos.extendedInfo}");
                }
            }catch(Exception e ){
                log.warn("check position change error, " + e.getMessage());
            }
		}

		// Save the position data
		try {
			Long id = saveOrUpdatePosition(newPos);
			if (id != null) {
				updateLatestPosition(device.id, id,newPos.time);

                // if position changed, also need to invalidate the cache and broadcast the changes
                if(true/*positionChanged*/){
                    // clear the position cache
                    trackerCacheService.removeDeviceFeature(device.udid);

                    if(log.isDebugEnabled()){
                        log.debug("Device ${device.udid} position updated to [${newPos.latitude},${newPos.longitude}], message:${newPos.message}");
                    }
                    // broadcast the position data change
                    // HACK - we should set the device type currently for websocket filtering work.
                    positionData.deviceType = device.status;
                    notifyPositionChanges(positionData);
                }
			}
		} catch (Exception error) {
			log.warn("update position error, " + error.getMessage());
		}
	}
	
	/**
	 * 
	 */
	public int deleteAprsPosition(int daysOfDataToSave){
		log.info("Delete APRS Positions ${daysOfDataToSave} days before");
		int count = 0;
		def aprsDevices = TrackerDevice.findAllByStatus(TrackerDevice.DEVICE_TYPE_APRS);
		log.info("Total ${aprsDevices.size()} APRS devices");
		Date timeToDelete = new java.util.Date(System.currentTimeMillis() - (daysOfDataToSave * 24 * 3600 * 1000));
		log.info("Will delete historical position data before ${timeToDelete}");
		for(TrackerDevice dev in aprsDevices){
			int c = TrackerPosition.executeUpdate('DELETE FROM TrackerPosition tp WHERE tp.device=:device AND tp.time < :time',[device:dev,time:timeToDelete]);
			if(c > 0){
				count +=c;
				if(count > 0 && count % 100 == 0){
					log.info("  ${dev.udid}: ${count} positions deleted.");
				}
			}
		}
		log.info("Total ${count} position records deleted");
		return count;
		
		//TrackerPosition.executeUpdate("DELETE FROM TrackerPosition tp WHERE tp.type==2 AND");
		//def list = TrackerPosition.executeQuery("FROM TrackerPosition tp JOIN TrackerDevice td ON tp.device_id = td.id");
		//Date timeToDelete = new java.util.Date(System.currentTimeMillis() - (daysOfDataToSave * 24 * 3600 * 1000)); 
		//def count = TrackerPosition.executeQuery("SELECT count(*) FROM TrackerPosition tp WHERE tp.device.status=2 AND tp.time < :time",[time:timeToDelete]);
		//def count = TrackerPosition.executeUpdate("DELETE TrackerPosition tp WHERE tp.id IN (SELECT p.id FROM TrackerPosition p WHERE p.device.status=2 AND p.time < :time)",[time:timeToDelete]);
		//return count;
	}

	/*
	 * call the message bus to notify the position changes.
	 */
	private void notifyPositionChanges(PositionData position){
		messageService.postMessage(position);
	}

	@PostConstruct
	public void start(){
		log.info "TrackerDataService initialized"
	}
	
	@PreDestroy
	public void stop(){
		log.info "TrackerDataSrvice destroyed"
	}
}
