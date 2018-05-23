package org.jboss.fuse.openwhisk.camel.feed.provider.service;


import java.util.Map;

public class TriggerData {

    private String authKey;
    private String routeUrl;
    private Map<String, String> mapping;
    private boolean binary;
    private String triggerName;
    private String triggerShortName;
    private String _rev;

    public TriggerData() {
    }

    public String getAuthKey() {
        return authKey;
    }

    public void setAuthKey(String authKey) {
        this.authKey = authKey;
    }

    public String getRouteUrl() {
        return routeUrl;
    }

    public void setRouteUrl(String routeUrl) {
        this.routeUrl = routeUrl;
    }

    public Map<String, String> getMapping() {
        return mapping;
    }

    public void setMapping(Map<String, String> mapping) {
        this.mapping = mapping;
    }

    public boolean isBinary() {
        return binary;
    }

    public void setBinary(boolean binary) {
        this.binary = binary;
    }

    public String getTriggerName() {
        return triggerName;
    }

    public void setTriggerName(String triggerName) {
        this.triggerName = triggerName;
    }

    public String getTriggerShortName() {
        return triggerShortName;
    }

    public void setTriggerShortName(String triggerShortName) {
        this.triggerShortName = triggerShortName;
    }

    public String getRevision() {
        return _rev;
    }

    public void setRevision(String revision) {
        this._rev = revision;
    }

    @Override
    public String toString() {
        return "TriggerData{" +
                "authKey='" + authKey + '\'' +
                ", routeUrl='" + routeUrl + '\'' +
                ", mapping=" + mapping +
                ", binary=" + binary +
                ", triggerName='" + triggerName + '\'' +
                ", triggerShortName='" + triggerShortName + '\'' +
                ", _rev='" + _rev + '\'' +
                '}';
    }
}
