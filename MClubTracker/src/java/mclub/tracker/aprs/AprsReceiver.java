package mclub.tracker.aprs;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import mclub.tracker.PositionData;
import mclub.tracker.TrackerDataService;
import mclub.tracker.aprs.parser.APRSPacket;
import mclub.tracker.aprs.parser.CourseAndSpeedExtension;
import mclub.tracker.aprs.parser.DataExtension;
import mclub.tracker.aprs.parser.Digipeater;
import mclub.tracker.aprs.parser.InformationField;
import mclub.tracker.aprs.parser.PHGExtension;
import mclub.tracker.aprs.parser.Parser;
import mclub.tracker.aprs.parser.Position;
import mclub.tracker.aprs.parser.PositionPacket;
import mclub.user.AuthUtils;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AprsReceiver {
	private Logger aprsLog = LoggerFactory.getLogger("aprs.log");
	
	private Logger log = LoggerFactory.getLogger(getClass());
	
    /********************************************************************************/
    /* Properties */
    /********************************************************************************/
	
	private TrackerDataService trackerDataService;
	private ClientBootstrap bootstrap;
    private ChannelGroup allChannels = new DefaultChannelGroup();

	private String protocol = "aprs";
    private String address;
	private Integer port;
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public Integer getPort() {
        return port;
    }
    public void setPort(Integer port) {
        this.port = port;
    }
    public String getProtocol() {
        return protocol;
    }    
    public TrackerDataService getTrackerDataService(){
    	return trackerDataService;
    }
    public void setTrackerDataService(TrackerDataService trackerDataService) {
		this.trackerDataService = trackerDataService;
	}
	public ChannelGroup getChannelGroup() {
        return allChannels;
    }
    
    
    /********************************************************************************/
    /* Service life cycle */
    /********************************************************************************/
	@PostConstruct
	public void startup(){
		if(!isEnabled()){
			return;
		}
		log.info("Startup APRS receiver...");
		initReceiver();
	}
	
	@PreDestroy
	public void shutdown(){
		if(!isEnabled()){
			return;
		}
		
		log.info("Shutdown APRS receiver...");
		// Close the connections
		try{
			// channel removed from group when closed.
			ChannelGroupFuture future = getChannelGroup().close();
	        future.awaitUninterruptibly();
		}catch(Exception e){
			// don't log
		}
        
		// Release resources
		releaseNettyResources();
	}
	
	private static final String APRS_LOGIN_COMMAND_PATTERN = "user %s pass %s vers mClubClient 001 filter %s\r\n";
	
	private String buildAprsLoginCommand(String call, String pass,String filter){
		return String.format(APRS_LOGIN_COMMAND_PATTERN, call,pass,filter);
	}
	
	private String getConfigString(String key){
		return (String)trackerDataService.getConfig(key);
	}
	
	private int getConfigInt(String key){
		return (Integer)trackerDataService.getConfig(key);
	}
	
	private void initReceiver(){
		InetSocketAddress aprsServerAddr;
		address = (String)trackerDataService.getConfig("tracker." + protocol + ".address");
        port = (Integer)trackerDataService.getConfig("tracker." + protocol + ".port");
        if (address == null) {
        	aprsServerAddr = new InetSocketAddress(port);
        } else {
        	aprsServerAddr = new InetSocketAddress(address, port);
        }
        
 		// Start the connection attempt.
 		try{
 	        bootstrap = new ClientBootstrap(
 					new NioClientSocketChannelFactory(
 							Executors.newCachedThreadPool(),
 							Executors.newCachedThreadPool(),
 							1 /*boss thread count*/,
 							2 /*worker thread count*/));        
 	        // Configure the pipeline factory.
 	 		bootstrap.setPipelineFactory(new PipelineFactory());
 			
 			ChannelFuture future = bootstrap.connect(aprsServerAddr);
 			// Wait until the connection attempt succeeds or fails.
 			Channel channel = future.awaitUninterruptibly().getChannel();
			if (future.isSuccess()) {
				// store the channels
				getChannelGroup().add(channel);
				log.debug("Connected to APRS server " + address + ":" + port);
				
				//FIXME - should use states
				Thread.sleep(5000); // sleep 5s for server welcome message;
				
				// TODO - use LoginCommand filters
				// Writing the filter command
				String call = getConfigString("tracker.aprs.call");
				String pass = getConfigString("tracker.aprs.pass");
				
				//32.14/120.09 hangzhou center
				//30.21,120.15 the river side
				
				String filter = getConfigString("tracker.aprs.filter");
				String loginCmd = buildAprsLoginCommand(call,pass,filter);
				log.debug("Send APRS Login:" + loginCmd);
				ChannelFuture writeFuture = channel.write(loginCmd);
				writeFuture.awaitUninterruptibly();
				
			}else{
				log.error("Error connect APRS server", future.getCause());
				releaseNettyResources();
				return;
			}
 		}catch(Exception e){
 			log.error("Error connect APRS server", e);
			releaseNettyResources();
 		}
	}
	
	private void releaseNettyResources(){
		if(bootstrap != null){
			try{
				bootstrap.releaseExternalResources();
			}finally{
				bootstrap = null;
			}
		}
	}
	
	private boolean isEnabled(){
		return Boolean.TRUE.equals(trackerDataService.getConfig("tracker."+protocol+".enabled"));
	}
	
	public boolean isConnected(){
		return !getChannelGroup().isEmpty();
	}
	
    public String toString(){
    	return "[APRS] receiver@" + Integer.toHexString(System.identityHashCode(this)); 
    }
    
    
	public class PipelineFactory implements ChannelPipelineFactory {
		public ChannelPipeline getPipeline() throws Exception {
			// Create a default pipeline implementation.
			ChannelPipeline pipeline = Channels.pipeline();

			// Add the text line codec combination first,
			pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192,
					Delimiters.lineDelimiter()));
			pipeline.addLast("decoder", new StringDecoder());
			pipeline.addLast("encoder", new StringEncoder());

			// and then business logic.
			pipeline.addLast("handler", new AprsReceiverClientHandler());

			return pipeline;
		}
	}
	
	////////////////////////////////////////////////////////////////////
	// See http://wa8lmf.net/aprs/APRS_symbols.htm
	////////////////////////////////////////////////////////////////////
	private static final String symbolIndexes =  
			"!\"#$%'()*+,-./0" + 
			"123456789:;<=>?@" + 
			"ABCDEFGHIJKLMNOP" +
			"QRSTUVWXYZ[\\]^_`" +
			"abcdefghijklmnop" +
			"qrstuvwxyz{|}~";
	private static String decodeSymbolIndex(char symbolTable, char symbolIndex){
		for(int i = 0;i < symbolIndexes.length();i++){
			char c = symbolIndexes.charAt(i);
			if(c == i){
				if(symbolTable == '/'){
					return Integer.toString(i);
				}else{
					return Integer.toString(i + 96 /*the index in second symbol table, Each table contains 96 icons*/);
				}
			}
		}
		return null;
	}
	/**
	 * The aprs receiver client handler
	 * @author shawn
	 *
	 */
	public class AprsReceiverClientHandler extends SimpleChannelUpstreamHandler {
		@Override
		public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e)
				throws Exception {
			if (e instanceof ChannelStateEvent) {
				log.debug(e.toString());
			}
			super.handleUpstream(ctx, e);
		}

		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
			//FIXME - use proper decoder
			PositionData positionData = parseAPRSPacket(e.getMessage().toString());
			if(positionData != null){
				// update the device position
				try{
					AprsReceiver.this.getTrackerDataService().updateTrackerPosition(positionData);
				}catch(Exception ex){
					log.error("Error update track position", ex);
				}
			}
		}
		
		private PositionData parseAPRSPacket(String aprsMessage){
			PositionData positionData = null;
			if(aprsMessage.startsWith("#")){
				log.debug("RECV CMD: " + aprsMessage);
				return null;
			}
			// log aprs message
			aprsLog.trace(aprsMessage);
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
					}
					
					Position pos = ((PositionPacket)info).getPosition();
					positionData.setLatitude(pos.getLatitude());
					positionData.setLongitude(pos.getLongitude());
					positionData.setAltitude(new Double(pos.getAltitude()));
					
					DataExtension ext = info.getExtension();
					if(ext instanceof CourseAndSpeedExtension){
						CourseAndSpeedExtension csext = (CourseAndSpeedExtension)ext;
						positionData.setSpeed(new Double(csext.getSpeed()));
						positionData.setCourse(new Double(csext.getCourse()));
					}else{
						positionData.setCourse(new Double(0));
						positionData.setSpeed(new Double(0));
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
					aprsData.setSymbol(decodeSymbolIndex(pos.getSymbolTable(),pos.getSymbolCode()));
					
					// PHG info
					if(ext instanceof PHGExtension){
						PHGExtension phg = (PHGExtension)ext;
						aprsData.setHeight(new Integer(phg.getHeight()));
						aprsData.setGain(new Integer(phg.getGain()));
						aprsData.setPower(new Integer(phg.getPower()));
						aprsData.setDirectivity(new Integer(phg.getDirectivity()));
					}
					positionData.addExtendedInfo("aprs",aprsData);
					positionData.setTime(pos.getTimestamp());
					positionData.setValid(true);
					positionData.setAprs(true);
					
					log.debug("ACCEPT: " + aprsMessage);				
					return positionData;
				}
			}catch(Exception e){
				log.info("Invalid aprs message, " + e.getMessage());
			}
			
			return null;
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
			log.warn("Unexpected exception from APRS server downstream.", e.getCause());
			e.getChannel().close();
		}
	}
    
//	public static void main(String[] args) throws Exception {
//		// Print usage if no argument is specified.
//		if (args.length != 2) {
//			System.err.println("Usage: " + AprsReceiver.class.getSimpleName()
//					+ " <host> <port>");
//			return;
//		}
//
//		// Parse options.
//		String host = args[0];
//		int port = Integer.parseInt(args[1]);
//
//		// Configure the client.
//		ClientBootstrap bootstrap = new ClientBootstrap(
//				new NioClientSocketChannelFactory(
//						Executors.newCachedThreadPool(),
//						Executors.newCachedThreadPool()));
//
//		// Configure the pipeline factory.
//		bootstrap.setPipelineFactory(new AprsReceiverClientHandler.PipelineFactory());
//
//		// Start the connection attempt.
//		ChannelFuture future = bootstrap.connect(new InetSocketAddress(host,
//				port));
//
//		// Wait until the connection attempt succeeds or fails.
//		Channel channel = future.awaitUninterruptibly().getChannel();
//		if (!future.isSuccess()) {
//			future.getCause().printStackTrace();
//			bootstrap.releaseExternalResources();
//			return;
//		}
//
//		// Read commands from the stdin.
//		ChannelFuture lastWriteFuture = null;
//		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
//		for (;;) {
//			String line = in.readLine();
//			if (line == null) {
//				break;
//			}
//
//			// Sends the received line to the server.
//			lastWriteFuture = channel.write(line + "\r\n");
//
//			// If user typed the 'bye' command, wait until the server closes
//			// the connection.
//			if (line.toLowerCase().equals("bye")) {
//				channel.getCloseFuture().awaitUninterruptibly();
//				break;
//			}
//		}
//
//		// Wait until all messages are flushed before closing the channel.
//		if (lastWriteFuture != null) {
//			lastWriteFuture.awaitUninterruptibly();
//		}
//
//		// Close the connection. Make sure the close operation ends because
//		// all I/O operations are asynchronous in Netty.
//		channel.close().awaitUninterruptibly();
//
//		// Shut down all thread pools to exit.
//		bootstrap.releaseExternalResources();
//	}
}
