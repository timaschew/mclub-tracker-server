package mclub.tracker.protocol;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import mclub.tracker.protocol.helper.ChannelBufferTools;
import mclub.tracker.protocol.helper.Checksum;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;

public class Gt06TrackerServerTest {

    private String concatenateStrings(String... strings) {
        StringBuilder builder = new StringBuilder();
        for (String s : strings) {
            builder.append(s);
        }
        return builder.toString();
    }

    protected ChannelBuffer binary(String... data) {
        return binary(ByteOrder.BIG_ENDIAN, data);
    }

    protected ChannelBuffer binary(ByteOrder endianness, String... data) {
        return ChannelBuffers.wrappedBuffer(
                endianness, ChannelBufferTools.convertHexString(concatenateStrings(data)));
    }

	@Test
	public void testDecode() throws Exception {

		Gt06TrackerServer.Gt06ProtocolDecoder decoder = new Gt06TrackerServer.Gt06ProtocolDecoder();
		
		//assertNotNull(decoder.decode(null, null, binary("787805120135dd520d0a")));
		assertNull(decoder.decode(null,null,binary("78780d01035548802012515100036bf70d0a")));
		assertEquals("355488020125151",decoder.getDeviceId());
		
		Object o = decoder
				.decode(null, null,
						binary("78781f120f0a17143037ca033d362a0ce3ebfa16347201cc0058140071c4013b0c0a0d0a"));
		assertNotNull(o);

		// 7878 0d01 0355 4880 2012 5151 000b e7bf 0d0a
	}

	@Test
	public void testCRC() {
		// '0501000b' --> 7686
		// 0d01 0355 4880 2012 5151 000b --> e7bf
		short i = 0x80;
		byte b[] = { 0x0d, 0x01, 0x03, 0x55, 0x48, (byte) (i & 0xff), 0x20,
				0x12, 0x51, 0x51, 0x00, 0x0b };
		int crc = Checksum.crc16(Checksum.CRC16_X25, ByteBuffer.wrap(b));
		assertEquals(0xe7bf, crc);
	}

}
