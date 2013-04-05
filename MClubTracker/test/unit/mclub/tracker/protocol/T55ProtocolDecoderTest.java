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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * @author shawn
 * 
 */
public class T55ProtocolDecoderTest {

	@Test
	public void testDecode() throws Exception {

		T55ProtocolDecoder decoder = new T55ProtocolDecoder(new MockTrackerService());
		//decoder.setDataManager(new TestDataManager());

		assertNull(decoder.decode(null, null, "$PGID,359853000144328*0F"));

		assertNotNull(decoder.decode(null, null, "$GPRMC,094907.000,A,6000.5332,N,03020.5192,E,1.17,60.26,091111,,*33"));

		assertNotNull(decoder.decode(null, null, "$GPRMC,115528.000,A,6000.5432,N,03020.4948,E,,,091111,,*06"));

		assertNotNull(decoder.decode(null, null,
		        "$GPRMC,064411.000,A,3717.240078,N,00603.046984,W,0.000,1,010313,,,A*6C"));

	}

}
