package mclub.user;

import static mclub.user.AuthUtils.generateSalt;
import static mclub.user.AuthUtils.hashPassword;
import static mclub.user.AuthUtils.hashSessionToken;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

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
	
	@Test
	public void test_extract_aprs_call(){
		String s = "BG5HHP-12";
		String[] r = AuthUtils.extractAPRSCall(s);
		assertNotNull(r);
		assertEquals(2,r.length);
		assertEquals("BG5HHP",r[0]);
		assertEquals("12",r[1]);
		
		s = "BG5HP-1";
		r = AuthUtils.extractAPRSCall(s);
		assertNotNull(r);
		assertEquals(2,r.length);
		assertEquals("BG5HP",r[0]);
		assertEquals("1",r[1]);
		
		s = "abcd1234";
		r = AuthUtils.extractAPRSCall(s);
		assertNull(r);
		
	}
	
	@Test
	public void test_generate_shortsalt_collision(){
		Set<String> results = new HashSet<String>();
		for(int i = 0;i < 1000000;i++){
			String s = AuthUtils.generateSalt(7).toLowerCase();
			if(results.contains(s)){
				fail("Got collisions after " + i + " rounds of iterations");
			}else{
				results.add(s);
			}
		}
	}
}

