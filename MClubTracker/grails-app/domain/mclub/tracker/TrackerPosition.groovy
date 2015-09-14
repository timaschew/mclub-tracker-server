package mclub.tracker

class TrackerPosition {
	public static final int COORDINATE_TYPE_WGS84 = 0;
	public static final int COORDINATE_TYPE_GCJ02 = 1;
	public static final int COORDINATE_TYPE_BD09 = 2;
	
	public static final int MESSAGE_TYPE_NORMAL = 0;
	public static final int MESSAGE_TYPE_ALERT = 1;
	public static final int MESSAGE_TYPE_EMERGENCY = 2;
	
	static belongsTo = TrackerDevice;
	
    static constraints = {
		power blank:true, nullable:true
		address blank:true,nullable:true
		extendedInfo blank:true, nullable:true
		coordinateType blank:true,nullable:true
		
		message blank:true, nullable:true
		messageType blank:true, nullable:true
    }
	
	static mapping = {
		device		index:'idx_trackerposition_device'
		time		index:'idx_trackerposition_time'
		extendedInfo	type:'text' 
		version false
	}
	
	//Long deviceId; // the associated device row id, not the unique device id(IMEI, eg)
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
	Integer coordinateType; // The coordinate system, 0 = WGS84, 1 = GCJ02, 2 = BD09
	
	String message;
	Integer messageType;
	
	TrackerDevice device;
}
