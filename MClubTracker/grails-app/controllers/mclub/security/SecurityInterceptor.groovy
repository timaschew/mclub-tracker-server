package mclub.security

import grails.converters.JSON
import mclub.user.User


class SecurityInterceptor {
    def userService;

    SecurityInterceptor(){
//        match(controller:'admin')
//        .except(controller:'admin',action:'login');
//        match(controller:'user')
        match(uri:'/*/admin/**')
    }

    boolean before() {
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

    boolean after() { true }

    void afterView() {
        // no-op
    }
}

class ApiAccessInterceptor{
    def userService;
    ApiAccessInterceptor(){
        match(controller:"trackerAPI",action:"update*|user")
    }
    boolean before() {
        def token = params.token
        if(!token)
            token = request.token;
        if(!token)
            token = request.JSON.token;

        if(token){
            if(log.isDebugEnabled())
                log.debug("got token: ${token}")
            def userSession = userService.checkSessionToken(token);
            if(userSession){
                request['session'] = userSession;
                log.debug("User session loaded ${userSession.username}")
                return true;
            }
        }

        log.info("No token found in request, params:${params}");
        def r = [code:2/*APIResponse.SESSION_EXPIRED_ERROR*/,message:'Session expired'];
        render r as JSON;
        return false
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }
}
