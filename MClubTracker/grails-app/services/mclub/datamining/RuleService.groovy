package mclub.datamining

import mclub.social.WeiboService
import mclub.tracker.TrackerDevice
import mclub.tracker.TrackerPosition
import mclub.tracker.TrackerService
import mclub.tracker.geocode.GoogleReverseGeocoder
import mclub.util.DateUtils

class RuleService {
	List<Rule> rules = [];
	WeiboService weiboService;
	TrackerService trackerService;
	
	GoogleReverseGeocoder addressResolver = new GoogleReverseGeocoder();
	
	@javax.annotation.PostConstruct
	def start(){
		loadRules();
	}
	
	@javax.annotation.PreDestroy
	def stop(){
		rules = [];
	}
	
	private def loadRules(){
		// Rule that detects the wake up
		//rules << new CountingRule();
		rules << new WakeUpRule();
		/*
		rules << new AutoTrackRule();
		*/
	}

	// Run the rules, which is triggered by a timer
	def runRules(){
		log.debug("Running ${rules.size()} rules...");
		for(Rule rule in rules){
			rule.execute([:]);
		}
	}
	
	/**
	 * 每天接收到的第一个有效的GPS点，说明已经开机
	 * 微博：啦啦啦，我醒啦(+位置)。主人要安全上路哦。
	 * 重复：每天一次
	 * @author shawn
	 *
	 */
	class WakeUpRule extends AbstractRule{
		public static final String lastExcutionTimeStamp = "LastExecutionTimeStamp";
		public static final String deviceId = "353451048729261";
		
		public String getName() {
			return "WakeUpRule";
		}
		public int execute(Map<Object, Object> params) {
			// get last execute time stamp
			def state = loadState();
			Long lets = state[lastExcutionTimeStamp];
			if(lets && lets > DateUtils.today().getTime() && lets < DateUtils.tomorrow().getTime()){
				// it's executed today
				return 0;
			}
			
			// load the GPS position
			TrackerDevice dev = TrackerDevice.findByUdid(deviceId);
			if(!dev){
				// no such device, bail out
				return 0;
			}
			TrackerPosition pos = TrackerPosition.find("FROM TrackerPosition tp WHERE deviceId=:did AND tp.time > :yesterday AND tp.time < :tomorrow",
				[did:dev.id,yesterday:DateUtils.yesterday(),tomorrow:DateUtils.tomorrow()]);
			if(pos){
				String addr = null; //addressResolver.getAddress(pos.latitude, pos.longitude);
				String msg = "嘀嘀嘀,引擎发动. 转速:1000,水温:75,油量:50. 准备上路! 主人要安全驾驶哦.";
				if(addr){
					msg += " (${addr})"
				}
				if(weiboService.postStatus(deviceId, msg)){
					this.update(lastExcutionTimeStamp, System.currentTimeMillis());
					this.commit();
				}
			}
			return 0;
		}
	}
}

class CountingRule extends AbstractRule{
	public String getName(){
		return "CountingRule"
	}

	/*
	 * 	
	 */
	public int execute(Map<Object,Object> params){
		int count = 0;//this.incrementExecutionCountToday();
		log.info("${getName()} count: ${count}");
		return 0;
	}
}



/**
 * 规则：在给定的时间/地点范围内，连续10个点速度<10KM/h的
 * 微博：呀呀呀 XXX 好堵啊，y分钟才开了z米，真想长个翅膀飞过去！
 * 重复：同一个地点，距上次执行间隔30分钟
 *
 * 例如：早上8~10点，钱江一桥南；早上8~11点，动物园；早上8：11点，北山路玉古路口
 */
class JamAlertRule implements Rule{
	public String getName(){
		return "JamAlertRule";
	}
	public int execute(Map<Object, Object> params) {
		return 0;
	}
}

/**
 * 规则：连续5个点速度超过60KM，取最大值
 * 重复：距上次执行间隔30分钟
 * 微博：哦哦哦 我在(xx位置) 可以跑到yy KM/H。好爽！好舒畅！
 * @author shawn
 */
class SpeedRule implements Rule{
	public String getName() {
		return "SpeedRule";
	}
	public int execute(Map<Object, Object> params) {
		return 0;
	}
}

/**
 * 自动轨迹生成
 * 规则：上3个点位置接近，速度趋0。无新节点上报距当下时间超过30分钟，或者距下一个节点的超过30分钟。
 * 微博：当当当 这趟我跑了xx公里，用了yy分钟，平均速度为zz KM/H. (好快好快|好慢好慢|一般一般！)
 * 重复：无限制
 */
class AutoTrackRule implements Rule{
	public String getName() {
		return "AutoTrackRule";
	}
	public int execute(Map<Object, Object> params) {
		return 0;
	}
}