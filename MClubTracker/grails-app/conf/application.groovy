// GPS103 tracker
tracker.gps103.enabled = true
tracker.gps103.port = 5001

// H02 tracker
tracker.h02.enabled = true
tracker.h02.port = 5002

// GT06 tracker
tracker.gt06.enabled = true
tracker.gt06.port = 5003

// TK103 tracker setup
tracker.tk103.enabled = false
tracker.tk103.port = 5004
// Not used configurations
//tracker.tk103.address = 'localhost' - not used
//tracker.tk103.resetDelay = 0

// T55 tracker setup
tracker.t55.enabled = false
tracker.t55.port = 5005

// enables tracker log
tracker.logger.enabled = false
tracker.geocode.enabled = false

// enable APRS receiver
tracker.aprs.enabled = true
tracker.aprs.address = 'rotate.aprs2.net'
tracker.aprs.port = 14580
tracker.aprs.call = 'foo'
tracker.aprs.pass = 'bar'
//small china filter
tracker.aprs.filter = 'r/31.864128/109.930590/1600'

//tracker.aprs.blacklist = 'a,b,c,d,e'

//huge china filter
//tracker.aprs.filter = 'r/36.045101/103.836093/2500'
//tracker.aprs.filter = 'r/30.21/120.15/100' //range in Hangzhou 100KM
//tracker.aprs.filter = 'p/B' //All callsign starts with B
//tracker.aprs.filter = 'p/BG5HHP'

//tracker.aprs.data.daysToPreserve = 7

tracker.minimalPositionUpdateInterval = 5000 // in milliseconds
tracker.maximumShowPositionInterval = 30 * 60 * 1000; // 30 minutes in milliseconds

tracker.map.forceSecure = false;
tracker.map.showLineDots = true;  // by default will show the line dots
tracker.map.amap_api_url = 'http://webapi.amap.com/maps?v=1.3&key=cfce41430c43afbb7bd2cdfab2d9a2ee'; // change me!

// Enable/disable the social feature
social.weibo.enabled = false

// Display site license if exists
// site.license = ""

//==========================================================
// The home directory setup
def homeDir = System.properties.getProperty('mclub.home');
if(homeDir){
    // Using external configurations if home dir specified
    grails.config.locations = ["file:${homeDir}/database.properties"]
    sys.ipdb.filepath = "${homeDir}/data/17monipdb.dat"
}else{
    System.out.println("CONFIG: \"mclub.home\" is not found in system properties, default values applied");
    sys.ipdb.filepath = "~/mclub-tracker/17monipdb.dat"
}
