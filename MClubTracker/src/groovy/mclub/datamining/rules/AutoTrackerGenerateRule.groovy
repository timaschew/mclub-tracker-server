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
import mclub.tracker.TrackerTrack
import mclub.tracker.geocode.ReverseGeocoder
import mclub.util.DateUtils
import mclub.util.LocationUtils

/**
 * 自动轨迹生成
 * 规则：自从上次轨迹结束后，存在6个节点，且最后一个节点的时间 距离当前时间超过30分钟，则创建新轨迹。       //上3个点位置接近，速度趋0。无新节点上报距当下时间超过30分钟，或者距下一个节点的超过30分钟。
 * 微博：当当当 这趟我跑了xx公里，用了yy分钟，平均速度为zz KM/H. (好快好快|好慢好慢|一般一般！)
 * 重复：无限制
 */
class AutoTrackerGenerateRule extends AbstractRule{
	
	private static final long HALF_HOUR = 0.5 * 3600 * 1000;
	
	public int execute(Map<Object, Object> context) {
		WeiboService weiboService = context['weiboService'];
		String deviceId = context['deviceId'];
		ReverseGeocoder addressResolver = context['addressResolver'];
		
		// get device id
		def dev = TrackerDevice.findByUdid(deviceId);
		if(!dev){
			// no such device
			log.info("Device ${deviceId} not found");
			return 0;
		}
		
		// get track start time
		// 最近一条记录的结束时间，可以作为接下来记录的查询时间点，实际开始时间则以第一个Pos的时间为准。
		def startTime = null;
		TrackerTrack activeTrack = TrackerTrack.findByDeviceIdAndType(dev.id,1);
		if(activeTrack){
			startTime = activeTrack.endDate;
		}else{
			TrackerTrack latestTrack = TrackerTrack.find("FROM TrackerTrack tt WHERE tt.deviceId=:dev ORDER BY endDate DESC",[dev:dev.id]);
			if(latestTrack){
				startTime = latestTrack.endDate;
			}else{
				startTime = DateUtils.today(); // start from today
			}
		}
		// count all 'un-tracked' positions
		def untrackedPositionCount = TrackerPosition.countByDeviceAndTimeGreaterThan(dev,startTime);
		
		// we have untracked positions!
		if(untrackedPositionCount > 0){
			// only start an active track when 5 or more positions collected
			if(!activeTrack && untrackedPositionCount < 5){
				log.info("No enough positions (${untrackedPositionCount})to start a new track");
				return 0;
			}
	
			// create or update the active track
			TrackerPosition p1,p2;
			// load first/last positions
			def query = "FROM TrackerPosition tp WHERE tp.device=:dev AND tp.time>:startTime ORDER BY tp.time"
			p1 = TrackerPosition.find(query,[dev:dev,startTime:startTime]);
			p2 = TrackerPosition.find(query + " DESC",[dev:dev,startTime:startTime]);
			
			log.info("${p1} - ${p2}");
			if(!activeTrack){
				// create new active track. note - we at least have 5 positions here
				activeTrack = new TrackerTrack();
				activeTrack.deviceId = dev.id;
				activeTrack.title = "Active track";
				activeTrack.beginDate = p1.time;
				activeTrack.endDate = p2.time;
				activeTrack.type = 1;
			}else{
				// update/extending the existing active track. note - we at least have 1 positions here
				activeTrack.endDate = p2.time;
			}
			if(!activeTrack.save(flush:true)){
				log.error("Error saving active track: ${activeTrack.errors}");
				return 0;
			}
		}
		
		// finish track if last position is occurred half hour ago
		if(activeTrack && System.currentTimeMillis() - activeTrack.endDate.time > HALF_HOUR){
			// update the track position
			activeTrack.type = 0;
			activeTrack.title = "Track ${activeTrack.beginDate}";
			if(!activeTrack.save(flush:true)){
				log.error("Error saving active track: ${activeTrack.errors}");
				return 0;
			}
			log.info("Finished ${activeTrack.title}");
			
			// ==========================================================
			// prepare the weibo message
			// load all positions for that track
			def all = TrackerPosition.findAllByDeviceAndTimeBetween(dev,activeTrack.beginDate,activeTrack.endDate);
			double totalDistance;
			double maxSpeed;
			TrackerPosition lastP = null;
			for(TrackerPosition p : all){
				if(p.speed > maxSpeed){
					maxSpeed = p.speed;
				}
				if(lastP){
					totalDistance += LocationUtils.distance(lastP.latitude, lastP.longitude, p.latitude, p.longitude);
				}
				lastP = p;
			}
			
			long totalDistanceKM = (long)(totalDistance / 1000f);
			long totalTimeMs = (activeTrack.endDate.time - activeTrack.beginDate.time);
			String totalTimeString = DateUtils.prettyTimeString(totalTimeMs);//totalTimeMs / 1000 / 60;
			double avgSpeedKMH = (totalDistance / (double)(totalTimeMs / 1000)) * 3.6f; // km/h
			String avgSpdStr = new DecimalFormat("0.00").format(avgSpeedKMH);
			
			double maxSpeedKMH = maxSpeed * 1.852;
			String maxSpdStr = new DecimalFormat("0.00").format(maxSpeedKMH);
						
			String mood = "正常";
			if(avgSpeedKMH < 5){
				mood = "这路堵得没法开啊！";
			}else if(avgSpeedKMH < 20){
				mood = "路上挺堵的！";
			}else if(avgSpeedKMH < 30){
				mood = "基本不堵！";
			}else if(avgSpeedKMH < 40){
				mood = "一路畅通！"
			}else if(avgSpeedKMH < 60){
				mood = "开得飞快！"
			}else if(avgSpeedKMH < 80){
				mood = "高速高速！"
			}else if(avgSpeedKMH < 120){
				mood = "感觉在飙车！"
			}else{
				mood = "呃...我其实是架飞机！"
			}
			
			String msg = "当当当 这趟我跑了${totalDistanceKM}公里,耗时${totalTimeString}.最高${maxSpdStr}km/h,平均${avgSpdStr}km/h. ${mood}";
			if(weiboService.postStatus(deviceId, msg)){
				// save last update timestamp
				this.update(deviceId, lastExcutionTimeStamp, System.currentTimeMillis());
				this.commit(deviceId);
			}
		}
				
		return 0;
	}
}
