package io.sophone.sdk.wechat.message.impl;

import io.sophone.sdk.wechat.message.arch.PushJSONFormat;
import io.sophone.sdk.wechat.message.arch.ReplyXMLFormat;
import io.sophone.sdk.wechat.message.arch.TextMessage;
import net.sf.json.JSONObject;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public class OutgoingTextMessage extends TextMessage implements ReplyXMLFormat, PushJSONFormat {

    @Override
    public String toReplyXMLString() {
        Element root = DocumentHelper.createElement("xml");
        root.addElement("ToUserName").addCDATA(getToUserName());
        root.addElement("FromUserName").addCDATA(getFromUserName());
        root.addElement("CreateTime").addCDATA(getCreateTime() + "");
        root.addElement("MsgType").addCDATA(getMsgType());
        root.addElement("Content").addCDATA(getContent());
        return root.asXML();
    }

    @Override
    public String toPushJSONString() {
        JSONObject json = new JSONObject();
        json.put("touser", getToUserName());
        json.put("msgtype", getMsgType());
        JSONObject contentObj = new JSONObject();
        contentObj.put("content", getContent());
        json.put("text", contentObj);
        return json.toString();
    }

}
