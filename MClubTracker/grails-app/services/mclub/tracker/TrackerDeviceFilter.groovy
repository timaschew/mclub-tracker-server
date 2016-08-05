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
	String historyTime; // currently only supports 'yy-MM-dd'

	private boolean boundsContainingCoordinate(double lat, double lon){
		Double[] b = getBoundsCoordinate();
		if(b){
			double lat1 = b[0];
			double lon1 = b[1];
			double lat2 = b[2];
			double lon2 = b[3];
			return (lat <= lat1) && (lon >= lon1) && (lat >= lat2) && (lon <= lon2);
		}
		return false;
	}

	public boolean accept(PositionData positionData){
		// First check the type
		if(type > 0 && type != positionData.deviceType){
			return false;
		}

		// check udid;
		if(udid && udid.length() > 0 && !"ALL".equalsIgnoreCase(udid)){
			// UDID available
			if(!positionData.udid.startsWith(udid)){
				return false;
			}
		}

		// check range
		if(bounds && bounds.length() > 0){
			if(!boundsContainingCoordinate(positionData.latitude,positionData.longitude)){
				return false;
			}
		}

		// the left is passed!
		return true;

		/*
		if(bounds) {
			return boundsContainingCoordinate(positionData.latitude,positionData.longitude);
		}else if(udid){
			// UDID available
			if(udid.equalsIgnoreCase("ALL") ){
				return true;
			} else if(positionData.udid.startsWith(udid)){
				return true;
			}
		}
		// just only have the type - should be DEPRECATED!
		if(udid == null && type > 0){
			return type == positionData.deviceType;
		}
		return false;
		*/
	}

	/**
	 * Parse the bound coordinate parameter (lat1,lon1,lat2,lon2) in a double array
	 * @return
	 */
	public Double[] getBoundsCoordinate(){
		if(bounds && bounds.length() > 0){
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