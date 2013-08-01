package mclub.social

import weibo4j.Oauth
import weibo4j.Timeline
import weibo4j.http.AccessToken
import weibo4j.model.Status
import weibo4j.model.WeiboException
import weibo4j.util.BareBonesBrowserLaunch

class WeiboController {

	def index() {
		render text:"bind,auth,post,list,read"
	}
	
	/**
	 * STEP 1 - Bind device with weibo account 
	 * @param deviceId
	 * @param username
	 * @param password
	 * @return
	 */
	def bind(String deviceId, String username, String password){
		if(!deviceId){
			deviceId = session.getAttribute("deviceId");
		}
		SocialNetworkAccessToken snat = null;
		
		// GET Info page
		if(request.method.equalsIgnoreCase("GET")){
			//TODO - read device id and show from db just display the form
			if(deviceId){
				snat = SocialNetworkAccessToken.findByDeviceId(deviceId);
			}
			render view:"bind_view.gsp", model:[record:snat] ;
			return;
		}
		
		// POST logic
		if(!deviceId || !username || !password){
			flash.message = 'missing parameter!';
			render view:"bind_view.gsp"
			return;
		}
		
		// save to database
		snat = SocialNetworkAccessToken.findByDeviceId(deviceId);
		if(!snat){
			snat = new SocialNetworkAccessToken();
			snat.deviceId = deviceId;
		}
		snat.socialUsername = username;
		snat.socialPassword = password;
		snat.save(flush:true);
		
		// redirect to the authorize page
		session.setAttribute("deviceId", deviceId);
		Oauth oauth = new Oauth();
		String authURL =  oauth.authorize("code","forester");
		redirect url:authURL;
	}
	
	/**
	 * STEP 2 - Jump back from OAuth server with code parameter available
	 * @param deviceId
	 * @param code
	 * @return
	 */
	def auth(String deviceId, String code){
		if(!code){
			// we should have the code
			render text:"Missing \"code\", aborted", status:401
			return;
		}

		if(!deviceId){
			// we should have device id stored in the session
			// in bind() step
			deviceId = session.getAttribute("deviceId");
			if(!deviceId){
				render text:"Missing \"deviceId\", aborted", status:401
				return;
			}
		}
		
		// Retrieve access token by the code
		Oauth oauth = new Oauth();
		SocialNetworkAccessToken snat = SocialNetworkAccessToken.findByDeviceId(deviceId);
		if(!snat){
			// we must have the snat record ready. othersize redirect to the re-bind page
			redirect action:'bind'
			return;
		}
		
		AccessToken weiboToken = null;
		try{
			weiboToken = oauth.getAccessTokenByCode(code);
			
			snat.accessToken = weiboToken.accessToken;
			snat.refreshToken = weiboToken.refreshToken;
			snat.socialUserId = weiboToken.uid;
			snat.expireTime = new java.util.Date(System.currentTimeMillis() + Long.parseLong(weiboToken.expireIn) * 1000);
			snat.save(flush:true);
			render text:"Your access token is: ${snat.accessToken}, which will expire in ${snat.expireTime}"
		}catch(Exception e){
			render text:e.message, status:500
		}
	}

	def refresh_token(String deviceId){
		if(!deviceId){
			deviceId = session.getAttribute('deviceId');
			if(!deviceId){
				render text:"Missing deviceId, aborted", status:409
				return;
			}
		}
		
		SocialNetworkAccessToken snat = SocialNetworkAccessToken.findByDeviceId(deviceId);
		if(snat){
			// post user/pass to weibo server to renew the access token
			Oauth oauth = new Oauth();
			AccessToken t = oauth.refreshToken(snat.socialUsername, snat.socialPassword);
			if(t){
				// success!
				snat.accessToken = t.accessToken;
				snat.refreshToken = t.refreshToken;
				// weibo api returns the expireIn value in seconds, convert to the date object
				snat.expireTime = new java.util.Date(System.currentTimeMillis() + (Long.parseLong(t.expireIn) * 1000));
				snat.save(flush:true);
				render text:"Token refreshed: ${snat.accessToken}, expires in ${snat.expireTime}";
				
				return;
			}
		}
		
		// failed, redirect to the bind page
		redirect action:'bind', params:[deviceId:deviceId];
	}
	
	/**
	 * Post weibo
	 * @param deviceId
	 * @param text
	 * @return
	 */
	def post(String deviceId, String text){
		if(!deviceId){
			deviceId = session.getAttribute('deviceId');
		}
		if(!deviceId || !text){
			render text:"Missing parameter", status:409
			return;
		}
		
		SocialNetworkAccessToken snat = SocialNetworkAccessToken.findByDeviceId(deviceId);
		if(!snat){
			redirect action:'bind', params:[deviceId:deviceId]
			return;
		}
		
		Timeline timeline = new Timeline();
		timeline.setToken(snat.accessToken);
		Status status = timeline.UpdateStatus(text);
		render text:"post successed, ${status}"
	}

	def list(){
		render text:"list"
	}

	def read(){
		render text:"read"
	}

	def test_auth(String code){
		Oauth oauth = new Oauth();
		BareBonesBrowserLaunch.openURL(oauth.authorize("code","hello"));
		//System.out.print("Hit enter when it's done.[Enter]:");
		//BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		//String code = br.readLine();
		//log.info("code: " + code);
		try{
			System.out.println(oauth.getAccessTokenByCode(code));
		} catch (WeiboException e) {
			if(401 == e.getStatusCode()){
				log.info("Unable to get the access token.");
			}else{
				log.error(e);
			}
		}
	}
	
	def test_refresh(){
		Oauth oauth = new Oauth();
		AccessToken t = oauth.refreshToken("foobar@163.com", "secret");
		if(t){
			render text:"Token refreshed: ${t.accessToken}, expires in ${t.expireIn}s";
		}else{
			render text:"Token refresh failed";
		}
	}
}
