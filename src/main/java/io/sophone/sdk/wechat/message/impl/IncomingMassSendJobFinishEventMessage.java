package io.sophone.sdk.wechat.message.impl;

import io.sophone.sdk.wechat.message.arch.EventMessage;

/**
 * @author eyakcn
 * @since 4/29/15 AD
 */
public class IncomingMassSendJobFinishEventMessage extends EventMessage {
    private String msgId;
    private String status;
    private int totalCount;
    private int filterCount;
    private int sentCount;
    private int errorCount;

    @Override
    public String getEvent() {
        return "MASSSENDJOBFINISH";
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getFilterCount() {
        return filterCount;
    }

    public void setFilterCount(int filterCount) {
        this.filterCount = filterCount;
    }

    public int getSentCount() {
        return sentCount;
    }

    public void setSentCount(int sentCount) {
        this.sentCount = sentCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }
}
