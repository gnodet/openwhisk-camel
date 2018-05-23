package org.jboss.fuse.openwhisk.camel.feed.provider.service;

import java.nio.charset.Charset;
import java.util.Base64;

public class Utils {

    public static String shortTriggerID(String triggerName) {
        if (triggerName != null && triggerName.contains("/")) {
            String[] triggerNameArray = triggerName.split("/");
            return triggerNameArray[triggerNameArray.length - 1];
        }

        return triggerName;
    }

    public static String base64Encoded(String text) {
        byte[] encodedText = Base64.getEncoder().encode(
                text.getBytes(Charset.forName("US-ASCII")));
        return new String(encodedText);
    }

    public static String base64Decode(String text) {
        byte[] decodedText = Base64.getDecoder().decode(
                text.getBytes(Charset.forName("US-ASCII")));
        return new String(decodedText);
    }
}
