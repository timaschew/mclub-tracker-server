package mclub.datamining

import java.util.Date;

class RuleState {

    static constraints = {
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
	String values;
}
