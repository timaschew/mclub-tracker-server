package mclub.sys

import grails.transaction.Transactional

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

class TaskService {
	private static Object lock = new Object();
	private ExecutorService taskThreads;
	private boolean stopFlag;
	@PostConstruct
	public void start(){
		stopFlag = false;
		taskThreads = Executors.newCachedThreadPool(new DefaultThreadFactory());
		log.info("TaskService started");
	}
	
	@PreDestroy
	public void stop(){
		synchronized(lock){
			stopFlag = true;
			lock.notifyAll();
		}
		taskThreads.shutdown();
		log.info("TaskService stopped");
	}
	
	public void execute(final Runnable task, final long taskSleepIntervalMs){
		assert(task != null);
		assert(taskSleepIntervalMs > 0);
		taskThreads.execute(new Runnable(){
			public void run(){
				while(!stopFlag){
					task.run();
					synchronized(lock){
						lock.wait(taskSleepIntervalMs);
					}
				}
			}
		});
	}
	
	/**
	 * The default thread factory
	 */
	static class DefaultThreadFactory implements ThreadFactory {
		private final ThreadGroup group;
		private final AtomicInteger threadNumber = new AtomicInteger(1);
		private final String namePrefix;

		DefaultThreadFactory() {
			SecurityManager s = System.getSecurityManager();
			group = (s != null) ? s.getThreadGroup() :
								  Thread.currentThread().getThreadGroup();
			namePrefix = "TaskService-thread-";
		}

		public Thread newThread(Runnable r) {
			Thread t = new Thread(group, r,
								  namePrefix + threadNumber.getAndIncrement(),
								  0);
			if (t.isDaemon())
				t.setDaemon(false);
			if (t.getPriority() != Thread.NORM_PRIORITY)
				t.setPriority(Thread.NORM_PRIORITY);
			return t;
		}
	}
}
