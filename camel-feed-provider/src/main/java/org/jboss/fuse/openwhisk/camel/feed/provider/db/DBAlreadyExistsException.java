package org.jboss.fuse.openwhisk.camel.feed.provider.db;

public class DBAlreadyExistsException extends Exception {
    private final String error;
    private final String reason;

    public DBAlreadyExistsException(String error, String reason) {
        super(error);
        this.error = error;
        this.reason = reason;
    }
}
