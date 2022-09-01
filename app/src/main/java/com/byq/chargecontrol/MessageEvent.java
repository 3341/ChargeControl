package com.byq.chargecontrol;

public class MessageEvent {
    public static final int TRY_CALL_SERVICE = 63;
    public static final int TRY_RESPONSE_ACTIVTY = 991;
    public static final int UPDATE_CONFIG_FILE = 213;
    private int eventId;
    private Object data;
    private Object dataArray;

    public MessageEvent(int eventId) {
        this.eventId = eventId;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Object getDataArray() {
        return dataArray;
    }

    public void setDataArray(Object dataArray) {
        this.dataArray = dataArray;
    }
}
