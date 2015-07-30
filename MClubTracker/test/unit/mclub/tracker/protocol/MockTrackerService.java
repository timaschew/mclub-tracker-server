/**
 * Project: CarTracServer
 * 
 * File Created at Apr 5, 2013
 * $id$
 * 
 * Copyright 2013, Shawn Chain (shawn.chain@gmail.com).
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mclub.tracker.protocol;

import java.util.HashMap;
import java.util.Map;

import mclub.tracker.TrackerDataService;
import mclub.tracker.TrackerPosition;

/**
 * @author shawn
 *
 */
public class MockTrackerService extends TrackerDataService {
	private long pid = 1L;
	private Map<String,Object> config = new HashMap<String,Object>();
	
	/* (non-Javadoc)
	 * @see mclub.tracker.TrackerService#getIdByUniqueDeviceId(java.lang.String)
	 */
    @Override
    public Long lookupDeviceId(String imeiOrUdid) {
    	return 1L;
    }

	/* (non-Javadoc)
	 * @see mclub.tracker.TrackerService#addPosition(mclub.tracker.TrackerPosition)
	 */
    @Override
    public Long addPosition(TrackerPosition position) throws Exception {
	    return pid++;
    }

	/* (non-Javadoc)
	 * @see mclub.tracker.TrackerService#updateLatestPosition(java.lang.Long, java.lang.Long)
	 */
    @Override
    public void updateLatestPosition(Long deviceId, Long positionId) throws Exception {
	    // noop
    }
    
	/**
	 * Bridge methods for Java POJO to access the grails configuration
	 * @return
	 */
	public Map<String,Object> getConfig(){
		return config;
	}
	
	/**
	 * Get config by key
	 * @param key
	 * @return
	 */
	public Object getConfig(String key){
		if(key.endsWith(".address")){
			return "127.0.0.1";
		}else if(key.endsWith(".port")){
			return 5000;
		}
		return null;
	}

}
