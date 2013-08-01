package mclub.social

import weibo4j.Oauth
import weibo4j.Timeline
import weibo4j.http.AccessToken
import weibo4j.model.Status
import weibo4j.model.WeiboException
import weibo4j.util.BareBonesBrowserLaunch

class WeiboController {

	def index() {
		render text:"auth,post,list,read"
	}

	private void saveDeviceIdInCookie(response, deviceId){
		/*
		// save device id in cookie
		def cookie = new Cookie('deviceId', deviceId);
		cookie.path = '/'
		cookie.maxAge = 0;
		response.addCookie( cookie)
		*/
	}
	
	private String getDeviceIdFromCookie(request){
		return null;
//		return g.cookie(name: 'deviceId');
	}
	
	def bind(String deviceId, String username, String password){
		if(!deviceId || !username || !password){
			flash.message = 'missing parameter!';
			render view:"bind_view.gsp"
			return;
		}
		
		
	}
	def auth(String deviceId, String code){
		if(deviceId){
			//store in cookie
			saveDeviceIdInCookie(response,deviceId);
		}else{
			// try to get device id from cookie
			deviceId = getDeviceIdFromCookie(request);
		}
		if(!deviceId){
			render text:"Missing parameters", status:401
			return;
		}
		Oauth oauth = new Oauth();
		
		if(!code){
			// STEP - 1
			// retrieve the access token
			
			// first try our database
			SocialNetworkAccessToken accessToken = SocialNetworkAccessToken.findByDeviceId(deviceId);
			if(accessToken){
				// return or render the access token
				render text:"Your access token is: ${accessToken.accessToken}";
			}else{
				// redirect to sina oauth web page
				String authURL =  oauth.authorize("code","forester");
				redirect url:authURL;	
			}
			return;
		}
		
		
		// STEP - 2
		// get access token by the code and save it
		AccessToken token = null;
		try{
			token = oauth.getAccessTokenByCode(code);
			// save the token in database
			SocialNetworkAccessToken accessToken = SocialNetworkAccessToken.findByDeviceId(deviceId);
			if(!accessToken){
				accessToken = new SocialNetworkAccessToken();
				accessToken.deviceId = deviceId;
			}
			accessToken.accessToken = token.accessToken;
			accessToken.refreshToken = token.refreshToken;
			accessToken.socialUserId = token.uid;
			
			accessToken.save(flush:true);
			render text:"Your access token is: ${accessToken.accessToken}"
		}catch(Exception e){
			render text:e.message, status:500
		}
	}

	def post(String deviceId, String text){
		if(!deviceId || !text){
			render text:"Missing parameter", status:409
			return;
		}
		
		SocialNetworkAccessToken at = SocialNetworkAccessToken.findByDeviceId(deviceId);
		if(!at){
			redirect action:'auth', params:[deviceId:deviceId]
			return;
		}
		
		Timeline timeline = new Timeline();
		timeline.setToken(at.accessToken);
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
