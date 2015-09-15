import grails.converters.JSON
import java.util.concurrent.atomic.AtomicLong
import mclub.user.User


class SecurityFilters {
	def userService;
	
	private static final AtomicLong REQUEST_NUMBER_COUNTER = new AtomicLong();
	private static final String START_TIME_ATTRIBUTE = 'Controller__START_TIME__'
	private static final String REQUEST_NUMBER_ATTRIBUTE = 'Controller__REQUEST_NUMBER__'
	def filters = {
		
		performanceLogFilter(controller:'x',action:'y'){
			before = {
				if (log.debugEnabled){
					long start = System.currentTimeMillis()
					long currentRequestNumber = REQUEST_NUMBER_COUNTER.incrementAndGet()
		
					request[START_TIME_ATTRIBUTE] = start
					request[REQUEST_NUMBER_ATTRIBUTE] = currentRequestNumber
		
					log.debug "preHandle request #$currentRequestNumber : " +
					   "'$request.servletPath'/'$request.forwardURI', " +
					   "from $request.remoteHost ($request.remoteAddr) " +
					   " at ${new Date()}, Ajax: $request.xhr, controller: $controllerName, " +
					   "action: $actionName, params: ${new TreeMap(params)}"
				}
			} // before
			
			after = { Map model ->
				if(log.debugEnabled){
					long start = request[START_TIME_ATTRIBUTE]
					long end = System.currentTimeMillis()
					long requestNumber = request[REQUEST_NUMBER_ATTRIBUTE]
		
					def msg = "postHandle request #$requestNumber: end ${new Date()}, " +
							  "controller total time ${end - start}ms"
					if (log.traceEnabled) {
						log.trace msg + "; model: $model"
					}
					else {
						log.debug msg
					}
				}
			} // after
			
			afterView = { Exception e ->

	            if (!log.debugEnabled) return true
	
	            long start = request[START_TIME_ATTRIBUTE]
	            long end = System.currentTimeMillis()
	            long requestNumber = request[REQUEST_NUMBER_ATTRIBUTE]
	
	            def msg = "afterCompletion request #$requestNumber: " +
	                      "end ${new Date()}, total time ${end - start}ms"
	            if (e) {
	               log.debug "$msg \n\texception: $e.message", e
	            }
	            else {
	               log.debug msg
	            }
	         }
		}
			
		
		apiFilter(controller:'trackerAPI', action:'update*|user'){
			before = {
				def token = params.token
				if(!token)
					token = request.token;
				if(!token)
					token = request.JSON.token;
					
				if(token){
					log.info("got token: ${token}")
					def userSession = userService.checkSessionToken(token);
					if(userSession){
						request['session'] = userSession;
						log.info("User session loaded ${userSession.username}")
						return true;
					}
				}
				
				log.info("No token found in request");
				def r = [code:2,message:'Session expired'];
				render r as JSON;
				return false
			}
		}
		
		adminFilter(uri:"/admin/**"){
			before = {
				if(controllerName?.equals('admin') && actionName?.equals('login')){
					return true;
				}
				User user = session['user'];
				if(user && user.type == User.USER_TYPE_ADMIN){
					return true;
				}else{
					redirect(controller:'admin', action:'login');
					return false;
				}
				
			}
		}
	}
}
