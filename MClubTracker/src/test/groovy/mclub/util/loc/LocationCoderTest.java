/**
 * Project: MClubTracker
 * 
 * File Created at 2014-8-2
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
package mclub.util.loc;

import static org.junit.Assert.*;

import java.io.File;

import mclub.util.loc.LocationCoderService;
import mclub.util.loc.LocationObject;

import org.junit.Test;

/**
 * @author shawn
 *
 */
public class LocationCoderTest {

	@Test
	public void testGeoCoderService() {
		LocationCoderService gc = new LocationCoderService("foo");
		LocationObject loc = gc.queryGeoCodeService("浙江，杭州");
		assertNotNull(loc);
		
	}
	
	@Test
	public void testGeoCoderSearch(){
		String geocodeFileName = "/tmp/geocode.json";
		LocationCoderService gc = new LocationCoderService(geocodeFileName);
		
		LocationObject loc = gc.getLocation("浙江，滨江区");
		assertNotNull(loc);
		gc.flushCache();
		
	}
	
	//T三乡438.07/-5/T88.5/20140815
	@Test
	public void testGeoCoderSearchFailure(){
		String geocodeFileName = "/tmp/geocode.json";
		LocationCoderService gc = new LocationCoderService(geocodeFileName);
		
		LocationObject loc = gc.getLocation("重庆武陵山",true);
		assertNotNull(loc);
		gc.flushCache();
		
	}
}
