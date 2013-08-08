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
 * 如果没有任何位置信息，超过14点以后，每天卖萌一次
 * 微博：哎呀都下午了,主人还没出现。伦家好无聊啊...好想出去溜达一圈~~~
 * 重复：14点以后，每天一次，
 * @author shawn
 *
 */
class IdleRule extends AbstractRule{

	public int execute(Map<Object, Object> context) {
		WeiboService weiboService = context['weiboService'];
		String deviceId = context['deviceId'];
		
		// get last execute time stamp
		def state = loadState(deviceId);
		Long lets = state[lastExcutionTimeStamp];
		if(lets && lets > DateUtils.today().getTime()){
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
		if(!pos){
			// if time is > 14:00
			if(System.currentTimeMillis() - DateUtils.today().getTime() > (14 * 3600 * 1000)){
				String msg = "哎呀都到下午了,主人还没出现。伦家好无聊啊...好想出去溜达一圈~~~"
				if(weiboService.postStatus(deviceId, msg)){
					this.update(deviceId,lastExcutionTimeStamp, System.currentTimeMillis());
					this.commit(deviceId);
				}
			}
		}
		return 0;
	}
}