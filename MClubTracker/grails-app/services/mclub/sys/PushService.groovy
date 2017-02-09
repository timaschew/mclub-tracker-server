package mclub.sys

import com.relayrides.pushy.apns.ApnsClient
import com.relayrides.pushy.apns.ApnsClientBuilder
import com.relayrides.pushy.apns.ApnsPushNotification
import com.relayrides.pushy.apns.PushNotificationResponse
import com.relayrides.pushy.apns.util.ApnsPayloadBuilder
import com.relayrides.pushy.apns.util.SimpleApnsPushNotification
import com.relayrides.pushy.apns.util.TokenUtil
import grails.transaction.Transactional
import io.netty.util.concurrent.Future
import io.netty.util.concurrent.GenericFutureListener
import mclub.push.PushMessage
import mclub.tracker.TrackerDevice

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

class PushService {
    private Object lock = new Object();
    private boolean shutdownFlag = false;

    boolean lazyInit = false
    TaskService taskService;
    ConfigService configService;
    ApnsClient apnsClient;
    private Future connectFuture;

    volatile int connectionState; // 0:not connected, 1:connecting, 2:connected
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    @PostConstruct
    public void start(){
        doConnect();

        // delay start to avoid "TX is not read" issue.
        new Thread(new Runnable(){
            @Override
            void run() {
                Thread.sleep(5000);
                startConsumerTask();
            }
        }).start();

        log.info("PushService started");
    }

    @PreDestroy
    public void stop(){
        shutdownFlag = true;
        synchronized (lock){
            lock.notifyAll();
        }
        doDisconnect();
        log.info("PushService stopped");
    }

    private boolean isConnected(){
        return connectionState == STATE_CONNECTED;
    }

    private void startConsumerTask(){
        // The task will be scheduled in the TaskService and
        // run() every 500ms
        // until we returns true indicates task is completed.
        taskService.schedule(new TaskService.Task(){
            @Override
            boolean run() {
                if(shutdownFlag) {
                    return true;
                }
                boolean busy = false;
                PushMessage.withTransaction {
                    busy = _doConsumeTask();
                }
                if(!busy){
                    log.debug("Push consume task will sleep");
                    synchronized(lock){
                        lock.wait(30000);
                    }
                }
                return false
            }
        },500);
        log.debug("Push consume task started");
    }

    /**
     * Fetch messages to push and update status
     * @return true if we have something to do or false if nothing to do
     */
    public boolean _doConsumeTask(){
        log.debug("Push consume task working");
        boolean idle = false,busy = true;
        def messages = _dequeuePushMessages(10);
        if(messages.size() == 0) {
            return idle /* idle flag */;
        }

        def sent = [];
        def fail = [];
        messages?.each{ msg ->
            if(doPush(msg,true)){ //synchronized push, will return when push server really returns.
                sent << msg;
            }else{
                fail << msg;
            }
        }
        // bulk update success messages
        this.updatePushMessageStatus(sent,PushMessage.STATUS_SENT);

        // update failed messages
        fail.each{ failMsg ->
            if(++failMsg.retryCount >= PushMessage.DEFAULT_RETRY_COUNT){
                log.info("Message ${failMsg.id} is dropped due to reach max retry count ${failMsg.retryCount}");
                failMsg.status = PushMessage.STATUS_DROPPED;
            }else{
                failMsg.status = PushMessage.STATUS_NEW;
            }
            failMsg.save(flush:true);
        }
        log.debug("${sent.size()} messages sent, ${fail.size()} messages failed");
        return busy /* busy */;
    }

    /**
     * Connect to the server
     * @return
     */
    private Future doConnect(){
        if(connectionState == STATE_CONNECTED){
            return null;
        }else if(connectionState == STATE_CONNECTING){
            return connectFuture;
        }
        if(apnsClient == null){
            try {
                apnsClient = new ApnsClientBuilder().build();
                apnsClient.registerSigningKey(new File("/tmp/apns_key.p8"),
                        configService.getConfigString('push.apns.teamId'),
                        configService.getConfigString('push.apns.key'),
                        "bg5hhp.mtracker");
            }catch(Throwable e){
                log.error("Error initialize ApnsClient",e);
                return null;
            }
        }

        connectionState = STATE_CONNECTING;
        Future f = apnsClient.connect(ApnsClient.DEVELOPMENT_APNS_HOST);
        f.addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            void operationComplete(Future<? super Void> future) throws Exception {
                if(future.isSuccess()) {
                    connectionState = STATE_CONNECTED;
                    connectFuture = null;
                    log.info("APNS server connected");
                }else {
                    connectionState = STATE_DISCONNECTED;
                    connectFuture = null;
                    log.info("APNS server connect failed", future.cause())
                }
            }
        })
        connectFuture = f;
        return f;
    }

    /**
     * Disconnect
     */
    private void doDisconnect(){
        Future f = null;
        if(apnsClient != null){
            try{
                f = apnsClient.disconnect();
                f.await(2000);
            }catch(Exception e){
                // noop
            }
            apnsClient = null;
        }
        connectionState = STATE_DISCONNECTED;
    }

    /**
     * Bulk update push message state
     * @param messages
     * @param status
     */
    private int updatePushMessageStatus(List<PushMessage> messages, int status){
        if(messages?.size() == 0){
            return 0
        };
        def ids = [];
        messages.each{ msg ->
            ids << msg.id;
        }
        String hql = "UPDATE PushMessage pm SET pm.status=:status WHERE pm.id IN :ids";
        int count = PushMessage.executeUpdate(hql,[ids:ids,status:status]);
        return count;
    }

    /**
     * Call the push API and returns the status
     * @return
     */
    private boolean doPush(PushMessage message,boolean waitOnComplete){
        if(!isConnected()){
            Future f = doConnect(); // doConnect will always returns the connect future
            if(f){
                if(!f.await(5000)){
                    //
                    log.info("wait connect time out, send aborted");
                    return false;
                }
            }
        }

        if(!isConnected()){
            // possible runs here if apns cient is not initialized (e.g ALPN-boot is not found)
            log.info("Push server is not connected yet, task aborted");
            return false;
        }

        log.debug("pushing...")
        final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
        payloadBuilder.setAlertBody(message.body);
        payloadBuilder.setAlertTitle(message.title);
        payloadBuilder.setSoundFileName("chime.aiff");

        final String token = message.deviceToken;
        final String payload = payloadBuilder.buildWithDefaultMaximumLength();
        ApnsPushNotification pushNotification = new SimpleApnsPushNotification(token, "bg5hhp.mtracker", payload);

        final Future<PushNotificationResponse<SimpleApnsPushNotification>> sendFuture =
                apnsClient.sendNotification(pushNotification);

        if(waitOnComplete){
            sendFuture.await(10000);
            if(sendFuture.isSuccess()){
                log.debug("Push message[${message.id}] success");
                return true;
            }else{
                sendFuture.cause()
                log.debug("Push message[${message.id}] failed, cause: ${sendFuture.cause().message}");
            }
            return false;
        }else{
            sendFuture.addListener(new GenericFutureListener<Future<? super PushNotificationResponse<SimpleApnsPushNotification>>>() {
                @Override
                void operationComplete(Future<? super PushNotificationResponse<SimpleApnsPushNotification>> future) throws Exception {
                    if(future.isSuccess()){
                        log.info("message[${message.id}] sent success");
                        message.status = PushMessage.STATUS_SENT;
                    }else{
                        if(++message.retryCount >= PushMessage.DEFAULT_RETRY_COUNT){
                            // reaches max retry count, mark as dropped
                            message.status = PushMessage.STATUS_DROPPED;
                            log.info("Message ${message.id} is dropped due to reach max retry count ${message.retryCount}");
                        }else{
                            // reset the status for next round of send.
                            message.status = PushMessage.STATUS_NEW;
                        }
                        log.info("message[${message.id}] sent failed, cause: ${future.cause()?.message}");
                    }
                    message.save(flush:true);
                }
            });
            return true;
        }
    }

    /**
     * Register device with push token, only update when push token really changes
     * @param udid
     * @param pushToken
     */
    public void registerDevice(String udid, String pushToken){
        String hql = "UPDATE TrackerDevice d SET d.pushToken=:pushToken WHERE d.udid=:udid AND d.pushToken <> :pushToken";
        int recordsUpdated = TrackerDevice.executeUpdate(hql,[pushToken:pushToken,udid:udid]);
        log.info("Device register push token, ${recordsUpdated} records affected.");
    }

    /**
     * Currently only dequeue message one by one ?
     * @return
     */
    private List<PushMessage> _dequeuePushMessages(int limit){
        // get new messages
        String hql = "FROM PushMessage pm WHERE pm.status=:status AND pm.deviceToken IS NOT NULL ORDER by pm.id ASC";
        def messages = PushMessage.executeQuery(hql,[status:PushMessage.STATUS_NEW,max:limit]);
        log.debug("dequeued ${messages.size()} new messages");
        if(messages.size() > 0){
            // update to pending and return
            int count = updatePushMessageStatus(messages,PushMessage.STATUS_PENDING);
            log.debug("armed ${count} messages to PENDING state");
        }
        return messages;
    }

    /**
     * Enqueue message to be pushed
     * @param title
     * @param body
     * @param deviceToken could be null
     * @return true if success or false if failed.
     */
    @Transactional
    public boolean enqueuePushMessage(String title, String body,String deviceToken){
        PushMessage pm = new PushMessage(
                title:title,
                body:body,
                deviceToken:deviceToken,
                uuid:UUID.randomUUID(),
                status:PushMessage.STATUS_NEW,
                creationTime: new java.util.Date()
        );
        pm.save();
        if(pm.hasErrors()){
            log.error("Error saving PushMessage, errors: " + pm.getErrors());
            return false;
        }

        // awaken the consume task
        synchronized(lock){
            lock.notifyAll();
        }

        return true;
    }

    def testPush(String msg) {
        if(!isConnected()){
            Future f = doConnect(); // doConnect will always returns the connect future
            if(f){
                if(!f.await(5000)){
                    //
                    log.info("wait connect time out, send aborted");
                    return;
                }
            }
        }
        if(!isConnected()){
            // possible runs here if apns cient is not initialized (e.g ALPN-boot is not found)
            log.info("Push server is not connected yet, task aborted");
            return;
        }

        log.debug("pushing...")
        final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
        if(!msg)msg="This is a test";
        payloadBuilder.setAlertBody(msg);
        payloadBuilder.setSoundFileName("chime.aiff");

        final String payload = payloadBuilder.buildWithDefaultMaximumLength();
        final String token = 'C238504B83ACC40CA0DB51BA057E87D9F7F8C97D115CCD702454192CFC75992B'//TokenUtil.sanitizeTokenString("<efc7492 bdbd8209>");

        ApnsPushNotification pushNotification = new SimpleApnsPushNotification(token, "bg5hhp.mtracker", payload);


        final Future<PushNotificationResponse<SimpleApnsPushNotification>> sendNotificationFuture =
                apnsClient.sendNotification(pushNotification);

        sendNotificationFuture.addListener(new GenericFutureListener<Future<? super PushNotificationResponse<SimpleApnsPushNotification>>>() {
            @Override
            void operationComplete(Future<? super PushNotificationResponse<SimpleApnsPushNotification>> future) throws Exception {
                if(future.isSuccess()){
                    log.info("Push sent success");
                }else{
                    log.info("Push sent failed, ${future.cause().message}");
                }
            }
        })
    }
}
