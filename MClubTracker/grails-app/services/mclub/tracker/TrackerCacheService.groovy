package mclub.tracker

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;


public class TrackerCacheService {
	private ExecutorService cleanThread;
	private ConcurrentHashMap<String, Object> featureCache = new ConcurrentHashMap<String,Object>();
	
	@PostConstruct
	public void start(){
		log.info "TrackerCacheService initialized"
	}
	
	@PreDestroy
	public void stop(){
		log.info "TrackerCacheService stopped"
	}
	public Object cacheDeviceFeature(String udid, Object feature){
		Object exists = featureCache.putIfAbsent(udid, feature);
		if(exists){
			return exists;
		}else{
			log.debug("CACHE device[" + udid + "] features");
			return feature;
		} 
	}
	
	public Object getDeviceFeature(String udid){
		return featureCache.get(udid);
	}
	
	public Object removeDeviceFeature(String udid){
		return featureCache.remove(udid);
	}
}
