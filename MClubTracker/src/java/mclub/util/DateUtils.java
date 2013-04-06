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

/**
 * @author shawn
 *
 */
public class DateUtils {
	public static Date today(){
		Calendar cal1 = Calendar.getInstance();
		cal1.set(Calendar.HOUR,0);
		cal1.set(Calendar.MINUTE,0);
		cal1.set(Calendar.SECOND,0);
		cal1.set(Calendar.MILLISECOND,0);
		return cal1.getTime();
	}
	
	// 00:00:00.000 ~ 23:59:59.999
	public static final long TIME_OF_ONE_DAY = 24 * 3600 * 1000 -1; 
}
