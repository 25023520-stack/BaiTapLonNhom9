package com.auction.system.common.payload;

public class ResponsePayload extends Payload {
    public ResponsePayload() {
        super(PayloadType.RESPONSE);
    }

    public ResponsePayload(boolean success, String message) {
        this();
        setSuccess(success);
        setMessage(message);
    }

    public boolean isSuccess() {
        Object success = getBody().get("success");
        return success instanceof Boolean bool && bool;
    }

    public void setSuccess(boolean success) {
        put("success", success);
    }

    public String getMessage() {
        return getString("message");
    }

    public void setMessage(String message) {
        put("message", message);
    }

    public static ResponsePayload ok(String message) {
        return new ResponsePayload(true, message);
    }

    public static ResponsePayload error(String message) {
        return new ResponsePayload(false, message);
    }
}
