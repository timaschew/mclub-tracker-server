package mclub.tracker.aprs;

import static org.junit.Assert.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

}
