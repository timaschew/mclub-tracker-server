// Place your Spring DSL code here


import mclub.tracker.LivePositionWebsocketServer
import mclub.tracker.TrackerServerManager;
import mclub.tracker.aprs.AprsReceiver;

beans = {
	trackerServerManager(TrackerServerManager){
		trackerDataService = ref('trackerDataService')
		configService = ref('configService')
	}
	
	aprsReceiver(AprsReceiver){
		trackerDataService = ref('trackerDataService')
		configService = ref('configService')
	}

    livePositionWebsocketServer(LivePositionWebsocketServer){
        grailsApplication = ref('grailsApplication')
        trackerService = ref('trackerService')
        trackerDataService = ref('trackerDataService')
        messageService = ref('messageService')
    }
}
