import mclub.sys.UpgradeService
import mclub.user.UserService

class BootStrap {
    UpgradeService upgradeService;
    UserService userService;
    def init = { servletContext ->
        upgradeService.performUpgrade();
        userService.initUserData();
    }
    def destroy = {
    }
}
