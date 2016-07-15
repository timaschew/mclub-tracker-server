package mclub

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import org.grails.core.io.DefaultResourceLocator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.EnvironmentAware
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.EnumerablePropertySource
import org.springframework.core.env.Environment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.core.env.PropertySource

class Application extends GrailsAutoConfiguration implements EnvironmentAware {
    private Logger log = LoggerFactory.getLogger(getClass());

    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }

    private static final String DATABASE_CONFIG_FILE = "database.properties";

    @Override
    void setEnvironment(Environment env) {
        //ApplicationConfigurationLoader.load(this,env);
        //Set up Configuration directory
        def appHome = System.getenv('mclub.home') ?: System.getProperty('mclub.home') ?: "/opt/mclub"
        log.info "Loading configuration from ${appHome}"

        loadExternalConfigFile(appHome, DATABASE_CONFIG_FILE,env)
    }

    private void loadExternalConfigFile(String appHome, String fileName, ConfigurableEnvironment env){
        def configFile = new File(appHome, fileName);
        if (!configFile.exists()) {
            log.info("  configuration ${configFile} not found")
            return;
        }

        log.info("  configuration ${configFile} found")
        try{
            Properties config = new Properties();
            config.load(new FileInputStream(configFile));
//            def config = new ConfigSlurper().parse(dbcfgFile.toURL());
//            DefaultResourceLocator resourceLocator = new DefaultResourceLocator()
//            def configurationResource = resourceLocator.findResourceForURI(configLocation)
//            if (configurationResource) {
//                def config = new ConfigSlurper(grails.util.Environment.current.name).parse(configurationResource.getURL())
//                environment.propertySources.addFirst(new MapPropertySource(configLocation, config))
//            }
            Map<String,String> originalConfig = new HashMap<String,String>(config);
            String currentGrailsEnvName = grails.util.Environment.current.name;
            String keyPrefix = "environments." + currentGrailsEnvName + ".";
            // prefix all configurations with environment.${currentGrailsEnvName}.
            for(Map.Entry<String,String> e in originalConfig.entrySet()){
                if(!(e.key.startsWith(keyPrefix))){
                    config.put(keyPrefix + e.key,e.value);
                }
            }

            env.propertySources.addFirst(new PropertiesPropertySource("external.datasource", config))

            if(log.debugEnabled){
                log.debug("dump property sources - begins")
                env.propertySources.each{ PropertySource p ->
                    log.debug("${p.class} - ${p}");
                    if(p instanceof EnumerablePropertySource){
                        ((EnumerablePropertySource)p).propertyNames.each{ String pName ->
                            log.debug("    ${pName} -> ${p.getProperty(pName)}");
                        }
                    }else{
                        log.debug "    not enumerable"
                    }
                }
                log.debug("dump property sources - completed")
            }

        }catch(Exception e){
            log.error "Error loading DB configuration: ${e.message}", e
        }
    }

    class ApplicationConfigurationLoader {

        private ApplicationConfigurationLoader() {}

        public static load(GrailsAutoConfiguration application, Environment environment) {
            if (application && environment) {
                DefaultResourceLocator resourceLocator = new DefaultResourceLocator()
                def applicationGroovy = application.getClass().classLoader.getResource('application.groovy')
                if (applicationGroovy) {
                    def applicationConfiguration = new ConfigSlurper(grails.util.Environment.current.name).parse(applicationGroovy)
                    for (String configLocation in applicationConfiguration.grails.config.locations) {
                        def configurationResource = resourceLocator.findResourceForURI(configLocation)
                        if (configurationResource) {
                            def config = new ConfigSlurper(grails.util.Environment.current.name).parse(configurationResource.getURL())
                            environment.propertySources.addFirst(new MapPropertySource(configLocation, config))
                        }
                    }
                }
            }
        }
    }
}


