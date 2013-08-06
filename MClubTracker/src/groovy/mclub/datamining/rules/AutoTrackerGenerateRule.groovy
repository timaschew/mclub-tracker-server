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

/**
 * 自动轨迹生成
 * 规则：上3个点位置接近，速度趋0。无新节点上报距当下时间超过30分钟，或者距下一个节点的超过30分钟。
 * 微博：当当当 这趟我跑了xx公里，用了yy分钟，平均速度为zz KM/H. (好快好快|好慢好慢|一般一般！)
 * 重复：无限制
 */
class AutoTrackerGenerateRule extends AbstractRule{
	public int execute(Map<Object, Object> context) {
		WeiboService weiboService = context['weiboService'];
		String deviceId = context['deviceId'];
		
		// TODO Auto-generated method stub
		log.info("WeiboService ${weiboService}");
		return 0;
	}
}
