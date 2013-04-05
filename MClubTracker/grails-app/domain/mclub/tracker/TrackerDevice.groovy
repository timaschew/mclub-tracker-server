package mclub.tracker

/**
 * The tracker device
 * @author shawn
 *
 */
class TrackerDevice {
    static constraints = {
		phoneNumber blank:true,nullable:true
		imei blank:true,nullable:true
		latestPositionId blank:true,nullable:true
    }
	
	static mappings = {
		deviceId index:'idx_device_id'
	}
	
	String deviceId; // the unique device id, mostly, will be the imei;
	
	String imei;
	String phoneNumber;
	Long latestPositionId; // for association with the position
}
