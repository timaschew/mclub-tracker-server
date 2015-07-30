package mclub.tracker;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Data object of position reported by a tracker
 * 
 * @author shawn
 *
 */
public class PositionData {
	String udid;
	String imei;
	Date time;
	Boolean valid;
	Double latitude;
	Double longitude;
	Double altitude;
	Double speed;
	Double course;

	Double power;
	String address;
	Map<String, Object> extendedInfo = new HashMap<String,Object>();

	public String getUdid() {
		return udid;
	}

	public void setUdid(String udid) {
		this.udid = udid;
	}

	public String getImei() {
		return imei;
	}

	public void setImei(String imei) {
		this.imei = imei;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public Boolean getValid() {
		return valid;
	}

	public void setValid(Boolean valid) {
		this.valid = valid;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public Double getAltitude() {
		return altitude;
	}

	public void setAltitude(Double altitude) {
		this.altitude = altitude;
	}

	public Double getSpeed() {
		return speed;
	}

	public void setSpeed(Double speed) {
		this.speed = speed;
	}

	public Double getCourse() {
		return course;
	}

	public void setCourse(Double course) {
		this.course = course;
	}

	public Double getPower() {
		return power;
	}

	public void setPower(Double power) {
		this.power = power;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public Map<String, Object> getExtendedInfo() {
		return extendedInfo;
	}

	public void setExtendedInfo(Map<String, Object> extendedInfo) {
		this.extendedInfo = extendedInfo;
	}

}
