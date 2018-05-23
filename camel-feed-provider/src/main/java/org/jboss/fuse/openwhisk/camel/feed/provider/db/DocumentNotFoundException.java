package org.jboss.fuse.openwhisk.camel.feed.provider.db;

public class DocumentNotFoundException extends Exception {
    private final String error;
    private final String reason;

    public DocumentNotFoundException(String error, String reason) {
        super(error);
        this.error = error;
        this.reason = reason;
    }
}
