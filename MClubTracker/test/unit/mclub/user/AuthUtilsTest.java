package mclub.user;

import static org.junit.Assert.*;

import org.junit.Test;
import static mclub.user.AuthUtils.*;

public class AuthUtilsTest {
	
	@Test
	public void test_generate_salt(){
		for(int i = 0;i <5;i++){
			System.out.println(generateSalt(5));
		}
	}
	
	@Test 
	public void test_hash_password(){
		for(int i = 0;i < 5;i++){
			System.out.println(hashPassword("secret","salt"));
		}
	}
	
	@Test 
	public void test_hask_session_token(){
		long t = System.currentTimeMillis() / 1000;
		for(int i = 0;i < 5;i++){
			System.out.println(hashSessionToken("admin",t+i,"salt"));
		}
	}
}

