/**
 * Project: CarTracServer
 * 
 * File Created at Apr 5, 2013
 * $id$
 * 
 * Copyright 2013, Shawn Chain (shawn.chain@gmail.com).
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mclub.util;

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.web.context.ServletContextHolder;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.springframework.context.ApplicationContext;

/**
 * @author shawn
 *
 */
public class GrailsApplicationHolder {
	static ApplicationContext applicationContext = null;
	static GrailsApplication grailsApplication = null;
	
	public static void setApplicationContext(ApplicationContext appCtx){
		applicationContext = appCtx;
	}
	
	public static ApplicationContext getApplicationContext(){
		if(applicationContext == null){
			applicationContext = (ApplicationContext) ServletContextHolder.getServletContext().
	    	        getAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT);
		}
		return applicationContext;
	}
	
	public static GrailsApplication getApplication(){
		if(grailsApplication == null){
			grailsApplication = getApplicationContext().getBean(GrailsApplication.class);			
		}
		return grailsApplication;
	}
//	public static ConfigObject getConfigObject(){
//		return getApplication().getConfig();
//	}
//	
//	public static Object getConfigValue(String key){
//		return getConfigObject().flatten().get(key);
//	}
//	public static Object getConfigValue(ConfigObject config, String key){
//		return config.flatten().get(key);
//	}
//	
//	public static TrackerService getTrackerService(){
//		return getApplicationContext().getBean(TrackerService.class);
//	}
}
