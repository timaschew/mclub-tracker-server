package mclub.system

import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class PerfMonInterceptor {
	private static final AtomicLong REQUEST_NUMBER_COUNTER = new AtomicLong();
	private static final String START_TIME_ATTRIBUTE = 'Controller__START_TIME__'
	private static final String REQUEST_NUMBER_ATTRIBUTE = 'Controller__REQUEST_NUMBER__'

	private Logger perfLog = LoggerFactory.getLogger("perf.log");

	PerfMonInterceptor(){
        match(controller:"trackerAPI");
	}

	boolean before() {
		if (perfLog.debugEnabled){
			long start = System.currentTimeMillis()
			long currentRequestNumber = REQUEST_NUMBER_COUNTER.incrementAndGet()

			request[START_TIME_ATTRIBUTE] = start
			request[REQUEST_NUMBER_ATTRIBUTE] = currentRequestNumber

			perfLog.debug "preHandle request #$currentRequestNumber : " +
					"'$request.servletPath'/'$request.forwardURI', " +
					"from $request.remoteHost ($request.remoteAddr) " +
					" at ${new Date()}, Ajax: $request.xhr, controller: $controllerName, " +
					"action: $actionName, params: ${new TreeMap(params)}"
		}
		return true;
	}

	boolean after() {
		if(perfLog.debugEnabled){
			long start = request[START_TIME_ATTRIBUTE]
			long end = System.currentTimeMillis()
			long requestNumber = request[REQUEST_NUMBER_ATTRIBUTE]

			def msg = "postHandle request #$requestNumber: end ${new Date()}, " +
					"controller total time ${end - start}ms"
			if (perfLog.traceEnabled) {
				perfLog.trace msg + "; model: $model"
			}
			else {
				perfLog.debug msg
			}
		}
		return true;
	}

	void afterView() {
		if (!perfLog.debugEnabled) return

		long start = request[START_TIME_ATTRIBUTE]
		long end = System.currentTimeMillis()
		long requestNumber = request[REQUEST_NUMBER_ATTRIBUTE]

        Throwable e = getThrowable();
		def msg = "afterCompletion request #$requestNumber: " +
				"end ${new Date()}, total time ${end - start}ms"
		if (e) {
			perfLog.debug "$msg \n\texception: $e.message", e
		}
		else {
			perfLog.debug msg
		}
	}
}
