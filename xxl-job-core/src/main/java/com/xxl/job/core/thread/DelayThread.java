package com.xxl.job.core.thread;

import com.xxl.job.core.biz.model.DelayTaskParam;
import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import com.xxl.job.core.context.XxlJobContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.log.XxlJobFileAppender;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;


/**
 * handler thread
 * @author xuxueli 2016-1-16 19:52:47
 */
public class DelayThread extends Thread{
	private static Logger logger = LoggerFactory.getLogger(DelayThread.class);

	private static DelayThread delayThread = new DelayThread();
	private LinkedBlockingQueue<DelayTaskParam> triggerQueue = new LinkedBlockingQueue<>();
	private ThreadPoolExecutor delayExecutor;

	private volatile boolean toStop = false;
	private String stopReason;

    private volatile boolean running = false;    // if running job
	private int idleTimes = 0;			// idel times
	private final Object LOCK = new Object();

	public static DelayThread getInstance(){
		return delayThread;
	}

    /**
     * new trigger to queue
     *
     * @param delayTaskParam
     * @return
     */
	public ReturnT<String> pushTriggerQueue(DelayTaskParam delayTaskParam) {
		if(!running){
			synchronized (LOCK){
				if(!running){
					running = true;
					delayExecutor = new ThreadPoolExecutor(5,
							20,
							60,
							TimeUnit.SECONDS,
							new LinkedBlockingQueue<>(2000),
							r -> new Thread(r, "xxl-job-delay, DelayThread-threadPool-" + r.hashCode()));
					delayThread.start();
				}
			}
		}
		triggerQueue.add(delayTaskParam);
        return ReturnT.SUCCESS;
	}

    /**
     * kill job thread
     *
     * @param stopReason
     */
	public void toStop(String stopReason) {
		/**
		 * Thread.interrupt只支持终止线程的阻塞状态(wait、join、sleep)，
		 * 在阻塞出抛出InterruptedException异常,但是并不会终止运行的线程本身；
		 * 所以需要注意，此处彻底销毁本线程，需要通过共享变量方式；
		 */
		this.toStop = true;
		this.stopReason = stopReason;
	}

    /**
     * is running job
     * @return
     */
    public boolean isRunningOrHasQueue() {
        return running || triggerQueue.size()>0;
    }

    @Override
	public void run() {
		while (!toStop){
			try {
				DelayTaskParam task = triggerQueue.take();
				delayExecutor.execute(()->{
					try {
						String executorHandler = task.getExecutorHandler();
						IJobHandler handler = XxlJobExecutor.loadJobDelayHandler(executorHandler);
						handler.init();
						XxlJobContext xxlJobContext = new XxlJobContext(task.getTaskId(), task.getExecutorParams(), null, 0, 0);
						XxlJobContext.setXxlJobContext(xxlJobContext);
						handler.execute();
						handler.destroy();
						//回写成功结果
						DelayTaskCallbackThread.pushCallBack(new HandleCallbackParam(
								task.getTaskId(),
								0,
								XxlJobContext.HANDLE_CODE_SUCCESS,
								null)
						);
					} catch (Exception e) {
						//回写失败结果
						DelayTaskCallbackThread.pushCallBack(new HandleCallbackParam(
								task.getTaskId(),
								0,
								XxlJobContext.HANDLE_CODE_FAIL,
								stopReason + " [job not executed, in the job queue, killed.]")
						);
					}
				});
			} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
			}
		}

		// callback trigger request in queue
		while(triggerQueue != null && triggerQueue.size() > 0){
			DelayTaskParam taskParam = triggerQueue.poll();
			if (taskParam!=null) {
				// is killed
				DelayTaskCallbackThread.pushCallBack(new HandleCallbackParam(
						taskParam.getTaskId(),
						0,
						XxlJobContext.HANDLE_CODE_FAIL,
						stopReason + " [job not executed, in the job queue, killed.]")
				);
			}
		}

		logger.info(">>>>>>>>>>> xxl-job-delay JobThread stoped, hashCode:{}", Thread.currentThread());
	}
}
