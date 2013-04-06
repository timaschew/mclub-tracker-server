import java.util.Date;

import mclub.tracker.TrackerDevice
import mclub.tracker.TrackerPosition
import mclub.util.GrailsApplicationHolder;

class BootStrap {
    def init = { servletContext ->
		// add test devices
		if(TrackerDevice.count() == 0){
			// load initial data
			new TrackerDevice(deviceId:'123456789012345').save(flush:true);
			
			// mock device position
			new TrackerPosition(
				deviceId:2,
				time:new Date(),
				valid:true,
				latitude:20,
				longitude:130,
				altitude:0,
				speed:28,
				course:180
			).save(flush:true);
			new TrackerPosition(
				deviceId:2,
				time:new Date(),
				valid:true,
				latitude:21,
				longitude:131,
				altitude:0,
				speed:29,
				course:181
			).save(flush:true);
			new TrackerPosition(
				deviceId:2,
				time:new Date(),
				valid:true,
				latitude:22,
				longitude:132,
				altitude:0,
				speed:30,
				course:182
			).save(flush:true);
		}
    }
	
    def destroy = {
    }
}
