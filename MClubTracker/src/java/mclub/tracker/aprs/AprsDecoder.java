package mclub.tracker.aprs;

import mclub.tracker.PositionData;
import mclub.tracker.aprs.parser.APRSPacket;
import mclub.tracker.aprs.parser.CourseAndSpeedExtension;
import mclub.tracker.aprs.parser.DataExtension;
import mclub.tracker.aprs.parser.Digipeater;
import mclub.tracker.aprs.parser.InformationField;
import mclub.tracker.aprs.parser.PHGExtension;
import mclub.tracker.aprs.parser.Parser;
import mclub.tracker.aprs.parser.Position;
import mclub.tracker.aprs.parser.PositionPacket;
import mclub.tracker.aprs.parser.Utilities;
import mclub.user.AuthUtils;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AprsDecoder extends OneToOneDecoder{
	private static Logger log = LoggerFactory.getLogger(AprsDecoder.class);
	private static Logger aprsLog = LoggerFactory.getLogger("aprs.log");

	private int state;
	private static final int STATE_INIT_WAIT_WELCOME = 0;
	private static final int STATE_LOGIN_WAIT_ACK = 1;
	private static final int STATE_WAIT_APRS = 2;

	private String loginCommand;
	
	public AprsDecoder(String call,String pass, String filter){
		this.loginCommand = buildAprsLoginCommand(call,pass,filter);
	}
	
	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel,
			Object msg) throws Exception {
		String response = (String)msg;
		switch(state){
		case STATE_INIT_WAIT_WELCOME:{
			// send the login command
			log.debug("<<< APRS Welcome: " + response);
			// send the login command and wait for the ack
			if(sendLoginCommand(channel)){
				state = STATE_LOGIN_WAIT_ACK;
				log.debug(">>> APRS Login: " + loginCommand);
			}
			break;
		}
		case STATE_LOGIN_WAIT_ACK:{
			state = STATE_WAIT_APRS;
			log.debug("<<< APRS Login ACK: " + response);
			break;
		}
		case STATE_WAIT_APRS:{
			try{
				log.debug("<<< APRS Packet: " + response);
				return decodeAPRS(response);
			}catch(Exception e){
				log.error("Error decode APRS packet", e);
			}
			break;
		}
		default:
			log.warn("Unhandled Server Response: " + response);
			break;
		}			
		return null;
	}
	
	private static final String APRS_LOGIN_COMMAND_PATTERN = "user %s pass %s vers mClubClient 001 filter %s\r\n";
	static String buildAprsLoginCommand(String call, String pass,String filter){
		return String.format(APRS_LOGIN_COMMAND_PATTERN, call,pass,filter);
	}

	protected boolean sendLoginCommand(Channel channel){
		//log.debug("Send APRS Login:" + loginCommand);
		try{
			ChannelFuture writeFuture = channel.write(loginCommand);
			//writeFuture.awaitUninterruptibly();
			return true;
		}catch(Exception e){
			// noop
		}
		return false;
		//writeFuture.awaitUninterruptibly();
		//return writeFuture.isSuccess();
		//return true;
	}
	
	protected PositionData decodeAPRS(String aprsMessage){
		// log aprs message
		aprsLog.trace(aprsMessage);
		PositionData positionData = null;
		if(aprsMessage.startsWith("#")){
			if(log.isDebugEnabled()) log.debug("RECV CMD: " + aprsMessage);
			return null;
		}
		try{
			APRSPacket pack = Parser.parse(aprsMessage);
			if(pack == null || !pack.isAprs()){
				// Invalid aprs packet
				log.info("Invalid APRS packet: " + (pack == null?"null":pack.getOriginalString()));
				return null;
			}
			
			InformationField info = pack.getAprsInformation();
			if(info instanceof PositionPacket){
				positionData = new PositionData();
				
				positionData.setUdid(pack.getSourceCall());
				// extract the call without ssid
				String[] name_id = AuthUtils.extractAPRSCall(pack.getSourceCall());
				if(name_id != null){
					positionData.setUsername(name_id[0]);	
				}else{
					log.warn("Invalid APRS source call " + pack.getSourceCall() + " Raw: " + aprsMessage);
				}
				
				Position pos = ((PositionPacket)info).getPosition();
				if(pos.getLatitude() == 0 && pos.getLongitude() == 0){
					// Invalid 0,0 position received, this might be caused by some broken APRS devices
					log.info("Invalid APRS packet position [0,0]: " + (pack == null?"null":pack.getOriginalString()));
					return null;
				}
				
				positionData.setLatitude(pos.getLatitude());
				positionData.setLongitude(pos.getLongitude());
				positionData.setAltitude(new Double(pos.getAltitude()));
				
				DataExtension ext = info.getExtension();
				if(ext instanceof CourseAndSpeedExtension){
					CourseAndSpeedExtension csext = (CourseAndSpeedExtension)ext;
					positionData.setSpeed(new Double(Utilities.kntsToKmh(csext.getSpeed()))); // speed in km/h
					positionData.setCourse(new Double(csext.getCourse()));
				}else{
					positionData.setCourse(new Double(-1));
					positionData.setSpeed(new Double(-1));
				}
				
				AprsData aprsData = new AprsData();
				// digi peater path
				StringBuilder sb = new StringBuilder();
				for(Digipeater digiPeater : pack.getDigipeaters()) {
					sb.append(digiPeater.toString()).append(',');
				}
				sb.deleteCharAt(sb.length()-1);
				aprsData.setPath(sb.toString());
				// comment
				aprsData.setComment(info.getComment());
				// index of symbol
				aprsData.setSymbol(convertSymbolCharToFileName(pos.getSymbolTable(),pos.getSymbolCode()));
				aprsData.setDestination(pack.getDestinationCall());
				// PHG info
				if(ext instanceof PHGExtension){
					PHGExtension phg = (PHGExtension)ext;
					aprsData.setHeight(new Integer(phg.getHeight()));
					aprsData.setGain(new Integer(phg.getGain()));
					aprsData.setPower(new Integer(phg.getPower()));
					aprsData.setDirectivity(new Integer(phg.getDirectivity()));
				}
				positionData.setTime(pos.getTimestamp());
				positionData.setValid(true);
				positionData.setAprs(true);
				positionData.addExtendedInfo("aprs",aprsData);					
				positionData.addExtendedInfo("protocol", "APRS");
				
				log.debug("ACCEPT: " + aprsMessage);				
				return positionData;
			}
		}catch(Exception e){
			log.info("Error parse APRS message, " + e.getMessage() + ". Raw: " + aprsMessage);
		}
		
		return null;
	}

	/**
	 * Convert symbol char to file name
	 * see http://www.aprs.net/vm/DOS/SYMBOLS.HTM
	 *     http://wa8lmf.net/aprs/APRS_symbols.htm
	 * @param table
	 * @param index
	 * @return
	 */
	private static String convertSymbolCharToFileName(char table, char index){
		if(table == '/'){
			return "aprs_1_" + String.format("%02d", index - '!');	
		}else if(table == '\\'){
			return "aprs_2_" + String.format("%02d", index - '!');
		}else{
			// ignore the overlay currently, always choose from the table 1
			return "aprs_1_" + String.format("%02d", index - '!');
			// "aprs_1_29"; // '>', Car
		}
	}
}
