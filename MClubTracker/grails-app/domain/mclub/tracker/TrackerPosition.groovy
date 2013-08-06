package mclub.tracker

class TrackerPosition {

    static constraints = {
		power blank:true, nullable:true
		address blank:true,nullable:true
		extendedInfo blank:true, nullable:true
    }
	
	static mapping = {
		deviceId	index:'idx_trackerposition_deviceid_time'
		time		index:'idx_trackerposition_deviceid_time'
		extendedInfo	type:'text' 
		version false
	}
	
	Long deviceId; // the associated device row id, not the unique device id(IMEI, eg)
	Date time;
	Boolean valid;
	Double latitude;
	Double longitude;
	Double altitude;
	Double speed;
	Double course;
	
	Double power;
	String address;
	String extendedInfo;
}
