grails.servlet.version = "2.5" // Change depending on target container compliance (2.5 or 3.0)
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.work.dir = "target/work"
grails.project.target.level = 1.6
grails.project.source.level = 1.6
//grails.project.war.file = "target/${appName}-${appVersion}.war"

grails.project.fork = [
    // configure settings for compilation JVM, note that if you alter the Groovy version forked compilation is required
    //  compile: [maxMemory: 256, minMemory: 64, debug: false, maxPerm: 256, daemon:true],

    // configure settings for the test-app JVM, uses the daemon by default
    test: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, daemon:true],
    // configure settings for the run-app JVM
    run: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, forkReserve:false],
    // configure settings for the run-war JVM
    war: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, forkReserve:false],
    // configure settings for the Console UI JVM
    console: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256]
]

grails.project.dependency.resolver = "maven" // or ivy
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // specify dependency exclusions here; for example, uncomment this to disable ehcache:
        // excludes 'ehcache'
    }
    log "error" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    checksums true // Whether to verify checksums on resolve
    legacyResolve false // whether to do a secondary resolve on plugin installation, not advised and here for backwards compatibility

    repositories {
        inherits true // Whether to inherit repository definitions from plugins

        grailsPlugins()
        grailsHome()
		mavenLocal()
        grailsCentral()
        mavenCentral()

        // uncomment these (or add new ones) to enable remote dependency resolution from public Maven repositories
        //mavenRepo "http://snapshots.repository.codehaus.org"
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
    }

    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes e.g.

		// enable if use sqlite
		// runtime 'org.xerial:sqlite-jdbc:3.7.2'
        // runtime 'mysql:mysql-connector-java:5.1.22'
		
		compile 'io.netty:netty:3.6.3.Final'
		
		/*
		compile ('commons-httpclient:commons-httpclient:3.1'){
			excludes "commons-codec","commons-logging"
		}
		*/
		compile 'org.htmlparser:htmlparser:1.6'
		compile 'com.alibaba:fastjson:1.2.6'
		//test 'org.spockframework:spock-grails-support:0.7-groovy-2.0'
		
		runtime 'com.h2database:h2:1.4.187'   // or 'com.h2database:h2:1.3.175' for older version
		
		//compile 'org.geotools:gt-main:2.6.0'
		//compile 'net.sf.json-lib:json-lib:2.4'
		
		build ('javax.websocket:javax.websocket-api:1.0') { export = false }
    }

plugins {
        // plugins for the build system only
        build ":tomcat:7.0.55.2"

        // plugins for the compile step
        compile ":scaffolding:2.1.2"
        compile ':cache:1.1.8'
		compile ':asset-pipeline:2.1.5'

        // plugins needed at runtime but not for compilation
		runtime ':hibernate:3.6.10.19' // ':hibernate4:4.3.8.1' for Hibernate 4
        runtime ":database-migration:1.4.0"
        runtime ":resources:1.2.14"
		runtime ":jquery:1.11.1"
        // Uncomment these (or add new ones) to enable additional resources capabilities
        //runtime ":zipped-resources:1.0.1"
        //runtime ":cached-resources:1.1"
        //runtime ":yui-minify-resources:0.1.5"
		
		// Greate theme for admin site
		//runtime ':adminlte-ui:0.1.0'
		
		//compile ':platform-core:1.0.0'
		//runtime ':twitter-bootstrap:3.3.5'
		
		compile ":quartz:1.0.2"
    }
}
