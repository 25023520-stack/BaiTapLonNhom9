package com.auction.system.common.payload;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Payload implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private PayloadType type;
    private Map<String, Object> body = new HashMap<>();

    public Payload() {
    }

    public Payload(PayloadType type) {
        this.type = type;
    }

    public Payload(PayloadType type, Map<String, Object> body) {
        this.type = type;
        this.body = body != null ? new HashMap<>(body) : new HashMap<>();
    }

    public PayloadType getType() {
        return type;
    }

    public void setType(PayloadType type) {
        this.type = type;
    }

    public Map<String, Object> getBody() {
        return body;
    }

    public void setBody(Map<String, Object> body) {
        this.body = body != null ? new HashMap<>(body) : new HashMap<>();
    }

    public Payload put(String key, Object value) {
        body.put(key, value);
        return this;
    }

    public String getString(String key) {
        Object value = body.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    public Integer getInt(String key) {
        Object value = body.get(key);
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return value != null ? Integer.parseInt(String.valueOf(value)) : null;
    }

    public Double getDouble(String key) {
        Object value = body.get(key);
        if (value instanceof Double doubleValue) {
            return doubleValue;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return value != null ? Double.parseDouble(String.valueOf(value)) : null;
    }

    public Boolean getBoolean(String key) {
        Object value = body.get(key);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return value != null ? Boolean.parseBoolean(String.valueOf(value)) : null;
    }
}
