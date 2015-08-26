package user

import javax.annotation.PostConstruct;

import grails.transaction.Transactional
import mclub.user.User

@Transactional
class UserService {
	Map<String,Object> sessions;
	
	@PostConstruct
	public void start(){
		
		// add test user
		User.withTransaction {
			User.executeUpdate('delete from User')
		}
		User.withTransaction {
			if(User.count() == 0){
				User u = new User(name:'admin', phone:'0001', type:User.USER_TYPE_ADMIN, creationDate:new java.util.Date(), passwordHash:'61b0c42c37319340f3ae4625bab292a4'/*secret*/, passwordSalt:'salt',sessionSalt:'salt',avatar:'',settings:'');
				if (!u.save(flush:true)){
					log.error(u.errors);	
				}
			}
		}
	}
	/**
	 * Generate user login credentials based on salt
	 * @return
	 */
	String login(String phone, String password){
		// search user by phone
		// compare passwordHash with md5(password*hash)
		// if success, generate session credential with md5(phone*hash)
		User user = User.findByPhone(phone);
		if(user){
			String hash1 = hash(password,user.passwordSalt);
			String hash2 = user.passwordHash;
			if(hash1.equals(hash2)){
				// login success, generate session token
				return hash(phone,user.sessionSalt);
			}else{
				log.info("User ${phone} login failed, wrong password, expected hash: ${hash2}, but got ${hash1}");
			}
		}else{
			log.info("User ${phone} not found");
		}
		return "";
	}
	
	private static final String MD5_HASH_CONCAT_CHAR = "*";
	private String hash(String data, String salt){
		def t = data + MD5_HASH_CONCAT_CHAR + salt;
//		MessageDigest md5 = MessageDigest.getInstance("MD5");
//		md5.update(claimedContent.getBytes());
//		BigInteger hash = new BigInteger(1, md5.digest());
//		String hashFromContent = hash.toString(16);
		return t.encodeAsMD5();
	}
}
