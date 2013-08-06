package mclub.datamining

import mclub.datamining.rules.*
import mclub.social.WeiboService
import mclub.tracker.TrackerService
import mclub.tracker.geocode.GoogleReverseGeocoder

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
	
	/**
	 * Manually load the rules
	 * @return
	 */
	private def loadRules(){
		// Rule that detects the wake up
		rules << new DailyGreetingRule();
		rules << new IdleRule();
		rules << new JamAlertRule();
		rules << new SpeedAlertRule();
		rules << new AutoTrackerGenerateRule();
	}

	// Run the rules, which is triggered by a timer
	def runRules(){
		log.debug("Running ${rules.size()} rules...");
		def context = [
			weiboService:weiboService,
			addressResolver:addressResolver,
			trackerService:trackerService
		];
	
		for(Rule rule in rules){
			try{
				rule.execute(context);
			}catch(Exception e){
				log.warn("Error executing rule ${rule.name}",e);
			}
			
		}
	}
	
//	/**
//	 * 每天接收到的第一个有效的GPS点，说明已经开机
//	 * 微博：啦啦啦，我醒啦(+位置)。主人要安全上路哦。
//	 * 重复：每天一次
//	 * @author shawn
//	 *
//	 */
//	class WakeUpRule extends AbstractRule{
//		
//		public String getName() {
//			return "WakeUpRule";
//		}
//		public int execute(Map<Object, Object> params) {
//			// get last execute time stamp
//			def state = loadState();
//			Long lets = state[lastExcutionTimeStamp];
//			if(lets && lets >= DateUtils.today().getTime()){
//				// it's executed today
//				return 0;
//			}
//			
//			// load the GPS position
//			TrackerDevice dev = TrackerDevice.findByUdid(deviceId);
//			if(!dev){
//				// no such device, bail out
//				return 0;
//			}
//			
//			log.info("${DateUtils.yesterday()}");
//			log.info("${DateUtils.today()}");
//			log.info("${DateUtils.tomorrow()}");
//			
//			TrackerPosition pos = TrackerPosition.find("FROM TrackerPosition tp WHERE tp.deviceId=:did AND tp.time>:yesterday AND tp.time<:tomorrow",
//				[did:dev.id,yesterday:DateUtils.yesterday(),tomorrow:DateUtils.tomorrow()]);
//			if(pos){
//				String addr = null; //addressResolver.getAddress(pos.latitude, pos.longitude);
//				String msg = "嘀嘀嘀,准备出发！引擎转速:1000,水温:75,油量:50,一切正常. 主人要安全驾驶哦.";
//				if(addr){
//					msg += " (${addr})"
//				}
//				if(weiboService.postStatus(deviceId, msg)){
//					this.update(lastExcutionTimeStamp, System.currentTimeMillis());
//					this.commit();
//				}
//			}else{
//				log.info("No position found for today: ${DateUtils.today()}");
//			}
//			return 0;
//		}
//	}
	
	
//	/**
//	 * 如果没有任何位置信息，超过14点以后，每天卖萌一次
//	 * 微博：哎呀都下午了,主人还没出现。伦家好无聊啊...好想出去溜达一圈~~~
//	 * 重复：14点以后，每天一次，
//	 * @author shawn
//	 *
//	 */
//	class IdleRule extends AbstractRule{
//		public String getName() {
//			return "IdleRule";
//		}
//		public int execute(Map<Object, Object> params) {
//			// get last execute time stamp
//			def state = loadState();
//			Long lets = state[lastExcutionTimeStamp];
//			if(lets && lets > DateUtils.today().getTime()){
//				// it's executed today
//				return 0;
//			}
//			
//			// load the GPS position
//			TrackerDevice dev = TrackerDevice.findByUdid(deviceId);
//			if(!dev){
//				// no such device, bail out
//				return 0;
//			}
//			TrackerPosition pos = TrackerPosition.find("FROM TrackerPosition tp WHERE deviceId=:did AND tp.time>:yesterday AND tp.time<:tomorrow",
//				[did:dev.id,yesterday:DateUtils.yesterday(),tomorrow:DateUtils.tomorrow()]);
//			if(!pos){
//				// if time is > 14:00
//				if(System.currentTimeMillis() - DateUtils.today().getTime() > (14 * 3600 * 1000)){
//					String msg = "哎呀都下午了,主人还没出现。伦家好无聊啊...好想出去溜达一圈~~~"
//					if(weiboService.postStatus(deviceId, msg)){
//						this.update(lastExcutionTimeStamp, System.currentTimeMillis());
//						this.commit();
//					}
//				}
//			}
//			return 0;
//		}
//	}
}






