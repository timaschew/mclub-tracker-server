package mclub.tracker.aprs;

import groovy.transform.ToString;

/**
 * 
 * @author shawn
 *
 */
@ToString(includeNames = true, includeFields = true, excludes = "metaClass,class")
public class AprsData {
	private String path;
	private String comment;
	
	private Integer power;
	private Integer height;
	private Integer gain;
	private Integer directivity;
	
	private String symbol; // index of APRS symbol, including both chart1 and chart2
	
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}

	public Integer getPower() {
		return power;
	}
	public void setPower(Integer power) {
		this.power = power;
	}
	public Integer getHeight() {
		return height;
	}
	public void setHeight(Integer height) {
		this.height = height;
	}
	public Integer getGain() {
		return gain;
	}
	public void setGain(Integer gain) {
		this.gain = gain;
	}
	public Integer getDirectivity() {
		return directivity;
	}
	public void setDirectivity(Integer directivity) {
		this.directivity = directivity;
	}
	
	/**
	 * The file name of APRS site symbol.
	 * See AprsReceiver::convertSymbolCharToFileName(table,index);
	 * @return
	 */
	public String getSymbol() {
		return symbol;
	}
	
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
}
