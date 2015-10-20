package mclub.tracker

import grails.validation.Validateable


@Validateable(nullable=true)
public class TrackerDeviceFilter{
	static constraints = {
		udid blank:false, nullable:false
	}
	
	String udid;
	Integer type;
	Double lat1,lon1,lat2,lon2;
	Date activeTime;
	
	public boolean accept(PositionData positionData){
		
		// UDID available
		if(udid){
			if(udid.equalsIgnoreCase("ALL") ){
				return true;
			//} else if(udid.equalsIgnoreCase(positionData.udid)){
			//	return true;
			} else if(positionData.udid.startsWith(udid)){
				return true;
			}
		}
		
		// just only have the type
		if(udid == null && type > 0){
			return type == positionData.deviceType;
		}
		return false;
	}
}

public class CompisiteTrackerDeviceFilter extends TrackerDeviceFilter{
	List<TrackerDeviceFilter> filters;
	
	public boolean accept(PositionData positionData){
		if(filters?.size() > 0){
			for(TrackerDeviceFilter f : filters){
				if(f.accept(positionData))return true;
			}
		}
		return false;
	}
}