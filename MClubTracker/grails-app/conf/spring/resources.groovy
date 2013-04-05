// Place your Spring DSL code here
import mclub.tracker.TrackerServerManager;

beans = {
	trackerServerManager(TrackerServerManager){
		trackerService = ref('trackerService')
	}
}
