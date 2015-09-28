package mclub.tracker.aprs;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import mclub.tracker.PositionData;
import mclub.tracker.TrackerDataService;

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
import org.jboss.netty.channel.SimpleChannelHandler;
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
	private int state;
	
	private static final int STATE_INIT = 0;
	private static final int STATE_CONNECTING = 1;
	private static final int STATE_CONNECTED = 2;
	private static final int STATE_DESTROY = 3;
	
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
		
		state = STATE_DESTROY;
		
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
	
	private String getConfigString(String key){
		return (String)trackerDataService.getConfig(key);
	}
	
//	private int getConfigInt(String key){
//		return (Integer)trackerDataService.getConfig(key);
//	}
	
	private void initReceiver(){
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
 	 		doConnect();
 		}catch(Exception e){
 			log.error("Error connect APRS server", e);
			releaseNettyResources();
 		}
	}
	
	private boolean doConnect() throws Exception{
		InetSocketAddress aprsServerAddr;
		address = (String)trackerDataService.getConfig("tracker." + protocol + ".address");
        port = (Integer)trackerDataService.getConfig("tracker." + protocol + ".port");
        if (address == null) {
        	aprsServerAddr = new InetSocketAddress(port);
        } else {
        	aprsServerAddr = new InetSocketAddress(address, port);
        }

		ChannelFuture future = bootstrap.connect(aprsServerAddr);
		// Wait until the connection attempt succeeds or fails.
		Channel channel = future.awaitUninterruptibly().getChannel();
		if (future.isSuccess()) {
			// store the channels
			getChannelGroup().add(channel);
			log.debug("Connected to APRS server " + address + ":" + port);
			
			return true;
		}else{
			log.error("Error connect APRS server", future.getCause());
			return false;
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
			pipeline.addLast("aprsDecoder",new AprsDecoder(
					getConfigString("tracker.aprs.call"),
					getConfigString("tracker.aprs.pass"),
					getConfigString("tracker.aprs.filter")
					));

			// and then business logic.
			pipeline.addLast("handler", new AprsReceiverClientHandler());

			return pipeline;
		}
	}
	
    /**
     * Open channel handler
     */
    protected class OpenChannelHandler extends SimpleChannelHandler {
        @Override
        public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
            getChannelGroup().add(e.getChannel());
        }
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
			//See aprsDecoder
			Object obj = e.getMessage();
			if(obj instanceof PositionData){
				try{
					PositionData positionData = (PositionData)obj;
					AprsReceiver.this.getTrackerDataService().updateTrackerPosition(positionData);
				}catch(Exception ex){
					log.error("Error update track position", ex);
				}				
			}
		}
		
		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
			log.warn("Exception from APRS server downstream.", e.getCause());
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
