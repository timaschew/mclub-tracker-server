package mclub.push

class PushMessage {
    public static final int STATUS_NEW = 0;
    public static final int STATUS_PENDING = 1;
    public static final int STATUS_SENT = 2;
    public static final int STATUS_DROPPED = 3;
    public static final int DEFAULT_RETRY_COUNT = 3;

    static constraints = {
        uuid blank:true,nullable:true
        title blank:true,nullable:true
        deviceToken blank:true,nullable:true
        updateTime blank:true,nullable:true
        extension blank:true,nullable:true
        retryCount blank:true,nullable:true
    }

    static mapping = {
        body	type:'text'
        extension type:'text'
        uuid index:'idx_uuid_status_updatetime'
        status index:'idx_uuid_status_updatetime'
        updateTime index:'idx_uuid_status_updatetime'
    }

    String uuid;
    String deviceToken;
    Date creationTime;
    Date updateTime;

    String title;
    String body;
    String extension;
    int status;
    int retryCount;  // error retry count, should be moved to BulkPushMessageTask in future
}
