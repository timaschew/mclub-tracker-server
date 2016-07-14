package mclub.user



import static org.springframework.http.HttpStatus.*
import grails.transaction.Transactional

@Transactional(readOnly = true)
class UserController {

	UserService userService;
	
    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 25, 100)
		if(!params.sort){
			// by default should sort by row id
			params.sort = 'id';
		}
        return [userInstanceList: User.list(params), userInstanceCount: User.count()]
    }

    def show(/*User userInstance*/) {
		User u = null;
		if(params.id){
			if(params.id.isNumber())
				u = User.load(params.id);
			if(!u){
				u = User.findByName(params.id);
			}
		}else if(params.phone){
			u = User.findByPhoneLike("${params.phone}%");
		}
		respond u;
    }

    def create() {
		User user = new User(params);
        user.creationDate = new java.util.Date();
        respond user;
    }

    @Transactional(readOnly = false)
    def save(User user) {
        if (user == null) {
            notFound()
            return
        }

//        if (user.hasErrors()) {
//            respond user.errors, view:'create'
//            return
//        }

		if(!userService.createUserAccount(user, 'changeme!')){
			respond user.errors, view:'create';
			return;
		}

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.created.message', args: [message(code: 'user.label', default: 'User'), user.id])
                redirect user
            }
            '*' { respond user, [status: CREATED] }
        }
    }

    def edit(User user) {
        respond user
    }

	/**
	 * Update user password
	 * @param oldPassword
	 * @param newPassword1
	 * @param newPassword2
	 * @return
	 */
    @Transactional(readOnly = false)
	def password(PasswordChangeCommand passwordChangeCommand){
		User user = session['user'];
		if(!user){
			log.error("SECURITY ISSUE - User access the change password page without any session credentials, check filter settings!");
			//flash['message'] = "Not logged in";
			render text:"error occured"
			return;
		}

		if(request.method == 'GET'){
			return;
		}
		
		if(passwordChangeCommand.hasErrors()){
			render(view:"password", model: [passwordChangeCommand: passwordChangeCommand])
			return;
		}

		if(!passwordChangeCommand.newPassword1.equals(passwordChangeCommand.newPassword2)){
			flash['message'] = "Password not changed, the new passwords you typed in are mismatch!";
			//render(view:"password", model: [passwordChangeCommand: passwordChangeCommand])
			render(view:"password");
			return;
		}
		// validate old password
		
		def updatedUser = userService.updateUserPassword(user.name,passwordChangeCommand.oldPassword, passwordChangeCommand.newPassword1);
		if(updatedUser){
			//update success
			flash['message'] = 'Update Password SUCCESS!';
		}else{
			flash['message'] = 'Update Password FAILED!';
		}
	}
	
    @Transactional
    def update(User user) {
        if (user == null) {
            notFound()
            return
        }

        if (user.hasErrors()) {
            respond user.errors, view:'edit'
            return
        }

        user.save flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.updated.message', args: [message(code: 'User.label', default: 'User'), user.id])
                redirect user
            }
            '*'{ respond user, [status: OK] }
        }
    }

    @Transactional
    def delete(User userInstance) {

        if (userInstance == null) {
            notFound()
            return
        }

        userInstance.delete flush:true

        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'User.label', default: 'User'), userInstance.id])
                redirect action:"index", method:"GET"
            }
            '*'{ render status: NO_CONTENT }
        }
    }

    protected void notFound() {
        request.withFormat {
            form multipartForm {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'user.label', default: 'User'), params.id])
                redirect action: "index", method: "GET"
            }
            '*'{ render status: NOT_FOUND }
        }
    }
	
}

class PasswordChangeCommand{
	String oldPassword;
	String newPassword1;
	String newPassword2;
	
	static constraints = {
		oldPassword size: 5..15, blank: false
		newPassword1 size: 5..15, blank: false
		newPassword1 size: 5..15, blank: false
	}
}
