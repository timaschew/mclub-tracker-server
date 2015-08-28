package mclub.user

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import grails.transaction.Transactional
import mclub.user.User
import static mclub.user.AuthUtils.*;

@Transactional
class UserService {
	
	ConcurrentHashMap<String,UserSession> sessions = new ConcurrentHashMap<String,UserSession>();
	ExecutorService sessionCleanupThread;
	private static final long SESSION_EXPIRE_TIME_SEC = 30 * 60; // session expires in 30 mins idle by defalt
	private static final long SESSION_CHECK_INTERVAL_MS = 3000; 
	volatile boolean runFlag = false;
	
	@PostConstruct
	public void start(){
		// add test user
		/*
		User.withTransaction {
			User.executeUpdate('delete from User')
		}
		*/
		User.withTransaction {
			if(User.count() == 0){
				User u = new User(name:'admin', phone:'0001', type:User.USER_TYPE_ADMIN, creationDate:new java.util.Date(),avatar:'',settings:'');
				if(!this.createUserAccount(u, "secret")){
					log.error(u.errors);
				}
			}
		}
		
		// start the session check thread
		runFlag = true;
		sessionCleanupThread = java.util.concurrent.Executors.newFixedThreadPool(1);
		sessionCleanupThread.execute(new Runnable(){
			public void run(){
				while(runFlag){
					try{
						Set keys = new HashSet(sessions.keySet());
						long t = System.currentTimeMillis();
						for(String key in keys){
							UserSession us = sessions.get(key);
							if(isSessionExpired(us,t)){
								// session expired;
								sessions.remove(key);
								log.info("Session ${key} expired")
							}
						}
						Thread.sleep(SESSION_CHECK_INTERVAL_MS);
					}catch(Exception e){
						// noop
					}
				}
			}
		});
	
		log.info("UserService initialized");
	}

	@PreDestroy
	public void stop(){
		runFlag = false;
		try{
			sessionCleanupThread?.shutdown();
		}catch(Exception e){
		}
		sessionCleanupThread = null;
		log.info "UserService destroyed"
	}
	
	//////////////////////////////////////////////////////////////////////////////
	// User creation Part
	
	/**
	 * Register user account
	 * @param user
	 * @return
	 */
	public boolean createUserAccount(User user, String password){
		// generate salts
		user.passwordSalt = AuthUtils.generateSalt(5);
		user.sessionSalt = AuthUtils.generateSalt(5);
		
		// generate password hash
		user.passwordHash = AuthUtils.hashPassword(password,user.passwordSalt);
		
		// TODO check if user already exists
		return user.save(flush:true);
	}
	
	
	//////////////////////////////////////////////////////////////////////////////
	// Authentication Part
	
	/**
	 * Login user with phone/password pair
	 * @return session token if auth succeed
	 */
	public String login(String phone, String password){
		// search user by phone
		// compare passwordHash with md5(password*hash)
		// if success, generate session credential with md5(phone*hash)
		User user = User.findByPhone(phone);
		if(user){
			String hash1 = hashPassword(password,user.passwordSalt);
			String hash2 = user.passwordHash;
			if(hash1.equals(hash2)){
				// login success
				// remove previous session
				for(UserSession us in sessions.values()){
					if(us.username.equals(phone)){
						// found existing session
						sessions.remove(us.token);
						log.debug("remove existing session token: ${us.token}")
						break;
					}
				}
				
				// generate session token
				UserSession usession = generateUserSession(user);
				sessions.put(usession.token, usession);
				log.debug("generated session token: ${usession.token}")
				return usession.token;
			}else{
				log.info("User ${phone} login failed, wrong password, expected hash: ${hash2}, but got ${hash1}");
			}
		}else{
			log.info("User ${phone} not found");
		}
		return "";
	}

	/**
	 * Check user token , also expand the session time stamp.
	 * 
	 * @param sessionToken
	 * @return true if authorize is granted.
	 */
	public UserSession checkSessionToken(String sessionToken){
		UserSession us = sessions.get(sessionToken);
		if(us){
			long t = System.currentTimeMillis();
			if(isSessionExpired(us,t)){
				sessions.remove(sessionToken);
				return null;
			}
			us.timestamp = t;
			return us.cloneone();
		}
		return null;
	}
	
	/**
	 * Generate user session token/user name and store in session
	 * @param sessionId
	 * @param sessionSalt
	 * @return
	 */
	private UserSession generateUserSession(User user){
		UserSession usession = new UserSession();
		usession.username = user.phone;
		usession.token = hashSessionToken(user.phone, usession.timestamp,user.sessionSalt);
		return usession;
	}
	
	private boolean isSessionExpired(UserSession usession, long t){
		return usession && ((t - usession.timestamp) / 1000 > SESSION_EXPIRE_TIME_SEC)
	}
	/**
	 * User session entity
	 * @author shawn
	 *
	 */
	public static class UserSession{
		String username;
		String token;
		long timestamp = System.currentTimeMillis();
		
		public UserSession cloneone(){
			return new UserSession(username:username, token:token, timestamp:timestamp);
		}
	}
	
}
