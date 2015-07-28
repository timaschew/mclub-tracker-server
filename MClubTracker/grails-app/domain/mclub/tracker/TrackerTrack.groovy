package mclub.tracker

class TrackerTrack {

	public static final int TRACK_TYPE_NORMAL = 0;
	public static final int TRACK_TYPE_ACTIVE = 1;
	
    static constraints = {
		description blank:true, nullable:true
		type blank:true, nullable:true
    }
	
	static mapping = {
		deviceId index:'idx_trackertrack_deviceid'
		beginDate index:'idx_trackertrack_begindate_enddate'
		endDate index:'idx_trackertrack_begindate_enddate'
		description type:'text'
	}
	
	String title;
	Date beginDate;
	Date endDate;
	Long deviceId; // PK of device record
	String description;
	Integer type = TRACK_TYPE_NORMAL; // normal track is 0 and active track is 1
}
