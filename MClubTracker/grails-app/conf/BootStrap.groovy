import mclub.sys.UpgradeService

class BootStrap {
	UpgradeService upgradeService;
    def init = { servletContext ->
		upgradeService.performUpgrade();
    }
	
    def destroy = {
    }
}
