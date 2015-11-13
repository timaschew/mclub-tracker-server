package mclub.tracker

/**
 * The tracker device
 * @author shawn
 *
 */
class TrackerDevice {
	public static final int DEVICE_TYPE_DEACTIVED = 0;
	public static final int DEVICE_TYPE_ACTIVED = 1;
	public static final int DEVICE_TYPE_APRS = 2;
	
    static constraints = {
		latestPositionId blank:true,nullable:true
		latestPositionTime blank:true,nullable:true
		
		username blank:true,nullable:true
		status blank:true,nullable:true
		icon blank:true,nullable:true
		comments blank:true,nullable:true
    }
	
	static mapping = {
		udid index:'idx_trackerdevice_udid'
		username index:'idx_trackerdevice_username'
		status index:'idx_trackerdevice_status'
		latestPositionTime index:'idx_trackerdevice_lastpositiontime'
	}
	
	String udid; // the unique device id, could be IMEI or callsign or manual assigned devices;
	String username; 		// Associated user name
	Integer status;			// status of tracker device, 0 means disabled.
	
	Long latestPositionId; 	// for association with the position
	Date latestPositionTime
	String icon;			// name of the device icon, eg: APRS site symbol
	String comments;		// the device comments, optional
}
