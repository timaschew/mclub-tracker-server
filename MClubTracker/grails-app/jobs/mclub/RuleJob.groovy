package mclub

import mclub.datamining.RuleService



class RuleJob {
    static triggers = {
      simple repeatInterval: 300000l, startDelay:5000l // execute job once in 5 minutes
    }

	def concurrent = false;
	def sessionRequired = true // we have a global session for each quartz worker thread
	
	RuleService ruleService;
	
    def execute() {
        // execute job
		ruleService.runRules();
    }
}
