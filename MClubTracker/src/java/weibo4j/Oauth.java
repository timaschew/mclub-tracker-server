package weibo4j;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

import weibo4j.http.AccessToken;
import weibo4j.http.BASE64Encoder;
import weibo4j.model.PostParameter;
import weibo4j.model.WeiboException;
import weibo4j.org.json.JSONException;
import weibo4j.org.json.JSONObject;
import weibo4j.util.WeiboConfig;

public class Oauth extends Weibo{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7003420545330439247L;
	// ----------------------------针对站内应用处理SignedRequest获取accesstoken----------------------------------------
	public String access_token;
	public String user_id;

	public String getToken() {
		return access_token;
	}

	/*
	 * 解析站内应用post的SignedRequest split为part1和part2两部分
	 */
	public String parseSignedRequest(String signed_request) throws IOException,
			InvalidKeyException, NoSuchAlgorithmException {
		String[] t = signed_request.split("\\.", 2);
		// 为了和 url encode/decode 不冲突，base64url 编码方式会将
		// '+'，'/'转换成'-'，'_'，并且去掉结尾的'='。 因此解码之前需要还原到默认的base64编码，结尾的'='可以用以下算法还原
		int padding = (4 - t[0].length() % 4);
		for (int i = 0; i < padding; i++)
			t[0] += "=";
		String part1 = t[0].replace("-", "+").replace("_", "/");

		SecretKey key = new SecretKeySpec(WeiboConfig
				.getValue("client_SERCRET").getBytes(), "hmacSHA256");
		Mac m;
		m = Mac.getInstance("hmacSHA256");
		m.init(key);
		m.update(t[1].getBytes());
		String part1Expect = BASE64Encoder.encode(m.doFinal());

		sun.misc.BASE64Decoder decode = new sun.misc.BASE64Decoder();
		String s = new String(decode.decodeBuffer(t[1]));
		if (part1.equals(part1Expect)) {
			return ts(s);
		} else {
			return null;
		}
	}

	/*
	 * 处理解析后的json解析
	 */
	public String ts(String json) {
		try {
			JSONObject jsonObject = new JSONObject(json);
			access_token = jsonObject.getString("oauth_token");
			user_id = jsonObject.getString("user_id");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return access_token;

	}

	/*----------------------------Oauth接口--------------------------------------*/

	public AccessToken getAccessTokenByCode(String code) throws WeiboException {
		return new AccessToken(client.post(
				WeiboConfig.getValue("accessTokenURL"),
				new PostParameter[] {
						new PostParameter("client_id", WeiboConfig
								.getValue("client_ID")),
						new PostParameter("client_secret", WeiboConfig
								.getValue("client_SERCRET")),
						new PostParameter("grant_type", "authorization_code"),
						new PostParameter("code", code),
						new PostParameter("redirect_uri", WeiboConfig
								.getValue("redirect_URI")) }, false));
	}

	public String authorize(String response_type,String state) throws WeiboException {
		return WeiboConfig.getValue("authorizeURL").trim() + "?client_id="
				+ WeiboConfig.getValue("client_ID").trim() + "&redirect_uri="
				+ WeiboConfig.getValue("redirect_URI").trim()
				+ "&response_type=" + response_type
				+ "&state="+state;
	}
	public String authorize(String response_type,String state,String scope) throws WeiboException {
		return WeiboConfig.getValue("authorizeURL").trim() + "?client_id="
				+ WeiboConfig.getValue("client_ID").trim() + "&redirect_uri="
				+ WeiboConfig.getValue("redirect_URI").trim()
				+ "&response_type=" + response_type
				+ "&state="+state
				+ "&scope="+scope;
	}
	
	public AccessToken refreshToken(String username,String password){
		String clientId = WeiboConfig.getValue("client_ID") ;  
        String redirectURI = WeiboConfig.getValue("redirect_URI") ;  
        String url = WeiboConfig.getValue("authorizeURL");  
          
        PostMethod postMethod = new PostMethod(url);  
        //应用的App Key   
        postMethod.addParameter("client_id",clientId);  
        //应用的重定向页面  
        postMethod.addParameter("redirect_uri",redirectURI);  
        //模拟登录参数  
        //开发者或测试账号的用户名和密码  
        postMethod.addParameter("userId", username);  
        postMethod.addParameter("passwd", password);  
        postMethod.addParameter("isLoginSina", "0");  
        postMethod.addParameter("action", "submit");  
        postMethod.addParameter("response_type","code");  
        HttpMethodParams param = postMethod.getParams();  
        param.setContentCharset("UTF-8");  
        //添加头信息  
        List<Header> headers = new ArrayList<Header>();  
        headers.add(new Header("Referer", "https://api.weibo.com/oauth2/authorize?client_id="+clientId+"&redirect_uri="+redirectURI+"&from=sina&response_type=code"));  
        headers.add(new Header("Host", "api.weibo.com"));  
        headers.add(new Header("User-Agent","Mozilla/5.0 (Windows NT 6.1; rv:11.0) Gecko/20100101 Firefox/11.0"));  
        HttpClient client = new HttpClient();  
        client.getHostConfiguration().getParams().setParameter("http.default-headers", headers);  
        try {
	        client.executeMethod(postMethod);
        } catch (Exception e) {
        	System.out.println("Token refresh failed with exception: " + e.getMessage());
	        return null;
        }
        
        int status = postMethod.getStatusCode();  
        System.out.println(status);  
        if (status != 302)  
        {  
            System.out.println("token刷新失败");  
            return null;  
        }  
        //解析Token  
        Header location = postMethod.getResponseHeader("Location");  
        if (location != null)   
        {  
            String retUrl = location.getValue();  
            int begin = retUrl.indexOf("code=");  
            if (begin != -1) {  
                int end = retUrl.indexOf("&", begin);  
                if (end == -1)  
                    end = retUrl.length();  
                String code = retUrl.substring(begin + 5, end);  
                if (code != null) {  
                    Oauth oauth = new Oauth();  
                    try{  
                        AccessToken token = oauth.getAccessTokenByCode(code);  
                        return token;  
                    }catch(Exception e){  
                        e.printStackTrace();  
                    }  
                }  
            }  
        }  
        return null; 
	}
}
