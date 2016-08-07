// locations to search for config files that get merged into the main config;
// config files can be ConfigSlurper scripts, Java properties files, or classes
// in the classpath in ConfigSlurper format

// grails.config.locations = [ "classpath:${appName}-config.properties",
//                             "classpath:${appName}-config.groovy",
//                             "file:${userHome}/.grails/${appName}-config.properties",
//                             "file:${userHome}/.grails/${appName}-config.groovy"]

// if (System.properties["${appName}.config.location"]) {
//    grails.config.locations << "file:" + System.properties["${appName}.config.location"]
// }

grails.project.groupId = appName // change this to alter the default package name and Maven publishing destination
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = false
grails.mime.types = [
    all:           '*/*',
    atom:          'application/atom+xml',
    css:           'text/css',
    csv:           'text/csv',
    form:          'application/x-www-form-urlencoded',
    html:          ['text/html','application/xhtml+xml'],
    js:            'text/javascript',
    json:          ['application/json', 'text/json'],
    multipartForm: 'multipart/form-data',
    rss:           'application/rss+xml',
    text:          'text/plain',
    xml:           ['text/xml', 'application/xml']
]

// URL Mapping Cache Max Size, defaults to 5000
//grails.urlmapping.cache.maxsize = 1000

// What URL patterns should be processed by the resources plugin
grails.resources.adhoc.patterns = ['/images/*', '/css/*', '/js/*', '/plugins/*']
grails.resources.adhoc.excludes = ['**/WEB-INF/**','**/META-INF/**']

grails.assets.minifyOptions.excludes = ["**/*.min.js"]

// The default codec used to encode data with ${}
grails.views.default.codec = "html" // none, html, base64
grails.views.gsp.encoding = "UTF-8"
grails.converters.encoding = "UTF-8"
// enable Sitemesh preprocessing of GSP pages
grails.views.gsp.sitemesh.preprocess = true
// scaffolding templates configuration
grails.scaffolding.templates.domainSuffix = 'Instance'

// Set to false to use the new Grails 1.2 JSONBuilder in the render method
grails.json.legacy.builder = false
// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
// packages to include in Spring bean scanning
grails.spring.bean.packages = []
// whether to disable processing of multi part requests
grails.web.disable.multipart=false

// request parameters to mask when logging exceptions
grails.exceptionresolver.params.exclude = ['password']

// configure auto-caching of queries by default (if false you can cache individual queries with 'cache: true')
grails.hibernate.cache.queries = false


environments {
    development {
        grails.logging.jul.usebridge = true
    }
    production {
        grails.logging.jul.usebridge = false
        grails.serverURL = "https://aprs.hamclub.net/mtracker"
    }
}

// log4j configuration
log4j = {
    // Example of changing the log pattern for the default console appender:
    //
    //appenders {
    //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    //}
	appenders {
		rollingFile name: 'aprsAppender',
					maxFileSize: 10240000,
					layout:pattern(conversionPattern: '%d{yyyy-MM-dd/HH:mm:ss.SSS} - %m%n'),
					file: '/tmp/mclub_aprs.log'
					
		rollingFile name: 'perfAppender',
					maxFileSize: 10240000,
					layout:pattern(conversionPattern: '%d{yyyy-MM-dd/HH:mm:ss.SSS} - %m%n'),
					file: '/tmp/mclub_perf.log'
					
		rollingFile name: 'gpsAppender',
					maxFileSize: 10240000,
					layout:pattern(conversionPattern: '%d{yyyy-MM-dd/HH:mm:ss.SSS} - %m%n'),
					file: '/tmp/mclub_gps.log'

        rollingFile name: 'talkboxAppender',
                maxFileSize: 10240000,
                layout:pattern(conversionPattern: '%d{yyyy-MM-dd/HH:mm:ss.SSS} - %m%n'),
                file: '/tmp/mclub_talkbox.log'
	}
	

    error  'org.codehaus.groovy.grails.web.servlet',        // controllers
           'org.codehaus.groovy.grails.web.pages',          // GSP
           'org.codehaus.groovy.grails.web.sitemesh',       // layouts
           'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
           'org.codehaus.groovy.grails.web.mapping',        // URL mapping
           'org.codehaus.groovy.grails.commons',            // core / classloading
           'org.codehaus.groovy.grails.plugins',            // plugins
           'org.codehaus.groovy.grails.orm.hibernate',      // hibernate integration
           'org.springframework',
           'org.hibernate',
           'net.sf.ehcache.hibernate'
	
	trace  aprsAppender: 'aprs.log',
		   additivity:false
		   
	debug  perfAppender: 'perf.log',
		   additivity:false
		   
	debug  gpsAppender: 'gps.log',
		   additivity:false

    info   talkboxAppender: 'talkbox.log',
           additivity:false
	/*
	debug 'mclub.tracker.LivePositionWebsocketServer',
		  'mclub.tracker.TalkBoxWebsocketServer',
	      'mclub.tracker.protocol.Gt06TrackerServer'
	*/

	environments {
		development {
			info   'grails.app'
			info	'mclub.tracker',
				   	'mclub.datamining'
			//debug  'org.hibernate.SQL'
			debug  'mclub.user',
				   'mclub.tracker.TrackerService'
		}
		production {
			info   'grails.app',
			'mclub.tracker',
			'mclub.datamining'
		}
    }
}

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
tracker.map.amapApiUrl = 'http://webapi.amap.com/maps?v=1.3&key=cfce41430c43afbb7bd2cdfab2d9a2ee'; // change me!

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

// Uncomment and edit the following lines to start using Grails encoding & escaping improvements
// See https://github.com/grails/grails-core/wiki/Default-Codecs for more details
/* remove this line
// GSP settings
grails {
    views {
        gsp {
            encoding = 'UTF-8'
            htmlcodec = 'xml' // use xml escaping instead of HTML4 escaping
            codecs {
                expression = 'html' // escapes values inside null
                scriptlet = 'none' // escapes output from scriptlets in GSPs
                taglib = 'none' // escapes output from taglibs
                staticparts = 'none' // escapes output from static template parts
            }
        }
        // escapes all not-encoded output at final stage of outputting
        filteringCodecForContentType {
            //'text/html' = 'html'
        }
    }
}
remove this line */