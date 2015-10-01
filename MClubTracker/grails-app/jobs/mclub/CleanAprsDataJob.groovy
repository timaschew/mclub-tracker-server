package mclub

import mclub.sys.*;
import mclub.user.*;
import mclub.tracker.*;

class CleanAprsDataJob {
	static triggers = {
		cron name: 'cleanAprsDataTrigger', cronExpression: "0 0 5 * * ?" // refresh token every day at 5:00AM
		//cron name: 'cleanAprsDataTrigger', cronExpression: "0 */1 * * * ?" // TEST - every 5 minutes.
	}

	def concurrent = false;
	def sessionRequired = true // we have a global session for each quartz worker thread

	ConfigService configService;
	TrackerDataService trackerDataService;

	def execute() {
		Integer daysToPreserve = configService.getConfigInt("tracker.aprs.data.daysToPreserve");
		if(!daysToPreserve){
			daysToPreserve = 3; // by default will keep 7 days data
		}
		trackerDataService.deleteAprsPosition(daysToPreserve);
	}
}
