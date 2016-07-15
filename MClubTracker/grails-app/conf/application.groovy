tracker.maximumShowPositionInterval = 30 * 60 * 1000; // 30 minutes in milliseconds

// Enable/disable the social feature
social.weibo.enabled = false

// Display site license if exists
// site.license = ""

//==========================================================
// The home directory setup
def homeDir = System.properties.getProperty('mclub.home');
if(homeDir){
    // Using external configurations if home dir specified
    sys.ipdb.filepath = "${homeDir}/data/17monipdb.dat"
}else{
    sys.ipdb.filepath = "~/mclub-tracker/17monipdb.dat"
}