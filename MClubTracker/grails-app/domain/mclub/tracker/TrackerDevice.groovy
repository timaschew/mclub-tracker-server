package mclub.tracker

/**
 * The tracker device
 * @author shawn
 *
 */
class TrackerDevice {
	public static final int DEVICE_TYPE_DEACTIVE = 0;
	public static final int DEVICE_TYPE_ACTIVE = 1;
	
    static constraints = {
		latestPositionId blank:true,nullable:true
		username blank:true,nullable:true
		status blank:true,nullable:true
    }
	
	static mapping = {
		udid index:'idx_trackerdevice_udid'
		username index:'idx_trackerdevice_username'
		status index:'idx_trackerdevice_status'
	}
	
	String udid; // the unique device id, could be IMEI or callsign or manual assigned devices;
	
	Long latestPositionId; // for association with the position
	Integer status;			// status of tracker device, 0 means disabled.
	String username; // Associated user name
}
