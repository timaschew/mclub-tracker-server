package mclub.sys

import grails.transaction.Transactional

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

class TaskService {
	boolean lazyInit = false

	private static Object lock = new Object();
	private ExecutorService taskThreads;
	private boolean stopFlag;
	@PostConstruct
	public void start(){
		stopFlag = false;
		taskThreads = Executors.newCachedThreadPool(new NamedThreadFactory("TaskService"));
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

//	public void schedule(final Task task, final long taskBusyWaitIntervalMs, final long taskIdleWaitIntervalMs){
//
//	}

	public void schedule(final Task task, final long taskSleepIntervalMs){
		assert(task != null);
		assert(taskSleepIntervalMs > 0);
		taskThreads.execute(new Runnable(){
			public void run(){
				while(!stopFlag){
					if(task.run()){
						// task.run() returns true means the task is done.
						// stop running anymore
						break;
					}
					synchronized(lock){
						lock.wait(taskSleepIntervalMs);
					}
				}
			}
		});
	}

	/**
	 * Task interface
	 */
	public static interface Task{
		/**
		 * task body
		 * @return false for run again, true for task completion and task service will not not run it anymore.
		 */
		public abstract boolean run();
	}
}
