import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.rolling.RollingFileAppender
import grails.util.BuildSettings
import grails.util.Environment

// See http://logback.qos.ch/manual/groovy.html for details on configuration
appender('STDOUT', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%level %logger - %msg%n"
    }
}

appender("APRS", FileAppender) {
    file = "/var/log/aprs.log"
    //maxFileSize = 10240000
    encoder(PatternLayoutEncoder) {
        pattern = "%d{yyyy-MM-dd/HH:mm:ss.SSS} - %m%n"
    }
}

appender("PERF", FileAppender) {
    file = "/tmp/mtracker_perf.log"
    //maxFileSize = 10240000
    encoder(PatternLayoutEncoder) {
        pattern = "%d{yyyy-MM-dd/HH:mm:ss.SSS} - %m%n"
    }
}

appender("GPS", FileAppender) {
    file = "/tmp/mtracker_gps.log"
    //maxFileSize = 10240000
    encoder(PatternLayoutEncoder) {
        pattern = "%d{yyyy-MM-dd/HH:mm:ss.SSS} - %m%n"
    }
}

appender("TALKBOX", FileAppender) {
    file = "/var/log/talkbox.log"
    //maxFileSize = 10240000
    encoder(PatternLayoutEncoder) {
        pattern = "%d{yyyy-MM-dd/HH:mm:ss.SSS} - %m%n"
    }
}

root(WARN, ['STDOUT'])

logger("aprs.log",TRACE,['APRS'],false)
logger("perf.log",DEBUG,['PERF'],false)
logger("gps.log",DEBUG,['GPS'],false)
logger("talkbox.log",INFO,['TALKBOX'],false)

def targetDir = BuildSettings.TARGET_DIR
if (Environment.isDevelopmentMode() && targetDir) {
    appender("FULL_STACKTRACE", FileAppender) {
        file = "${targetDir}/stacktrace.log"
        append = true
        encoder(PatternLayoutEncoder) {
            pattern = "%level %logger - %msg%n"
        }
    }
    logger("StackTrace", ERROR, ['FULL_STACKTRACE'], false)
    logger("grails.app", DEBUG, ['STDOUT'], false)

    logger("mclub", INFO , ['STDOUT'], false)

    logger("mclub.user", INFO , ['STDOUT'], false)
    logger("mclub.tracker", INFO , ['STDOUT'], false)
}else if(Environment.current == Environment.PRODUCTION){
    logger("grails.app", INFO, ['STDOUT'], false)
    logger("mclub.user", INFO , ['STDOUT'], false)
    logger("mclub.tracker", INFO , ['STDOUT'], false)
    logger("mclub.security", INFO , ['STDOUT'], false)
}