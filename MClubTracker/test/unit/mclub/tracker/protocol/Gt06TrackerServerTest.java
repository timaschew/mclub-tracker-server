package mclub.tracker.protocol;

import static org.junit.Assert.*;

import org.junit.Test;

public class Gt06TrackerServerTest {

	 @Test
	    public void testDecode() throws Exception {
		 
		 Gt06TrackerServer.Gt06ProtocolDecoder decoder = new Gt06TrackerServer.Gt06ProtocolDecoder();
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
