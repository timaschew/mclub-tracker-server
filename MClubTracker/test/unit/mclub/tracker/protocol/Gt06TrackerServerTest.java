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
	
	@Test
	public void testFoo(){
		/*
Recv: ##,imei:868683020378349,A;
Recv: imei:868683020378349,tracker,151025165516,,F,085513.000,A,3016.9793,N,12001.3847,E,0.00,0;
Recv: imei:868683020378349,tracker,151025171614,,F,091612.000,A,3017.7145,N,12006.4231,E,9.25,178.57;
Recv: imei:868683020378349,tracker,151025172744,,L,,,5717,,860e,,,;
Recv: 868683020378349;
		String datas[] = {
				"23232c696d65693a3836383638333032303337383334392c413b",
				"696d65693a3836383638333032303337383334392c747261636b65722c3135313032353136353531362c2c462c3038353531332e3030302c412c333031362e393739332c4e2c31323030312e333834372c452c302e30302c303b",
				"696d65693a3836383638333032303337383334392c747261636b65722c3135313032353137313631342c2c462c3039313631322e3030302c412c333031372e373134352c4e2c31323030362e343233312c452c392e32352c3137382e35373b",
				"696d65693a3836383638333032303337383334392c747261636b65722c3135313032353137323734342c2c4c2c2c2c353731372c2c383630652c2c2c3b",
				"3836383638333032303337383334393b"
		};
*/		
		String datas[] = {
				"2a48512c3335353438383032303133373332352c56312c3137323435312c412c333031312e35303433332c4e2c31323030392e30383432312c452c302e31372c302c3235313031352c4646454646424646230d0a",
				"2a48512c3335353438383032303133373332352c58542c562c302c30230d0a"
		};
		for(String data : datas){
			ChannelBuffer buf = binary(data);
			String s = new String(buf.array());
			System.out.println("Recv: " + s);
		}
	}

}
