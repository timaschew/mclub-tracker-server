/**
 * Project: MClubTracker
 * 
 * File Created at 2013-8-6
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
package mclub.datamining.rules

import mclub.social.WeiboService
import mclub.tracker.TrackerDevice
import mclub.tracker.TrackerPosition
import mclub.util.DateUtils

/**
 * 每天接收到的第一个有效的GPS点，说明已经开机
 * 微博：啦啦啦，我醒啦(+位置)。主人要安全上路哦。
 * 重复：每天一次
 * @author shawn
 *
 */
public class DailyGreetingRule extends AbstractRule{
	public int execute(Map<Object, Object> context) {
		WeiboService weiboService = context['weiboService'];
		String deviceId = context['deviceId'];
		
		// get last execute time stamp
		def state = loadState(deviceId);
		Long lets = state[lastExcutionTimeStamp];
		if(lets && lets >= DateUtils.today().getTime()){
			// it's executed today
			return 0;
		}
		
		// load the GPS position
		TrackerDevice dev = TrackerDevice.findByUdid(deviceId);
		if(!dev){
			// no such device, bail out
			return 0;
		}
		
		TrackerPosition pos = TrackerPosition.findByDeviceIdAndTimeGreaterThan(dev.id,DateUtils.today());
		if(pos){
			String addr = null; //addressResolver.getAddress(pos.latitude, pos.longitude);
			String msg = "嘀嘀嘀,开始工作啦！引擎转速/水温/油量 一切正常. 主人要安全驾驶哦.";
			if(addr){
				msg += " (${addr})"
			}
			if(weiboService.postStatus(deviceId, msg)){
				this.update(deviceId, lastExcutionTimeStamp, System.currentTimeMillis());
				this.commit(deviceId);
			}
		}else{
			log.info("No position found for today: ${DateUtils.today()}");
		}
		return 0;
	}
}