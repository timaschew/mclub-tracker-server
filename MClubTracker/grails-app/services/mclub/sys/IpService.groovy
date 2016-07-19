package mclub.sys

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;

import mclub.util.IP;
import mclub.util.loc.LocationObject;
import mclub.util.loc.LocationCoderService;
import grails.converters.JSON;

public class IpService {
	ConfigService configService;
	
	LocationCoderService locationCoderService;
	
	@PostConstruct
	public void start(){
		String ipdbFilePath = configService.getConfigString("sys.ipdb.filepath");
		if(ipdbFilePath){
			File f = null;
			
			if(ipdbFilePath.charAt(0) == '~'){
				f = new File(System.getProperty("user.home"),ipdbFilePath.substring(1));
			}else{
				f = new File(ipdbFilePath);
			}
			if(f.exists()){
				try{
					IP.load(f.getAbsolutePath());
					log.info("Loading ipdb file ${ipdbFilePath}");
				}catch(Exception e){
					log.warn("Error loading ipdb file ${ipdbFilePath}",e);
				}
			}else{
				log.warn("IP db file not found at ${ipdbFilePath}");
			}
		}
		
		// initialize the location coder service
		locationCoderService = new LocationCoderService("/tmp/location_coder_cache.json");
		locationCoderService.start();
	}
	
//	public String lookupIpLocation(String ip){
//		String loc = null;
//		try{
//			String addr = lookupIpAddress(ip);
//			if(addr){
//				loc = addressToLocation(addr);
//			}
//		}catch(Exception e){
//			log.info("lookup ip adderss failed: " + e.getMessage())
//		}
//		if(!loc){
//			loc = "[120.219375,30.259244]";
//		}
//		return loc;
//	}
	
	public String lookupIpAddress(String ip){
		if(!ip) return null;
		def addr = IP.find(ip);
		if(addr){
			return addr.join(',');
		}
		return null;
	}
	
	public List<Float> addressToLocation(String address){
		if(!address) return null;
		LocationObject loc;
		loc = locationCoderService.getLocation(address, true);
		if(loc){
			return [loc.lon,loc.lat];
		}
	}
	
	@PreDestroy
	public void stop(){
		this.locationCoderService.stop();
	}
	
//	private LocationObject queryGeoLocation(String cityNames){
//		// http://api.map.baidu.com/geocoder?address=%E6%B5%99%E6%B1%9F%E6%9D%AD%E5%B7%9E&output=json&key=g2fQ61PYZCgBwTY7YAk9c8n7&city=%E6%B5%99%E6%B1%9F%E6%9D%AD%E5%B7%9E
//		LocationObject location = null;
//		String mapApiURL = "http://api.map.baidu.com/geocoder";
//		try {
//			HttpClient client = new HttpClient();
//			GetMethod httpget = new GetMethod(mapApiURL /*"http://api.map.baidu.com/geocoder"*/);
//			httpget.setRequestHeader("Accept-Charset", "utf-8,gb2312");
//			
//			List<NameValuePair> params = new ArrayList<NameValuePair>();
//			params.add(new NameValuePair("address",cityNames));
//			params.add(new NameValuePair("city",cityNames));
//			params.add(new NameValuePair("output","json"));
//			params.add(new NameValuePair("key","g2fQ61PYZCgBwTY7YAk9c8n7"));
//			
//			httpget.setQueryString(params.toArray(new NameValuePair[0]));
//
//			int status = client.executeMethod(httpget);
//			//System.out.println("Response status: " + status);
//			log.debug("Response status: " + status);
//
//			String jsonString = httpget.getResponseBodyAsString();
//			log.debug("JSON:");
//			log.debug(jsonString);
//			//System.out.println("JSON:");
//			//System.out.println(jsonString);
//			
//			def obj = JSON.parse(jsonString);
//			if(obj != null){
//				if("OK".equalsIgnoreCase(obj["status"])){
//					def loc = obj["result"]["location"];
//					if(loc != null){
//						// construct the GeoLocation object
//						location = new LocationObject();
//						location.addr = cityNames;
//						location.lat = String.format("%.6f", loc["lat"]);//new String(loc("lat");
//						location.lon = String.format("%.6f", loc["lng"]);//loc.getString("lon");
//					}
//				}
//				
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return location;
//	}
}
