package org.collectd.protocol;

import org.collectd.api.Notification;
import org.collectd.api.ValueList;

import java.util.logging.Logger;

class DispatchLogger implements Dispatcher {
    protected Logger getLog() {
        return Logger.getLogger(getClass().getName());
    }
    public void dispatch(ValueList values) {
        getLog().info(values.toString());
    }

    public void dispatch(Notification notification) {
        getLog().info(notification.toString());
    }
}