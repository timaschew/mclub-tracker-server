package mclub.sys;

/**
 * Message receiver  
 * @author shawn
 *
 */
public interface MessageListener {
	/**
	 * message receive callback
	 * @param message
	 */
	public void onMessageReceived(Object message);
}
