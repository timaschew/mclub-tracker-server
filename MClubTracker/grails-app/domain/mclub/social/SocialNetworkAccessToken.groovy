package mclub.social

import java.util.Date;

/**
 * Domain class for user's external social 
 * @author shawn
 *
 */
class SocialNetworkAccessToken {

	static constraints = {
		accessToken blank:true,nullable:true
		refreshToken blank:true,nullable:true
		expireTime blank:true,nullable:true
		socialUserId blank:true,nullable:true
	}
	
	static mapping = {
		version false
	}

	String accessToken;
	String refreshToken;
	Date expireTime;
	
	String socialUserId;
	String socialUsername;
	String socialPassword;
	
	String deviceId;
//	String userId;
	Date lastUpdated;
}
