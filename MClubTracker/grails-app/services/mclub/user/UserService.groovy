package mclub.user

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import grails.transaction.Transactional
import mclub.user.User
import static mclub.user.AuthUtils.*;

class UserService {
	
	ConcurrentHashMap<String,UserSession> sessions = new ConcurrentHashMap<String,UserSession>();
	ExecutorService sessionCleanupThread;
	private static final long SESSION_EXPIRE_TIME_SEC = 30 * 60; // session expires in 30 mins idle by defalt
	private static final long SESSION_CHECK_INTERVAL_MS = 3000; 
	private static final Object sleepLock = new Object();
	volatile boolean runFlag = false;
	
	@PostConstruct
	public void start(){
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
							if(UserService.this.isSessionExpired(us,t)){
								// session expired;
								sessions.remove(key);
								log.info("Session ${key} expired")
							}
						}
					}finally{
						try{
							synchronized(sleepLock){
								sleepLock.wait(SESSION_CHECK_INTERVAL_MS);
							}
							//Thread.sleep(SESSION_CHECK_INTERVAL_MS);
						}catch(Exception e){
							e.printStackTrace();
						}
					}
				}
			}
		});

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
		runFlag = false;
		synchronized(sleepLock){
			sleepLock.notifyAll();
		}
		try{
			sessionCleanupThread?.shutdown();
		}catch(Exception e){
		}
		sessionCleanupThread = null;
		log.info "UserService destroyed"
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
	/**
	 * Login user with username/password pair
	 * @return session token if auth succeed
	 */
	public UserSession login(String username, String password){
		// search user by phone
		// compare passwordHash with md5(password*hash)
		// if success, generate session credential with md5(phone*hash)
		User user = User.findByName(username);
		if(user){
			String hash1 = hashPassword(password,user.passwordSalt);
			String hash2 = user.passwordHash;
			if(hash1.equals(hash2)){
				// login success
				// remove previous session
				for(UserSession us in sessions.values()){
					if(us.username.equals(username)){
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
				return usession.cloneone();
			}else{
				log.info("User ${username} login failed, wrong password, expected hash: ${hash2}, but got ${hash1}");
			}
		}else{
			log.info("User ${username} not found");
		}
		return null;
	}

	
	/**
	 * Login user with phone/password pair
	 * @return session token if auth succeed
	 */
	public UserSession loginByPhone(String phone, String password){
		// search user by phone
		User user = User.findByPhone(phone);
		if(user){
			String username = user.name;
			return login(username,password);
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
		usession.username = user.name;
		usession.token = hashSessionToken(user.name, usession.timestamp,user.sessionSalt);
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
