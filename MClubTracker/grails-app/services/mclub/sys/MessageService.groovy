package mclub.sys

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

public class MessageService {
	private static Object lock = new Object();
	private ExecutorService notifyThread;
	private boolean stopFlag;
	private Set<MessageListener> listeners;
	
	@PostConstruct
	public void start(){
		notifyThread = Executors.newFixedThreadPool(1,new NamedThreadFactory("MessageService"));
		listeners = new LinkedHashSet<MessageListener>();
		stopFlag = false;
		log.info("MessageService started");
	}
	
	@PreDestroy
	public void stop(){
		synchronized(lock){
			stopFlag = true;
			lock.notifyAll();
		}
		notifyThread.shutdown();
		log.info("MessageService stopped");
	}

	public void addListener(MessageListener listener){
		listeners.add(listener);
	}
	
	public void removeListener(MessageListener listener){
		listeners.remove(listener);
	}

	public void postMessage(Object message){
		if(stopFlag || listeners.isEmpty()){
			// we're stopping or no listeners yet.
			return;
		}
		notifyThread.execute(new Runnable(){
			public void run(){
				for(MessageListener l : listeners){
					l.onMessageReceived(message);
				}
			};
		});
	}
}
