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
 * 规则：连续5个点速度超过60KM，取最大值
 * 重复：距上次执行间隔30分钟
 * 微博：哦哦哦 我在(xx位置) 可以跑到yy KM/H。好爽！好舒畅！
 * @author shawn
 */
class SpeedAlertRule extends AbstractRule{
	public int execute(Map<Object, Object> context) {
		WeiboService weiboService = context['weiboService'];

		// TODO Auto-generated method stub
		log.info("WeiboService ${weiboService}");
		return 0;
	}
}
