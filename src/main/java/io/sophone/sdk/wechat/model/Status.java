package io.sophone.sdk.wechat.model;

import java.io.Serializable;

/**
 * @author eyakcn
 * @since 4/23/15 AD
 */
public class Status implements Serializable {
    private static final long serialVersionUID = 1L;

    public int errcode;
    public String errmsg;

    public Status() {
        this.errcode = 0;
        this.errmsg = "";
    }

    public Status(int errcode, String errmsg) {
        this.errcode = errcode;
        this.errmsg = errmsg;
    }

    public void setError(int errcode, String errmsg) {
        this.errcode = errcode;
        this.errmsg = errmsg;

    }

    public boolean isSuccess() {
        return this.errcode == 0;
    }
}
