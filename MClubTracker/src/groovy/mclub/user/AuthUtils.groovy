package mclub.user

import java.security.MessageDigest
import java.util.Random;

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
}
