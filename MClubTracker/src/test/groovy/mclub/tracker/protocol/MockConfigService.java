package mclub.tracker.protocol;

import java.util.HashMap;
import java.util.Map;

import mclub.sys.ConfigService;

public class MockConfigService extends ConfigService{
	private Map<String,Object> config = new HashMap<String,Object>();

	/**
	 * Bridge methods for Java POJO to access the grails configuration
	 * @return
	 */
	public Map<String,Object> getConfig(){
		return config;
	}
	
	/**
	 * Get config by key
	 * @param key
	 * @return
	 */
	public Object getConfig(String key){
		if(key.endsWith(".address")){
			return "127.0.0.1";
		}else if(key.endsWith(".port")){
			return 5000;
		}
		return null;
	}

}
