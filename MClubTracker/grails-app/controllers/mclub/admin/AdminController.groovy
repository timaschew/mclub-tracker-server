package mclub.admin

import grails.validation.Validateable
import mclub.user.AuthUtils;
import mclub.user.User
import mclub.user.UserService;
import mclub.user.UserService.UserSession

class AdminController {
	UserService userService;
	
	def index(){
		render view:"admin.gsp"
	}
	
	def login(LoginCommand loginCommand, String returnURL){
		if(params.logout){
			session['user'] = null;
			redirect action:'index';
			return;	
		}
		
		User user = session['user'];
		if(user){
			// we're done!
			if(user.type == User.USER_TYPE_ADMIN){
				redirect action:'index';
			}else if(user.type == User.USER_TYPE_USER){
				redirect controller:'user', action:'password';
			}else{
				// invalid user!
				session['user'] = null;
				render text:"No permission";
			}
			return;
		}
		
		if(request.method == 'POST'){
			if(!loginCommand.hasErrors()){
				
				UserSession usession = null;
				if(AuthUtils.isMobilePhoneNumber(loginCommand.username)){
					usession = userService.loginByPhone(loginCommand.username, loginCommand.password, true);
				}else{
					usession = userService.login(loginCommand.username, loginCommand.password, true);
				}
				if(usession){
					user = User.findByName(usession.username);
					session['user'] = user;
					if(returnURL){
						redirect uri:returnURL;
						return;
					}
					if(user.type == User.USER_TYPE_ADMIN){
						redirect action:'index';
					}else if(user.type == User.USER_TYPE_USER){
						redirect controller:'user', action:'password';
					}else{
						// invalid user!
						session['user'] = null;
						render text:"No permission";
					}
					return;
				}else{
					flash['message'] = "Login failed";
				}
				loginCommand.password = null;
			}
			render(view:"login", model: [loginCommand: loginCommand]);
		}else{
			render(view:"login");
		}
		
	}
}

@Validateable
class LoginCommand{
	String username;
	String password;
}