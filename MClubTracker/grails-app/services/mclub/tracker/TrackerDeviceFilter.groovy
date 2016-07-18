package mclub.tracker

import grails.validation.Validateable
import org.slf4j.Logger
import org.slf4j.LoggerFactory


@Validateable(nullable=true)
public class TrackerDeviceFilter{
	private static Logger log = LoggerFactory.getLogger(getClass());
	static constraints = {
		udid blank:false, nullable:false
	}
	
	String udid;
	Integer type;
	String bounds;
	//Double lat1,lon1,lat2,lon2;
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

	/**
	 * Parse the bound coordinate parameter (lat1,lon1,lat2,lon2) in a double array
	 * @return
	 */
	public Double[] getBoundsCoordinate(){
		if(bounds){
			String[] s = bounds.split(",")
			if(s.length == 4){
				try{
					Double[] d = new Double[4];
					d[0] = Double.parseDouble(s[0]);
					d[1] = Double.parseDouble(s[1]);
					d[2] = Double.parseDouble(s[2]);
					d[3] = Double.parseDouble(s[3]);

					// validation check
					if(d[0] < d[2]){
						log.warn "topLeftLat must be >= bottomRighLat"
						return null;
					}
					if(d[1] > d[3]){
						log.warn "topLeftLon must be <= bottomRighLon"
						return null;
					}
					return d;
				}catch(Exception e){
					log.info("Invalid bound coordinate, ${e.message}")
				}
			}
		}
		return null;
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