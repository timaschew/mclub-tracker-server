/**
 * Project: CarTracServer
 * 
 * File Created at Apr 6, 2013
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

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author shawn
 *
 */
public class DateUtils {
	public static Date today(){
		/*
		Date today = new Date(System.currentTimeMillis() - 2 * 24 * 3600 * 1000);
		return getDayOfTime(today);
		*/
		return getDayOfTime(new Date());
	}
	
	public static Date yesterday(){
		return new java.util.Date(today().getTime() - TIME_OF_ONE_DAY);
	}
	
	public static Date tomorrow(){
		return new java.util.Date(today().getTime() + TIME_OF_ONE_DAY);
	}
	
	// 00:00:00.000 ~ 23:59:59.999
	public static final long TIME_OF_ONE_DAY = 24 * 3600 * 1000 -1; 
	
	public static Calendar getCalendar(){
		return Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
	}
	
	public static Date getDayOfTime(Date time){
		Calendar cal = DateUtils.getCalendar();
		cal.setTime(time);
		int y = cal.get(Calendar.YEAR);
		int m = cal.get(Calendar.MONTH);
		int d = cal.get(Calendar.DATE);
		cal.clear();
		cal.set(Calendar.YEAR, y);
		cal.set(Calendar.MONTH, m);
		cal.set(Calendar.DATE, d);
		return cal.getTime();
	}
	
	public static String prettyTimeString(long elapsed){
		long diffInSeconds = elapsed / 1000;

	    long diff[] = new long[] { 0, 0, 0, 0 };
	    /* sec */diff[3] = (diffInSeconds >= 60 ? diffInSeconds % 60 : diffInSeconds);
	    /* min */diff[2] = (diffInSeconds = (diffInSeconds / 60)) >= 60 ? diffInSeconds % 60 : diffInSeconds;
	    /* hours */diff[1] = (diffInSeconds = (diffInSeconds / 60)) >= 24 ? diffInSeconds % 24 : diffInSeconds;
	    /* days */diff[0] = (diffInSeconds = (diffInSeconds / 24));
	    StringBuilder sb = new StringBuilder();
	    if(diff[0] > 0){
	    	sb.append(String.format("%d天",diff[0]));
	    }
	    if(diff[1] > 0){
	    	sb.append(String.format("%d小时",diff[1]));
	    }
	    sb.append(String.format("%d分钟",diff[2]));
	    return sb.toString();
	}
}
