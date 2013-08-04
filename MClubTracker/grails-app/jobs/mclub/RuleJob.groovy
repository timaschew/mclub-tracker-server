package mclub

import mclub.datamining.RuleService



class RuleJob {
    static triggers = {
      simple repeatInterval: 5000l, startDelay:5000l // execute job once in 5 seconds
    }

	def concurrent = false;
	def sessionRequired = true // we have a global session for each quartz worker thread
	
	RuleService ruleService;
	
    def execute() {
        // execute job
		ruleService.runRules();
    }
}
