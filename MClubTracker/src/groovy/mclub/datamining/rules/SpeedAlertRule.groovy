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

import java.text.DecimalFormat

import mclub.social.WeiboService
import mclub.tracker.TrackerDevice
import mclub.tracker.TrackerPosition
import mclub.tracker.geocode.ReverseGeocoder

/**
 * 规则：当前的10个点，如果最早点的时间不超过10分钟，则认为有效。计算10个点的平均速度，如果速度 < 5，则认为是堵车，30~40则认为正常，40 ~ 60 认为较快， >60则认为很快
 * 重复：距上次执行间隔15分钟
 * 微博：哦哦哦 我在(xx位置) 可以跑到yy KM/H。好爽！好舒畅！
 * @author shawn
 */
class SpeedAlertRule extends AbstractRule{
	public int execute(Map<Object, Object> context) {
		WeiboService weiboService = context['weiboService'];
		String deviceId = context['deviceId'];
		ReverseGeocoder addressResolver = context['addressResolver'];
		
		// check last execution time
		def state = loadState(deviceId)
		Long lets = state[lastExcutionTimeStamp];
		if(lets && ((lets + 15 * 60 * 1000) > System.currentTimeMillis())){
			// executes at least every 15mins
			return 0;
		}
		
		TrackerDevice dev = TrackerDevice.findByUdid(deviceId);
		if(!dev){
			log.info("No device found for ${deviceId}");
			return 0;
		}
		
		// find max 10 positions in time desc order
		def points = TrackerPosition.findAll("FROM TrackerPosition tp WHERE tp.deviceId=:did ORDER BY tp.time DESC",[did:dev.id,max:10]);
		if(points?.size() < 10){
			log.info("not enough points to check the speed");
			return 0;
		}
		
		// the first point should be in 10mins
		TrackerPosition p0 = points[0];
		TrackerPosition p9 = points[9];
		if(System.currentTimeMillis() - p9.time.time > 10 * 60 * 1000 ){
			log.info("points are expired: ${p9.time}");
			return 0;
		}
		
		// calculate the average speed
		double avgSpeed = 0;
		for(TrackerPosition p : points){
			avgSpeed += p.speed;
		} 
		avgSpeed = avgSpeed / points.size();
		
		// convert from knot to kmh
		avgSpeed = avgSpeed * 1.852;
		String avgSpeedStr = new DecimalFormat("0.00").format(avgSpeed);
//		String location = addressResolver.getAddress(p0.latitude,p0.longitude);
		
		log.info("Average speed: ${(avgSpeed)}");
		String spd = "不正常";
		if(avgSpeed < 5){
			spd = "堵在路上啦";
		}else if(avgSpeed < 40){
			spd = "路况不错，车速正常(${avgSpeedStr}km/h)"
		}else if(avgSpeed < 60){
			spd = "路况很好，车速挺快哒(${avgSpeedStr}km/h)"
		}else if(avgSpeed < 120){
			spd = "路上没车，嗖嗖开的飞快(${avgSpeedStr}km/h)"
		}else{
			spd = "呃...主人你以前是开飞机么？开太快啦！"
		}
		String msg = "${spd}."
		
		if(weiboService.postStatus(deviceId, msg, p0.latitude,p0.longitude)){
			// post success, so update the rule execution timestamp
			this.update(deviceId, lastExcutionTimeStamp, System.currentTimeMillis());
			this.commit(deviceId);
		}
		
		return 0;
	}
}
