package mclub.social

import java.util.Date;

/**
 * Domain class for user's external social 
 * @author shawn
 *
 */
class SocialNetworkAccessToken {

	static constraints = {
	}
	
	static mappings = {
		version false
	}

	String accessToken;
	String refreshToken;
	
	String socialUserId;
	String socialUsername;
	String socialPassword;
	
	String deviceId;
	String userId;
	Date lastUpdated;
}
