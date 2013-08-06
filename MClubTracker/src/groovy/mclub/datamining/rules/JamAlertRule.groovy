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
import javax.annotation.*

import mclub.social.WeiboService


/**
 * 规则：在给定的时间/地点范围内，连续10个点速度<10KM/h的
 * 微博：呀呀呀 XXX 好堵啊，y分钟才开了z米，真想长个翅膀飞过去！
 * 重复：同一个地点，距上次执行间隔30分钟
 *
 * 例如：早上8~10点，钱江一桥南；早上8~11点，动物园；早上8：11点，北山路玉古路口
 * 
 * @author shawn
 */
class JamAlertRule extends AbstractRule{
    public int execute(Map<Object, Object> context) {
		WeiboService weiboService = context['weiboService'];
		
	    // TODO Auto-generated method stub
		log.info("WeiboService ${weiboService}");
	    return 0;
    }
}
