package mclub.user

class User {

	public static final int USER_TYPE_DISABLED = 0;
	public static final int USER_TYPE_GUEST = 1;
	public static final int USER_TYPE_USER = 2;
	public static final int USER_TYPE_ADMIN = 3;
	
	static constraints = {
		avatar 		blank:true, nullable:true
		settings 	blank:true, nullable:true
		settings	type:'text'
		name		unique: true
		displayName 		blank:true, nullable:true
	}
	
	static mapping = {
		name		index:'idx_user_name'
		phone		index:'idx_user_phone'
		type		index:'idx_user_type'
		version 	false
	}
	
	String name;
	String phone;
	Integer type;
	
	String avatar; 		// index or path of user avatar file, could be null
	String settings; 	// JSON string of user settings.
	
	String displayName; // user display name
	
	Date creationDate;

	String passwordHash;
	String passwordSalt;
	String sessionSalt;
}
