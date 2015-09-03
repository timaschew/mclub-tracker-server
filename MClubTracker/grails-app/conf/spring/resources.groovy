// Place your Spring DSL code here
import mclub.tracker.TrackerServerManager;
import mclub.tracker.aprs.AprsReceiver;

beans = {
	trackerServerManager(TrackerServerManager){
		trackerDataService = ref('trackerDataService')
	}
	
	aprsReceiver(AprsReceiver){
		trackerDataService = ref('trackerDataService')
	}
}
