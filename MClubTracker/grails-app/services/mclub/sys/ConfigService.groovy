package mclub.sys

import java.util.Map;
import java.util.concurrent.ExecutorService

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy

import org.codehaus.groovy.grails.commons.GrailsApplication;

/**
 * The system configuration service
 * @author shawn
 *
 */
public class ConfigService implements ConfigServiceKeys{
	GrailsApplication grailsApplication;
	TaskService taskService;
	
	private long configLastModifiedTime;
	private File configFile;
	private Properties config;
	private static final String DEFAULT_CONF_FILE = "/etc/mclub_server.conf";
	private static final int SECONDS = 1000
	@PostConstruct
	public void start(){
		String homeDir = System.properties.getProperty('mclub.home');
		if(homeDir != null){
			configFile = new File(homeDir,"mclub_server.conf");
		}else{
			configFile = new File(DEFAULT_CONF_FILE);
		}

		config = new Properties();
		
		// check every 30s
		taskService.execute(new Runnable(){
			public void run(){
				ConfigService.this.loadConfig();
			}
		}, 30 * SECONDS);
	}
	
	private void loadConfig(){
		if(!configFile.exists()){
			return;
		}
		if(configFile.lastModified() == configLastModifiedTime){
			return;
		}
		
		try {
			config.clear();
			config.load(new FileInputStream(configFile));
			configLastModifiedTime = configFile.lastModified();
			log.info("Loaded ${configFile}");
		} catch(IOException e) {
			log.warn("Error load ${configFile}, ${e.getMessage()}");
		}
		
	}
	
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
		String v = config.getProperty(key);
		if(!v){
			v = grailsApplication.getFlatConfig().get(key); 
		}
		return v;
	}
	
	public Boolean getConfigBool(String key){
		try{
			return Boolean.parseBoolean((String)getConfig(key));
		}catch(Exception e){
			return false;
		}
	}
	
	public String getConfigString(String key){
		return (String)getConfig(key);
	}
	
	public Integer getConfigInt(String key){
		def v = getConfig(key);
		if(v){
			try{
				return Integer.parseInt(v.toString());	
			}catch(Exception e){
				// noop
			}
		}
		return null;
	}
}

public interface ConfigServiceKeys{
	//TODO - move all configuration keys here
	public static final String MAP_FORCE_SECURE = "tracker.map.forceSecure";
	public static final String MAP_SHOW_LINE_DOTS = "tracker.map.showLineDots";
	public static final String MAP_AMAP_API_URL = "tracker.map.amapApiUrl";

	public static final String MAP_DEFAULT_MAP = "tracker.map.defaultMap";

	public static final String LIVE_DATA_SERVICE_URL = "tracker.liveDataServiceUrl"

	public static final String MAP_HISTORY_ENABLED = "tracker.map.history.enabled"
}
