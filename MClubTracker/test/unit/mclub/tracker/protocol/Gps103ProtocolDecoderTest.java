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

import static org.junit.Assert.*;
import mclub.tracker.PositionData;
import mclub.tracker.TrackerPosition;

import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.junit.Test;

/**
 * @author shawn
 * 
 */
public class Gps103ProtocolDecoderTest {
	@Test 
	public void testDecode_nomove() throws Exception{
		String msg = "imei:123456789012345,tracker,1304052225,15824189878,F,142532.000,A,3012.4191,N,12012.2353,E,0.00,,;";
		Gps103TrackerServer decoder = new Gps103TrackerServer(new MockNettyBootstrap(),"",new MockTrackerService());
		PositionData p = (PositionData)decoder.decode(null, null, msg);
		assertNotNull(p);
	}
	
	@Test 
	public void testDecode_acalarm() throws Exception{
		String msg = "imei:123456789012345,ac alarm,1304052259,15824189878,F,145912.000,A,3011.4072,N,12009.1746,E,8.41,352.59,;";
		Gps103TrackerServer decoder = new Gps103TrackerServer(new MockNettyBootstrap(),"",new MockTrackerService());
		PositionData p = (PositionData)decoder.decode(null, null, msg);
		assertNotNull(p);
	}
	
	@Test
	public void testDecode() throws Exception {

		Gps103TrackerServer decoder = new Gps103TrackerServer(new MockNettyBootstrap(),"",new MockTrackerService());
		//decoder.setDataManager(new TestDataManager());

		// Log on request
		assertNull(decoder.decode(null, null, "##,imei:359586015829802,A"));

		// Heartbeat package
		assertNull(decoder.decode(null, null, "359586015829802"));

		// No GPS signal
		assertNull(decoder.decode(null, null, "imei:359586015829802,tracker,000000000,13554900601,L,;"));

		assertNotNull(decoder.decode(null, null,
		        "imei:359710040656622,tracker,13/02/27 23:40,,F,125952.000,A,3450.9430,S,13828.6753,E,0.00,0"));

		assertNotNull(decoder.decode(null, null,
		        "imei:353451047570260,tracker,1302110948,,F,144807.000,A,0805.6615,S,07859.9763,W,0.00,,"));

		assertNotNull(decoder.decode(null, null,
		        "imei:359587016817564,tracker,1301251602,,F,080251.000,A,3223.5832,N,11058.9449,W,0.03,"));

		assertNotNull(decoder.decode(null, null,
		        "imei:012497000208821,tracker,1301080525,,F,212511.000,A,2228.5279,S,06855.6328,W,18.62,268.98,"));

		assertNotNull(decoder.decode(null, null,
		        "imei:012497000208821,tracker,1301072224,,F,142411.077,A,2227.0739,S,06855.2912,,0,0,"));

		assertNotNull(decoder.decode(null, null,
		        "imei:012497000431811,tracker,1210260609,,F,220925.000,A,0845.5500,N,07024.7673,W,0.00,,"));

		assertNotNull(decoder.decode(null, null,
		        "imei:100000000000000,help me,1004171910,,F,010203.000,A,0102.0003,N,00102.0003,E,1.02,"));

		assertNotNull(decoder.decode(null, null,
		        "imei:353451040164707,tracker,1105182344,+36304665439,F,214418.000,A,4804.2222,N,01916.7593,E,0.37,"));

		assertNotNull(decoder.decode(null, null,
		        "imei:353451042861763,tracker,1106132241,,F,144114.000,A,2301.9052,S,04909.3676,W,0.13,"));

		assertNotNull(decoder
		        .decode(null, null,
		                "imei:359587010124900,tracker,0809231929,13554900601,F,112909.397,A,2234.4669,N,11354.3287,E,0.11,321.53,"));

		assertNotNull(decoder
		        .decode(null, null,
		                "imei:353451049926460,tracker,1208042043,123456 99008026,F,124336.000,A,3509.8668,N,03322.7636,E,0.00,,"));

		// SOS alarm
		assertNotNull(decoder.decode(null, null,
		        "imei:359586015829802,help me,0809231429,13554900601,F,062947.294,A,2234.4026,N,11354.3277,E,0.00,"));

		// Low battery alarm
		assertNotNull(decoder
		        .decode(null, null,
		                "imei:359586015829802,low battery,0809231429,13554900601,F,062947.294,A,2234.4026,N,11354.3277,E,0.00,"));

		// Geo-fence alarm
		assertNotNull(decoder.decode(null, null,
		        "imei:359586015829802,stockade,0809231429,13554900601,F,062947.294,A,2234.4026,N,11354.3277,E,0.00,"));

		// Move alarm
		assertNotNull(decoder.decode(null, null,
		        "imei:359586015829802,move,0809231429,13554900601,F,062947.294,A,2234.4026,N,11354.3277,E,0.00,"));

		// Over speed alarm
		assertNotNull(decoder.decode(null, null,
		        "imei:359586015829802,speed,0809231429,13554900601,F,062947.294,A,2234.4026,N,11354.3277,E,0.00,"));

		assertNotNull(decoder.decode(null, null,
		        "imei:863070010423167,tracker,1211051840,,F,104000.000,A,2220.6483,N,11407.6377,,0,0,"));

		assertNotNull(decoder.decode(null, null,
		        "imei:863070010423167,tracker,1211051951,63360926,F,115123.000,A,2220.6322,N,11407.5313,E,0.00,,"));

		assertNotNull(decoder.decode(null, null,
		        "imei:863070010423167,tracker,1211060621,,F,062152.000,A,2220.6914,N,11407.5506,E,15.85,347.84,"));

		assertNotNull(decoder.decode(null, null,
		        "imei:863070012698733,tracker,1303092334,,F,193427.000,A,5139.0369,N,03907.2791,E,0.00,,"));
	}

}
