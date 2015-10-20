package mclub.tracker

import grails.converters.JSON
import mclub.user.User

class TrackerMap {

	public static final Integer TRACKER_MAP_TYPE_PUBLIC = 0;
	public static final Integer TRACKER_MAP_TYPE_PROTECTED = 1;
	public static final Integer TRACKER_MAP_TYPE_PRIVATE = 2;
	
	static constraints = {
		uniqueId 	unique: true
		name 		blank:true, nullable:true
		filterJSON 	blank:true, nullable:true
		username		blank:true, nullable:true
	}

	static mapping = { 
		uniqueId		index:'idx_trackermap_uniqueid'
		username		index:'idx_trackermap_username' 
	}

	String uniqueId; // auto generated random id
	String name;	 // user specified display name
	String filterJSON;   // the JSON string of device filter
	
	String username;       // associated user's name
	Integer type;
	
	/**
	 * Load filter accord to the filter json field
	 * @return
	 */
	public List<TrackerDeviceFilter> loadFilters(){
		def filters = [];
		if(filterJSON){
			def json = JSON.parse(filterJSON); // currently, it's an array of device IDs
			if(json instanceof List){
				for(String s : json){
					filters.add(new TrackerDeviceFilter(udid:s));
				}
			}
		}
		return filters;
	}
}
