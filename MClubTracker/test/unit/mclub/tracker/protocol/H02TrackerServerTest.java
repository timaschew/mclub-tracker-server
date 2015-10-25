package mclub.tracker.protocol;

import static org.junit.Assert.assertNull;

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import mclub.tracker.PositionData;
import mclub.tracker.protocol.helper.ChannelBufferTools;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Assert;
import org.junit.Test;
public class H02TrackerServerTest {

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

    protected ChannelBuffer buffer(String... data) {
        return ChannelBuffers.copiedBuffer(concatenateStrings(data), Charset.defaultCharset());
    }

    protected ChannelBuffer binary(ByteOrder endianness, String... data) {
        return ChannelBuffers.wrappedBuffer(
                endianness, ChannelBufferTools.convertHexString(concatenateStrings(data)));
    }
    
    @Test
    public void testDecodeTraccar() throws Exception {
    	H02TrackerServer.H02ProtocolDecoder decoder = new H02TrackerServer.H02ProtocolDecoder();
    	
        verifyPosition(decoder, buffer(
                "*HQ,355488020119695,V1,050418,,2827.61232,N,07703.84822,E,0.00,0,031015,FFFEFBFF#"));

        verifyPosition(decoder, buffer(
                "*HQ,1451316409,V1,030149,A,-23-29.0095,S,-46-51.5852,W,2.4,065,070315,FFFFFFFF#"));

        verifyNothing(decoder, buffer(
                "*HQ,353588020068342,V1,000000,V,0.0000,0,0.0000,0,0.00,0.00,000000,ffffffff,000106,000002,000203,004c87,16#"));

        verifyPosition(decoder, buffer(
                "*HQ,3800008786,V1,062507,V,3048.2437,N,03058.5617,E,000.00,000,250413,FFFFFBFF#"));
        
        verifyPosition(decoder, buffer(
                "*HQ,4300256455,V1,111817,A,1935.5128,N,04656.3243,E,0.00,100,170913,FFE7FBFF#"));

        verifyPosition(decoder, buffer(
                "*HQ,123456789012345,V1,155850,A,5214.5346,N,2117.4683,E,0.00,270.90,131012,ffffffff,000000,000000,000000,000000#"));
        
        verifyPosition(decoder, buffer(
                "*HQ,353588010001689,V1,221116,A,1548.8220,S,4753.1679,W,0.00,0.00,300413,ffffffff,0002d4,000004,0001cd,000047#"));

        verifyPosition(decoder, buffer(
                "*HQ,354188045498669,V1,195200,A,701.8915,S,3450.3399,W,0.00,205.70,050213,ffffffff,000243,000000,000000#"));
        
        verifyPosition(decoder, buffer(
                "*HQ,2705171109,V1,213324,A,5002.5849,N,01433.7822,E,0.00,000,140613,FFFFFFFF#"));
        
        verifyPosition(decoder, buffer(
                "*TH,2020916012,V1,050316,A,2212.8745,N,11346.6574,E,14.28,028,220902,FFFFFBFF#"));
        
        verifyPosition(decoder, buffer(
                "*TH,2020916012,V4,S17,130305,050316,A,2212.8745,N,11346.6574,E,14.28,028,220902,FFFFFBFF#"));
        
        verifyPosition(decoder, buffer(
                "*TH,2020916012,V4,S14,100,10,1,3,130305,050316,A,2212.8745,N,11346.6574,E,14.28,028,220902,FFFFFBFF#"));
        
        verifyPosition(decoder, buffer(
                "*TH,2020916012,V4,S20,ERROR,130305,050316,A,2212.8745,N,11346.6574,E,14.28,028,220902,FFFFFBFF#"));
        
        verifyPosition(decoder, buffer(
                "*TH,2020916012,V4,S20,DONE,130305,050316,A,2212.8745,N,11346.6574,E,14.28,028,220902,F7FFFBFF#"));
        
        verifyPosition(decoder, buffer(
                "*TH,2020916012,V4,R8,ERROR,130305,050316,A,2212.8745,N,11346.6574,E,14.28,028,220902,FFFFFBFF#"));
        
        verifyPosition(decoder, buffer(
                "*TH,2020916012,V4,S23,165.165.33.250:8800,130305,050316,A,2212.8745,N,11346.6574,E,14.28,028,220902,FFFFFBFF#"));
        
        verifyPosition(decoder, buffer(
                "*TH,2020916012,V4,S24,thit.gd,130305,050316,A,2212.8745,N,11346.6574,E,14.28,028,220902,FFFFFBFF#"));
        
        verifyPosition(decoder, buffer(
                "*TH,2020916012,V4,S1,OK,pass_word,130305,050316,A,2212.8745,N,11346.6574,E,14.28,028,220902,FFFFFBFD#"));
        
        verifyPosition(decoder, buffer(
                "*HQ,353588020068342,V1,062840,A,5241.1249,N,954.9490,E,0.00,0.00,231013,ffffffff,000106,000002,000203,004c87,24#"));

        verifyPosition(decoder, buffer(
                "*HQ,353505220903211,V1,075228,A,5227.5039,N,01032.8443,E,0.00,0,231013,FFFBFFFF,106,14, 201,2173#"));

        verifyPosition(decoder, buffer(
                "*HQ,353505220903211,V1,140817,A,5239.3538,N,01003.5292,E,21.03,312,221013,FFFBFFFF,106,14, 203,1cd#"));
        
        verifyPosition(decoder, buffer(
                "*HQ,356823035368767,V1,083618,A,0955.6392,N,07809.0796,E,0.00,0,070414,FFFBFFFF,194,3b5,  71,c9a9#"));

        verifyNothing(decoder, buffer(
                "*HQ,8401016597,BASE,152609,0,0,0,0,211014,FFFFFFFF#"));

        verifyPosition(decoder, binary(
                "24410600082621532131081504419390060740418306000000fffffbfdff0015060000002c02dc0c000000001f"));

        verifyPosition(decoder, binary(
                "2427051711092133391406135002584900014337822e000000ffffffffff0000"));

        verifyPosition(decoder, binary(
                "2427051711092134091406135002584900014337822e000000ffffffffff0000"));

        verifyPosition(decoder, binary(
                "2410307310010503162209022212874500113466574C014028fffffbffff0000"));

        verifyPosition(decoder, binary(
                "2441090013450831250401145047888000008554650e000000fffff9ffff001006000000000106020299109c01"));

//        verifyPosition(decoder, binary(
//                "24270517030820321418041423307879000463213792000056fffff9ffff0000"));

        verifyPosition(decoder, binary(
                "2441091144271222470112142233983006114026520E000000FFFFFBFFFF0014060000000001CC00262B0F170A"));
        
        verifyPosition(decoder, binary(
                "24971305007205201916101533335008000073206976000000effffbffff000252776566060000000000000000000049"));
    }
    
	@Test
	public void testDecode() throws Exception {
		H02TrackerServer.H02ProtocolDecoder decoder = new H02TrackerServer.H02ProtocolDecoder();
		
		PositionData pos;
		
		pos = (PositionData)decoder
				.decode(null, null,
						buffer("*HQ,355488020137325,V1,172451,A,3011.50433,N,12009.08421,E,0.17,0,251015,FFEFFBFF#"));
		verifyDecodedPosition(pos);
		
		pos = (PositionData)decoder
				.decode(null, null,
						buffer("*HQ,355488020137325,XT,V,0,0#"));
		assertNull(pos);
		
	}
	
	
	@Test
	public void testHex(){
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

    protected void verifyPosition(H02TrackerServer.H02ProtocolDecoder decoder, Object object) throws Exception {
        verifyDecodedPosition(decoder.decode(null, null, object));
    }

    protected void verifyPosition(H02TrackerServer.H02ProtocolDecoder decoder, Object object, PositionData position) throws Exception {
        verifyDecodedPosition(decoder.decode(null, null, object), position);
    }

    protected void verifyPositions(H02TrackerServer.H02ProtocolDecoder decoder, Object object) throws Exception {
        Object decodedObject = decoder.decode(null, null, object);
        Assert.assertNotNull(decodedObject);
        Assert.assertTrue(decodedObject instanceof List);
        for (Object item : (List) decodedObject) {
            verifyDecodedPosition(item);
        }
    }

    private void verifyDecodedPosition(Object decodedObject, PositionData expected) {

        Assert.assertNotNull(decodedObject);
        Assert.assertTrue(decodedObject instanceof PositionData);

        PositionData position = (PositionData) decodedObject;

        if (expected.getTime() != null) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Assert.assertEquals("time",
                    dateFormat.format(expected.getTime()), dateFormat.format(position.getTime()));
        }
        Assert.assertEquals("valid", expected.getValid(), position.getValid());
        Assert.assertEquals("latitude", expected.getLatitude(), position.getLatitude(), 0.00001);
        Assert.assertEquals("longitude", expected.getLongitude(), position.getLongitude(), 0.00001);

        verifyDecodedPosition(decodedObject);

    }
    private void verifyDecodedPosition(Object decodedObject) {

        Assert.assertNotNull(decodedObject);
        Assert.assertTrue(decodedObject instanceof PositionData);

        PositionData position = (PositionData) decodedObject;

        Assert.assertNotNull(position.getTime());
        Assert.assertTrue("year > 2000", position.getTime().after(new Date(946684800000L)));
        Assert.assertTrue("time < +25 hours",
                position.getTime().getTime() < System.currentTimeMillis() + 25 * 3600000);

        Assert.assertTrue("latitude >= -90", position.getLatitude() >= -90);
        Assert.assertTrue("latitude <= 90", position.getLatitude() <= 90);

        Assert.assertTrue("longitude >= -180", position.getLongitude() >= -180);
        Assert.assertTrue("longitude <= 180", position.getLongitude() <= 180);

        Assert.assertTrue("altitude >= -12262", position.getAltitude() >= -12262);
        Assert.assertTrue("altitude <= 18000", position.getAltitude() <= 18000);

        Assert.assertTrue("speed >= 0", position.getSpeed() >= 0);
        Assert.assertTrue("speed <= 869", position.getSpeed() <= 869);

        Assert.assertTrue("course >= 0", position.getCourse() >= 0);
        Assert.assertTrue("course <= 360", position.getCourse() <= 360);
    }
    
    protected void verifyNothing(H02TrackerServer.H02ProtocolDecoder decoder, Object object) throws Exception {
        Assert.assertNull(decoder.decode(null, null, object));
    }
}