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

import java.text.SimpleDateFormat
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
			TrackerTrack latestTrack = TrackerTrack.find("FROM TrackerTrack tt WHERE tt.deviceId=:did ORDER BY endDate DESC",[did:dev.id]);
			if(latestTrack){
				startTime = latestTrack.endDate;
			}else{
				startTime = DateUtils.today(); // start from today
			}
		}
		
		// count all 'un-tracked' positions
		def untrackedPositionCount = TrackerPosition.countByDeviceIdAndTimeGreaterThanEquals(dev.id,startTime);
		if(untrackedPositionCount < 5){
			// not enough positions to start a track
			log.info("No positions to track");
			return 0;
		}
		
		// load first/last positions
		def query = "FROM TrackerPosition tp WHERE tp.deviceId=:did AND tp.time>:startTime ORDER BY tp.time"
		TrackerPosition p1 = TrackerPosition.find(query,[did:dev.id,startTime:startTime]);
		TrackerPosition p2 = TrackerPosition.find(query + " DESC",[did:dev.id,startTime:startTime]);
		log.info(p1);
		log.info(p2);
		
		// create active track if not found
		if(!activeTrack){
			activeTrack = new TrackerTrack();
			activeTrack.deviceId = dev.id;
			activeTrack.title = "Active track";
			activeTrack.beginDate = p1.time;
			activeTrack.endDate = p2.time;
			activeTrack.type = 1;
		}else{
			activeTrack.endDate = p2.time;
		}
		if(!activeTrack.save(flush:true)){
			log.error("Error saving active track: ${activeTrack.errors}");
			return 0;
		}
		
		// no updates in 30m
		if(System.currentTimeMillis() -  p2.time.time > 0.5 * 3600 * 1000 ){
			// update the track position
			activeTrack.type = 0;
			activeTrack.title = "Track ${activeTrack.beginDate}";
			if(!activeTrack.save(flush:true)){
				log.error("Error saving active track: ${activeTrack.errors}");
				return 0;
			}
			
			// load all positions for that track
			def all = TrackerPosition.findAllByDeviceIdAndTimeBetween(dev.id,activeTrack.beginDate,activeTrack.endDate);
			double totalDistance;
			TrackerPosition lastP = null;
			for(TrackerPosition p : all){
				if(lastP){
					totalDistance += LocationUtils.distance(lastP.latitude, lastP.longitude, p.latitude, p.longitude);
				}
				lastP = p;
			}
			
			long totalDistanceKM = (long)(totalDistance / 1000f);
			long totalTimeMs = (activeTrack.endDate.time - activeTrack.beginDate.time);
			String totalTimeString = DateUtils.prettyTimeString(totalTimeMs);//totalTimeMs / 1000 / 60;
			double avgSpeedKMH = (totalDistance / (double)(totalTimeMs / 1000)) * 3.6f; // km/h
			
			String mood;
			if(avgSpeedKMH < 25){
				mood = "好慢好慢";
			}else if(avgSpeedKMH < 45){
				mood = "一般一般";
			}else{
				mood = "好快好快";
			}
			
			String msg = "当当当 这趟我跑了${totalDistanceKM}公里，耗时${totalTimeString}，平均时速 ${avgSpeedKMH}km/h. ${mood}";
			log.info();
			if(weiboService.postStatus(deviceId, msg)){
				// save last update timestamp
				this.update(deviceId, lastExcutionTimeStamp, System.currentTimeMillis());
				this.commit(deviceId);
			}
			
		}
		
		return 0;
	}
}
