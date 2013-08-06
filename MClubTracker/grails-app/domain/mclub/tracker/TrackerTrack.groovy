package mclub.tracker

class TrackerTrack {

    static constraints = {
		description blank:true, nullable:true
    }
	
	static mapping = {
		deviceId index:'idx_device_id'
		beginDate index:'idx_date_begin_end'
		endDate index:'idx_date_begin_end'
		description type:'text'
	}
	
	String title;
	Date beginDate;
	Date endDate;
	Long deviceId; // PK of device record
	String description;
}
