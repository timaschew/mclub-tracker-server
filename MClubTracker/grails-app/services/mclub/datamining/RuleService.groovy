package mclub.datamining

import mclub.datamining.rules.*
import mclub.social.WeiboService
import mclub.tracker.TrackerService
import mclub.tracker.geocode.GoogleReverseGeocoder

class RuleService {
	List<Rule> rules = [];
	WeiboService weiboService;
	TrackerService trackerService;
	
	//FIXME - handle device ids
	String deviceId = '353451048729261';
	
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
//		rules << new JamAlertRule();
		rules << new SpeedAlertRule();
		rules << new AutoTrackerGenerateRule();
	}

	// Run the rules, which is triggered by a timer
	def runRules(){
		log.debug("Running ${rules.size()} rules...");
		def context = [
			weiboService:weiboService,
			addressResolver:addressResolver,
			trackerService:trackerService,
			deviceId:deviceId
		];
	
		for(Rule rule in rules){
			try{
				rule.execute(context);
			}catch(Exception e){
				log.warn("Error executing rule ${rule.name}",e);
			}
			
		}
	}
}






