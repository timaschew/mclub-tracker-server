package mclub.tracker.aprs;

import static org.junit.Assert.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mclub.tracker.PositionData;
import mclub.tracker.aprs.parser.APRSPacket;
import mclub.tracker.aprs.parser.CourseAndSpeedExtension;
import mclub.tracker.aprs.parser.DataExtension;
import mclub.tracker.aprs.parser.InformationField;
import mclub.tracker.aprs.parser.PHGExtension;
import mclub.tracker.aprs.parser.Parser;
import mclub.tracker.aprs.parser.Position;
import mclub.tracker.aprs.parser.PositionPacket;

import org.junit.Test;

public class AprsReceiverTest {

//	private static class AprsPacket{
//		String srcCall,dstCall,symbol,lat,lon,height,message;
//	}
//	
//	private AprsPacket parse(String s){
//		String APRS_PACKET_PATTERN = "^(\\w+)\\>(\\.+)\\:(\\)$";
//		Pattern p = Pattern.compile(APRS_PACKET_PATTERN);
//		Matcher m = p.matcher(s);
//		if(m.matches() && m.groupCount() >= 6){
//			AprsPacket packet = new AprsPacket();
//			String[] ret = new String[2];
//			ret[0] = m.group(1);
//			ret[1] = m.group(2);  
//			return packet;	
//		}
//		
//		return null;
//	}
	
	@Test
	public void testParseAPRS1() throws Exception{
		String s = "BR5HB-2>APOT21,WIDE1-1,qAS,BG5HLN-10:!3010.79N/12025.84E# 13.9V  30C ";
		
		APRSPacket pack = Parser.parse(s);
		assertNotNull(pack);
		
		InformationField info = pack.getAprsInformation();
		assertTrue(info instanceof PositionPacket);
		Position pos = ((PositionPacket)info).getPosition();;
		
		String ss = String.format("%s->%s, (%s,%s), %s", pack.getSourceCall(),pack.getDestinationCall(),pos.getLatitude(),pos.getLongitude(),info.getComment());
		System.out.println(ss);
		
	}

	
	@Test
	public void testParseAPRS_fixedSite() throws Exception{
		String s = "BA5DX-10>AP4R10,TCPIP*,qAC,T2XWT:!3022.74N/12002.74ErBA5DX IGATE  431.040Mhz  ^_^";

		APRSPacket pack = Parser.parse(s);
		assertNotNull(pack);
		
		InformationField info = pack.getAprsInformation();
		assertTrue(info instanceof PositionPacket);
		Position pos = ((PositionPacket)info).getPosition();;
		
		String ss = String.format("%s->%s, (%s,%s), %s", pack.getSourceCall(),pack.getDestinationCall(),pos.getLatitude(),pos.getLongitude(),info.getComment());
		System.out.println(ss);
	}

	@Test
	public void testParseAPRS3() throws Exception{
		//String s = "BI4QZW-13>WX51,qAS,BI4QZW-10:=3117.50N/12037.50E_000/000g000t089r000p000h46b10125SuZhou.JS.China QQ:234220938 12.0V 26.9C";
		String s = "BG5HLN-10>APET51,TCPIP*,qAC,T2XWT:!3030.71N/12054.78ErPHG2470/Haiyan iGate 144.390MHz 1200bps 9.1V";
				
		APRSPacket pack = Parser.parse(s);
		assertNotNull(pack);
		
		InformationField info = pack.getAprsInformation();
		assertTrue(info instanceof PositionPacket);
		PositionPacket pp = ((PositionPacket)info);
		Position pos = pp.getPosition();
		
		DataExtension ext = pp.getExtension();
		assertTrue(ext instanceof PHGExtension);
		PHGExtension phgExt = (PHGExtension)ext;
		
		String ss = String.format("%s->%s, (%s,%s), phg(%s,%s,%s) %s", pack.getSourceCall(),pack.getDestinationCall(),pos.getLatitude(),pos.getLongitude(),phgExt.getPower(),phgExt.getHeight(),phgExt.getGain(),info.getComment());
		System.out.println(ss);
	}

	@Test
	public void testParseAPRS_uncompressedSpeedPacket() throws Exception{
		String s = "BG5EEK-9>APOTC1,BR5HB-2*,WIDE1*,WIDE2-1,qAS,BG5HLN-10:/040236z3027.58N/12021.99E>356/019!W36!/A=000052 14.0V 52C";
		APRSPacket pack = Parser.parse(s);
		assertNotNull(pack);
		
		InformationField info = pack.getAprsInformation();
		assertTrue(info instanceof PositionPacket);
		Position pos = ((PositionPacket)info).getPosition();;
		
		String ss = String.format("%s->%s, lat:%s, lon:%s, ", pack.getSourceCall(),pack.getDestinationCall(),pos.getLatitude(),pos.getLongitude());
		System.out.print(ss);
		assertTrue("30.45967".equals(String.format("%.5f", pos.getLatitude())));
		assertTrue("120.3665".equals(String.format("%.4f", pos.getLongitude())));
		
		DataExtension ext = info.getExtension();
		assertTrue(ext instanceof CourseAndSpeedExtension);
		CourseAndSpeedExtension courseAndSpeedExt = (CourseAndSpeedExtension)ext;
		System.out.print(String.format("speed: %d, course:%d, ",courseAndSpeedExt.getSpeed(), courseAndSpeedExt.getCourse()));
		assertTrue(19 == courseAndSpeedExt.getSpeed());
		assertTrue(356 == courseAndSpeedExt.getCourse());
		
		System.out.println("Altitude: " + pos.getAltitude());
		ss = String.format("comment:%s", info.getComment());
		System.out.println(ss);
	}
	
	@Test
	public void testParseCompressedPacket() throws Exception{
		String s = "BG5DWL-9>R9SRW8,BR5DO-3*,WIDE1*,WIDE2-1,qAS,BG5DOV-10:`/Oel![j/]\"6]}=";
		APRSPacket pack = Parser.parse(s);
		assertNotNull(pack);
		
		InformationField info = pack.getAprsInformation();
		assertTrue(info instanceof PositionPacket);
		Position pos = ((PositionPacket)info).getPosition();;
		
		String ss = String.format("%s->%s, lat:%s, lon:%s, ", pack.getSourceCall(),pack.getDestinationCall(),pos.getLatitude(),pos.getLongitude());
		System.out.print(ss);
		
		DataExtension ext = info.getExtension();
		assertTrue(ext instanceof CourseAndSpeedExtension);
		CourseAndSpeedExtension courseAndSpeedExt = (CourseAndSpeedExtension)ext;
		System.out.print(String.format("speed: %d, course:%d, ",courseAndSpeedExt.getSpeed(), courseAndSpeedExt.getCourse()));
		
		ss = String.format("comment:%s", pack.getSourceCall(),pack.getDestinationCall(),pos.getLatitude(),pos.getLongitude(),info.getComment());
		System.out.println(ss);
	}
	
	@Test
	public void testPraseAltitudeInComment() throws Exception{
		String s = "BG5EEK-9>APOTC1,BR5HB-2*,WIDE1*,WIDE2-1,qAS,BG5HLN-10:/040236z3027.58N/12021.99E>356/019/A=000052HelloWorld";
		APRSPacket pack = Parser.parse(s);
		InformationField info = pack.getAprsInformation();
		Position pos = ((PositionPacket)info).getPosition();;
		System.out.println("Altitude: " + pos.getAltitude());
		System.out.println("Comment: " + info.getComment());
		assertEquals(16,pos.getAltitude());
		assertEquals("HelloWorld",info.getComment());
	}
	
	private static String convertSymbolCharToFileName(char table, char index){
		if(table == '/'){
			return "1_" + String.format("%02d", index - '!');	
		}else if(table == '\\'){
			return "2_" + String.format("%02d", index - '!');
		}else{
			return "1_29"; // '>', Car
		}
	}
	@Test
	public void testConvertSymbolFileName(){
		assertEquals("1_00",convertSymbolCharToFileName('/','!'));
		assertEquals("2_00",convertSymbolCharToFileName('\\','!'));
	}
	
	
	@Test 
	public void testReceiverParseAPRSPacket(){
		String s = "BG5EEK-9>APOTC1,BR5HB-2*,WIDE1*,WIDE2-1,qAS,BG5HLN-10:/040236z3027.58N/12021.99E>356/019/A=000052HelloWorld";
		PositionData pd = new AprsDecoder(null,null,null).decodeAPRS(s);
		assertNotNull(pd);
		AprsData aprsData = (AprsData)pd.getExtendedInfo().get("aprs");
		assertNotNull(aprsData);
		assertEquals("1_29",aprsData.getSymbol());
	}


	@Test
	public void testDecodeMICEPacket(){
		String s = "BG5HSC-9>SPQRQ5,WIDE1-1,qAR,BG5HSC-10:`0*Nm!kv/]\"4<}=";
		PositionData pd = new AprsDecoder(null,null,null).decodeAPRS(s);
		assertNotNull(pd);
		AprsData aprsData = (AprsData)pd.getExtendedInfo().get("aprs");
		assertNotNull(aprsData);
		assertEquals("1_85",aprsData.getSymbol());
		System.out.println(aprsData.getComment());
	}
	
	
	@Test
	public void testCalcualteXY(){
		int i = 2;
		System.out.println("x=" + (i % 16));
		System.out.println("y=" + (i / 16));
		
		Double d = new Double(0.12345678f);
		System.out.println("Formatted double: " + String.format("%.6f",d));
		
	}
	
	/*
	BA5AC-3 · 居中 · 放大 · 信息
	2014-07-13 08:08:15 - 2015-10-07 22:44:07
	APRS/CWOP 气象 2015-10-07 22:44:07: 显示气象图表
	温度 20.6°C 湿度 50% 气压 1010.3 mbar
	风 0° 0.0 m/s (大风 0.0 m/s)
	雨 0.0 mm/1h 0.0 mm/24h
	Yuhang cangqian WX 8.8V ---ug
	[WX51R via BR5AA-1*,WIDE1*,qAS,BA5DX-10]
	*/
	
	@Test
	public void testParseWeatherReport(){
		//Complete Weather Report Format — with Lat/Long position, no Timestamp
		String s = "BA5AC-3>WX51R,WIDE1-1,qAR,BA5AG-10:=3018.51N/12001.16E_000/000g000t078r000p000h53b10093Yuhang cangqian WX 8.8V ---ug";
		PositionData pd = new AprsDecoder(null,null,null).decodeAPRS(s);
		assertNotNull(pd);
		AprsData aprsData = (AprsData)pd.getExtendedInfo().get("aprs");
		System.out.println(aprsData.getComment());
	}


	@Test
	public void testParseSomeInvalidPacket(){
		String s = "BG5EOG-10>APVRT7,TCPIP*,qAC,T2XWT:!2930.50N/11954.56ErPHG1010144.3900MHz By AVRT7";
		PositionData pd = new AprsDecoder(null,null,null).decodeAPRS(s);
		assertNotNull(pd);
		AprsData aprsData = (AprsData)pd.getExtendedInfo().get("aprs");
		System.out.println(aprsData.getComment());
	}

	@Test
	public void testParseBlacklist(){
		assertTrue(deviceIsBlackListed("BG5123"));
		assertTrue(deviceIsBlackListed("BG6123"));
		assertFalse(deviceIsBlackListed("BG7123"));
	}

	private boolean deviceIsBlackListed(String udid){
		if(udid == null) {
			return false;
		}
		String[] bl = "BG5123,BG6*".trim().split(",");
		if(bl == null) {
			return false;
		}
		for(String b : bl){
			b = b.trim().toUpperCase();
			udid = udid.toUpperCase();
			if(b.endsWith("*")){
				b = b.substring(0,b.length()-1);
				if(udid.startsWith(b)){
					return true;
				}
			}else if(b.equals(udid)){
				return true;
			}
		}
		return false;
	}
}
