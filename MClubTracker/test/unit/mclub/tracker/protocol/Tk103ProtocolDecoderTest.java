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
public class Tk103ProtocolDecoderTest {

	 @Test
	    public void testDecode() throws Exception {

	        Tk103TrackerServer decoder = new Tk103TrackerServer(new MockNettyBootstrap(),"",new MockTrackerService(),new MockConfigService());
//	        decoder.setDataManager(new TestDataManager());

	        assertNull(decoder.decode(null, null, "(090411121854BP0000001234567890HSO"));

	        assertNotNull(decoder.decode(null, null,
	                "(035988863964BP05000035988863964110524A4241.7977N02318.7561E000.0123536356.5100000000L000946BB"));

	        assertNotNull(decoder.decode(null, null,
	                "(013632782450BP05000013632782450120803V0000.0000N00000.0000E000.0174654000.0000000000L00000000"));

	        assertNotNull(decoder.decode(null, null,
	                "(013666666666BP05000013666666666110925A1234.5678N01234.5678W000.002033490.00000000000L000024DE"));
	        
	        assertNotNull(decoder.decode(null, null,
	                "(013666666666BO012110925A1234.5678N01234.5678W000.0025948118.7200000000L000024DE"));

	        assertNotNull(decoder.decode(null, null,
	                "\n\n\n(088045133878BR00130228A5124.5526N00117.7152W000.0233614352.2200000000L01B0CF1C"));

	    }

}
