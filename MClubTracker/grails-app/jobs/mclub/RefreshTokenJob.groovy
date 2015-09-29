package mclub

import mclub.social.WeiboService


class RefreshTokenJob {
    static triggers = {
		cron name: 'refreshTokenTrigger', cronExpression: "0 0 6,18 * * ?" // refresh token every day at 6:00 and 18:00
    }

	def concurrent = false;
	def sessionRequired = true // we have a global session for each quartz worker thread
	
	WeiboService weiboService;
	
    def execute() {
        // execute job
		if(weiboService.isEnabled())
			weiboService.refreshTokens();
    }
}
