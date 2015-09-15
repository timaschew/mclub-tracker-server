package mclub.admin

import grails.validation.Validateable
import mclub.user.User
import mclub.user.UserService;
import mclub.user.UserService.UserSession

class AdminController {
	UserService userService;
	
	def index(){
		render view:"admin.gsp"
	}
	
	def login(LoginCommand loginCommand){
		if(request.method == 'GET'){
			return;
		}
		if(!loginCommand.hasErrors()){
			UserSession usession = userService.login(loginCommand.username, loginCommand.password);
			if(usession){
				User user = User.findByName(usession.username);
				session['user'] = user;
				redirect action:'index';
				return;
			}else{
				flash['message'] = "Login failed";
			}
			loginCommand.password = null;
		}else{
			
		}
		render(view:"login", model: [loginCommand: loginCommand])
	}
}

@Validateable
class LoginCommand{
	String username;
	String password;
}
