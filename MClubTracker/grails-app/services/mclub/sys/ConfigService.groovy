package mclub.sys

import java.util.Map;

import org.codehaus.groovy.grails.commons.GrailsApplication;

/**
 * The system configuration service
 * @author shawn
 *
 */
public class ConfigService {
	GrailsApplication grailsApplication;
	
	/**
	 * Bridge methods for Java POJO to access the grails configuration
	 * @return
	 */
	public Map<String,Object> getConfig(){
		return grailsApplication.getFlatConfig();
	}
	
	/**
	 * Get config by key
	 * @param key
	 * @return
	 */
	public Object getConfig(String key){
		return grailsApplication.getFlatConfig().get(key);
	}
	
	public String getConfigString(String key){
		return (String)getConfig(key);
	}
	
	public int getConfigInt(String key){
		return (Integer)getConfig(key);
	}
}
