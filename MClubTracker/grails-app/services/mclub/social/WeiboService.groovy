package mclub.social

import mclub.sys.ConfigService
import mclub.tracker.TrackerDataService
import mclub.tracker.TrackerDevice
import weibo4j.Oauth
import weibo4j.Timeline
import weibo4j.http.AccessToken
import weibo4j.model.Status

class WeiboService {
	
	ConfigService configService;
	public boolean isEnabled(){
		return Boolean.TRUE.equals(configService.getConfig("social.weibo.enabled"));
	}

	/**
	 * Post status
	 * @param status
	 * @return
	 */
    public Status postStatus(String deviceId, String text) {
		log.info("Sending Weibo Status: ${text}");
		Status status = null;
		SocialAccessToken sat = SocialAccessToken.findByDeviceId(deviceId);
		if(sat && sat.accessToken){
			Timeline timeline = new Timeline();
			timeline.setToken(sat.accessToken);
			status = timeline.UpdateStatus(text);
		}else{
			log.info("No access token found for device ${deviceId}, bind first");
		}
		return status;
    }
	
	/**
	 * Post status with locations
	 */
	public Status postStatus(String deviceId, String text, Double lat, Double lon){
		log.info("Sending Weibo Status: ${text} with position ${lat},${lon}");
		Status status = null;
		SocialAccessToken sat = SocialAccessToken.findByDeviceId(deviceId);
		if(sat && sat?.accessToken){
			Timeline timeline = new Timeline();
			timeline.setToken(sat.accessToken);
			try{
				status = timeline.UpdateStatus(text,lat.floatValue(),lon.floatValue(),"");
			}catch(Exception e){
				log.error("Error update weibo status!",e);
			}
		}else{
			log.error("Abort update weibo status! No access token found for device ${deviceId}, bind first");
		}
		return status;
	}
	
	
	public void refreshTokens(){
		// Enumerate the devices
		List<TrackerDevice> devices = TrackerDevice.findAllByStatus(TrackerDevice.DEVICE_TYPE_ACTIVED);
		for(TrackerDevice dev in devices){
			String deviceId = dev.udid;
			try{
				SocialAccessToken sat = SocialAccessToken.findByDeviceId(deviceId);
				if(sat && sat.socialUsername && sat.socialPassword){
					// post user/pass to weibo server to renew the access token
					Oauth oauth = new Oauth();
					AccessToken t = oauth.refreshToken(sat.socialUsername, sat.socialPassword);
					if(t){
						// success!
						sat.accessToken = t.accessToken;
						sat.refreshToken = t.refreshToken;
						// weibo api returns the expireIn value in seconds, convert to the date object
						sat.expireTime = new java.util.Date(System.currentTimeMillis() + (Long.parseLong(t.expireIn) * 1000));
						sat.save(flush:true);
						log.info("Token refreshed: ${sat.accessToken}, expires in ${sat.expireTime}");
					}else{
						log.warn("Failed to refresh token: ${sat.accessToken}, expires in ${sat.expireTime}");
					}
				}else{
					log.info("No token to refresh for device ${deviceId}");
				}
			}catch(Exception e){
				log.error("refresh token error: ${e.message}");
			}		
		}// end of for loop
	}
}
