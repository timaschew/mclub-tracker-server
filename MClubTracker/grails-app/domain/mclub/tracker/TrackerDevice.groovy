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
	
	static mapping = {
		udid index:'idx_trackerdevice_udid'
	}
	
	String udid; // the unique device id, could be IMEI or callsign or manual assigned devices;
	
	String imei;
	String phoneNumber;
	Long latestPositionId; // for association with the position
}
