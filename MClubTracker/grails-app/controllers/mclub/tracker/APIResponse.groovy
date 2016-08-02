package mclub.tracker

/**
 * Created by shawn on 16/8/2.
 */
class APIResponse {
    public static final int NO_ERROR = 0
    public static final int OPERATION_FAIL_ERROR = 1;
    public static final int SESSION_EXPIRED_ERROR = 2;
    public static final int AUTH_DENIED_ERROR = 3;

    public static Map<String,Object> OK(){
        Map<String,Object> resp = ['code':NO_ERROR, 'message':'OK'];
        return resp;
    }

    public static Map<String,Object> OK(Object data){
        Map<String,Object> resp = ['code':NO_ERROR, 'message':'OK', 'data':data];
        return resp;
    }

    public static Map<String,Object> SUCCESS(String message){
        Map<String,Object> resp = ['code':NO_ERROR, 'message':message];
        return resp;
    }


    public static Map<String,Object> ERROR(String message){
        Map<String,Object> resp = ['code':OPERATION_FAIL_ERROR, 'message':message];
        return resp;
    }

    public static Map<String,Object> ERROR(int code, String message){
        Map<String,Object> resp = ['code':code, 'message':message];
        return resp;
    }
}
