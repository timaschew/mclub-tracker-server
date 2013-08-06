package mclub.datamining

import java.util.Date;

class RuleState {

    static constraints = {
		
    }
	static mapping = {
		deviceId index:'idx_rulestate_deviceid'
	}
	
	/**
	 * The rule name
	 */
	String ruleName;
	
	/**
	 * Last update time stamp
	 */
	Date lastUpdated;
	
	/**
	 * Stored rule values, could be anything in text format
	 */
	String value;
	
	/**
	 * Associated device id
	 */
	String deviceId;
}
