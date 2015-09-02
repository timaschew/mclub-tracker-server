package mclub.tracker

/**
 * The tracker device
 * @author shawn
 *
 */
class TrackerDevice {
    static constraints = {
//		phoneNumber blank:true,nullable:true
//		imei blank:true,nullable:true
		latestPositionId blank:true,nullable:true
		username blank:true,nullable:true
    }
	
	static mapping = {
		udid index:'idx_trackerdevice_udid'
		udid index:'idx_trackerdevice_username'
	}
	
	String udid; // the unique device id, could be IMEI or callsign or manual assigned devices;
	
//	String imei;
//	String phoneNumber; // tracker device may not have a phone number
	Long latestPositionId; // for association with the position
	
	String username; // Associated user name
}
