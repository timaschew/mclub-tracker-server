import grails.converters.JSON
import java.util.concurrent.atomic.AtomicLong
import mclub.user.User


class SecurityFilters {
	def userService;
	def filters = {
		adminFilter(uri:"/admin/**"){
			before = {
				if(controllerName?.equals('admin') && actionName?.equals('login')){
					return true;
				}
				
				User user = session['user'];
				if(!user){
					redirect(controller:'admin', action:'login');
					return false;
				}
				
				// for change password, allows normal users
				if(controllerName?.equals('user') && actionName?.equals('password') && user.type == User.USER_TYPE_USER){
					return true
				}else if(user.type == User.USER_TYPE_ADMIN){
					return true;
				}
				return false;
			}
		} // admin filter
		
		
		apiFilter(controller:'trackerAPI', action:'update*|user'){
			before = {
				def token = params.token
				if(!token)
					token = request.token;
				if(!token)
					token = request.JSON.token;
					
				if(token){
					log.debug("got token: ${token}")
					def userSession = userService.checkSessionToken(token);
					if(userSession){
						request['session'] = userSession;
						log.debug("User session loaded ${userSession.username}")
						return true;
					}
				}
				
				log.info("No token found in request");
				def r = [code:2,message:'Session expired'];
				render r as JSON;
				return false
			}
		} // api filter
		
	}
}
