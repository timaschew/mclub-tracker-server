package mclub

import mclub.social.WeiboService


class RefreshTokenJob {
    static triggers = {
		cron name: 'refreshTokenTrigger', cronExpression: "0 0 6 * * ?" // refresh token every day at 6:00 am
    }

	def concurrent = false;
	def sessionRequired = false // we have a global session for each quartz worker thread
	
	WeiboService weiboService;
	
    def execute() {
        // execute job
		weiboService.refreshTokens();
    }
}
