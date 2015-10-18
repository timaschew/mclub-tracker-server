package mclub.user

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import grails.transaction.Transactional
import mclub.sys.TaskService
import mclub.user.User
import static mclub.user.AuthUtils.*;

public class UserService implements Runnable{
	
	ConcurrentHashMap<String,UserSession> userSessionStore = new ConcurrentHashMap<String,UserSession>();
	private static final long SESSION_EXPIRE_TIME_SEC = 48 * 60 * 60; // session expires in 48 hours of idle by default
	private static final long SESSION_CHECK_INTERVAL_MS = 3000; 
	TaskService taskService;
	
	@PostConstruct
	public void start(){
		// start the session check task
		taskService.execute(this, SESSION_CHECK_INTERVAL_MS);

		// QUICK-AND-DIRTY solution: Perform a delay data initialize due to some dependency issues.
		new Thread(new java.lang.Runnable(){
			public void run(){
				Thread.sleep(5000);
				UserService.this.initUserData();
			}
		}).start();
	
		log.info("UserService initialized");
	}

	@PreDestroy
	public void stop(){
		log.info "UserService destroyed"
	}

	/**
	 * Called by the task service reguarlly.	
	 */
	public void run(){
		// check the session
		doSessionCheckTask();
	}
	
	private void doSessionCheckTask(){
		// Copy the keys first to avoid concurrent modificaiton issue.
		//log.debug("do session check task")
		Set keys = new HashSet(userSessionStore.keySet());
		long t = System.currentTimeMillis();
		for(String key in keys){
			UserSession us = userSessionStore.get(key);
			if(isSessionExpired(us,t)){
				// session expired;
				userSessionStore.remove(key);
				log.info("Session ${key} expired")
			}
		}
	}
	
	@Transactional
	public void initUserData(){
		// add test user
		/*
		User.withTransaction {
			User.executeUpdate('delete from User')
		}
		*/
		if(User.count() == 0){
			User u = new User(name:'admin', phone:'0001', type:User.USER_TYPE_ADMIN, creationDate:new java.util.Date(),avatar:'',settings:'');
			if(!this.createUserAccount(u, "secret")){
				log.error(u.errors);
			}
			log.info("User data initialized");
		}
	}
	
	//////////////////////////////////////////////////////////////////////////////
	// User creation Part
	
	/**
	 * Register user account
	 * @param user
	 * @return
	 */
	public boolean createUserAccount(User user, String password){
		user.creationDate = new java.util.Date();
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
	public UserSession login(String username, String password) throws AuthException{
		return login(username,password,false);
	}
	
	/**
	 * Login user with username/password pair
	 * @return session token if auth succeed
	 */
	public UserSession login(String username, String password, boolean authOnly) throws AuthException{
		// search user by phone
		// compare passwordHash with md5(password*hash)
		// if success, generate session credential with md5(phone*hash)
		User user = User.findByName(username);
		if(!user){
			log.info("User ${username} not found");
			throw new AuthException("User not found");
		}
		
		if(user.type == 0){
			// user account disabled.
			throw new AuthException("User account is not activated yet");
		}
		
		String hash1 = hashPassword(password,user.passwordSalt);
		String hash2 = user.passwordHash;
		if(!hash1.equals(hash2)){
			log.info("User ${username} login failed, wrong password, expected hash: ${hash2}, but got ${hash1}");
			throw new AuthException("User account password mismatch");
		}
		
		// login success
		// if authOnly == false, will replace the previous session
		// that will kick-off previous user
		// generate session token
		UserSession usession = generateUserSession(user);
		if(log.isDebugEnabled()) log.debug("generated new session token: ${usession.token}")

		if(!authOnly){
			for(UserSession us in userSessionStore.values()){
				if(us.username.equals(username)){
					// found existing session
					userSessionStore.remove(us.token);
					log.debug("removed previous session token: ${us.token}")
					break;
				}
			}
			userSessionStore.put(usession.token, usession);
		}
		return usession.cloneone();
	}

	
	/**
	 * Login user with phone/password pair
	 * @return session token if auth succeed
	 */
	public UserSession loginByPhone(String phone, String password, boolean authOnly) throws AuthException{
		// search user by phone
		User user = User.findByPhone(phone);
		if(user){
			String username = user.name;
			return login(username,password,authOnly);
		}else{
			// no user found
			return null;
		}
	}
	
	public User updateUserPassword(String username, String oldpassword, String newpassword){
		User user = User.findByName(username);
		if(!user) return null;
		
		if(!user.passwordHash.equals(hashPassword(oldpassword,user.passwordSalt))){
			// oldpassword mismatch
			log.warn("User ${username} failed to change the password due to incorrect old passwords");
			return null;
		}
		
		if(oldpassword.equals(newpassword)){
			// not chaning the password because they're same
			return null;
		}
		
		// really update the password
		user.passwordHash = AuthUtils.hashPassword(newpassword,user.passwordSalt);
		if(!user.save(flush:true)){
			//TODO - update user change password timestamp;
			log.error("Error updating user password, " + user.errors)
			return null;
		}
		
		return user;
	}

	/**
	 * Check user token , also expand the session time stamp.
	 * 
	 * @param sessionToken
	 * @return true if authorize is granted.
	 */
	public UserSession checkSessionToken(String sessionToken){
		UserSession us = userSessionStore.get(sessionToken);
		if(us){
			long t = System.currentTimeMillis();
			if(isSessionExpired(us,t)){
				userSessionStore.remove(sessionToken);
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
		usession.username = user.name;
		usession.token = hashSessionToken(user.name, usession.timestamp,user.sessionSalt);
		usession.type = user.type;
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
		Integer type;
		long timestamp = System.currentTimeMillis();
		
		public UserSession cloneone(){
			return new UserSession(username:username, token:token, timestamp:timestamp,type:type);
		}
	}
	
	public static class AuthException extends Exception{
		public AuthException(String message){
			super(message);
		}
	}
	
}
