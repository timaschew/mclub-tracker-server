package mclub.user

import java.security.MessageDigest
import java.util.Random;
import java.util.regex.*;

/**
 * A simple util for authentication credentials.
 * @author shawn
 *
 */
public class AuthUtils {
	private static final String HASH_CONCAT_CHAR = "*";
	public static String hashPassword(String password, String salt){
		def t = password + HASH_CONCAT_CHAR + salt;
		return encodeAsMD5(t);
		//return t.encodeAsMD5();
	}
	
	public static String hashSessionToken(String username, Long timestamp, String salt){
		def t = username + HASH_CONCAT_CHAR + timestamp.toString() + HASH_CONCAT_CHAR + salt;
		//return t.encodeAsMD5();
		return encodeAsMD5(t);
	}
	
	private static String encodeAsMD5(String text){
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		md5.update(text.getBytes());
		BigInteger hash = new BigInteger(1, md5.digest());
		return String.format("%032x", hash);
	}
	
	private static final String CHAR_LIST = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
	private static final Random rnd = new Random();
	public static String generateSalt(int saltLen){
		StringBuilder buf = new StringBuilder();
		for(int i = 0;i < saltLen;i++){
			buf.append(CHAR_LIST.charAt(rnd.nextInt(CHAR_LIST.length())));
		}
		return buf.toString();
	}
	
	private static final String APRS_CALL_REGEXP = '^([a-zA-Z0-9]+)\\-([a-zA-Z0-9]+)$';
	
	/**
	 * Extract APRS Call string
	 * @param aprsCall
	 * @return CALL and SSID in array or null if invalid aprs call met.
	 */
	public static String[] extractAPRSCall(String aprsCall){
		//def reg = /^([a-zA-Z0-9]+)\-([0-9]+)$/
		Pattern p = Pattern.compile(APRS_CALL_REGEXP);
		Matcher m = p.matcher(aprsCall);
		if(m.matches() && m.groupCount() == 2){
			String[] ret = new String[2];
			ret[0] = m.group(1);
			ret[1] = m.group(2);  
			return ret;	
		}
		
		if((aprsCall.length() == 5 || aprsCall.length() == 6) && (aprsCall.indexOf('-') == -1)){
			String[] ret = new String[2];
			ret[0] = aprsCall
			ret[1] = '0'; // char 0
			return ret;
		}
		/*
		def matcher = (aprsCall =~ /^([a-zA-Z0-9]+)\-([0-9]+)$/)
		
		if(matcher.getCount() >= 2){
			String[] ret = new String[2];
			ret[0] = matcher[0];
			ret[1] = matcher[1];
			return ret;
		}
		*/
		return null;
	}
	
	public static boolean isMobilePhoneNumber(String input){
		if(input?.length() == 11){
			for(int i = 0;i < input.length();i++){
				char c = input.charAt(i);
				if(c >='0' && c <='9'){
					continue;
				}else{
					return false;
				}
			}
			return true;
		}
		return false;
	}

}
