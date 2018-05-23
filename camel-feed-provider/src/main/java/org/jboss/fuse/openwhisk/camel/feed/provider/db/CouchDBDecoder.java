package org.jboss.fuse.openwhisk.camel.feed.provider.db;

import java.io.IOException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import feign.FeignException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CouchDBDecoder implements ErrorDecoder {

    private Logger log = LoggerFactory.getLogger(CouchDBDecoder.class);

    public Exception decode(String methodKey, Response response) {
        try {
            JsonObject jsonObject = new JsonParser().parse(response.body().asReader()).getAsJsonObject();
            log.info("Response: " + jsonObject);
            if (jsonObject == null || !jsonObject.has("error")) {
                return FeignException.errorStatus(methodKey, response);
            }
            if (response.status() == 404) {
                String error = jsonObject.get("error").getAsString();
                String reason = jsonObject.get("reason").getAsString();
                if ("not_found".equalsIgnoreCase(error)) {
                    if ("missing".equalsIgnoreCase(reason) || "deleted".equalsIgnoreCase(reason)) {
                        return new DocumentNotFoundException(error, reason);
                    } else if ("Database does not exist.".equalsIgnoreCase(reason)) {
                        return new DBDoesNotExistsException(error, reason);
                    }
                }

            } else if (response.status() == 412) {
                String error = jsonObject.get("error").getAsString();
                String reason = jsonObject.get("reason").getAsString();
                if ("file_exists".equalsIgnoreCase(error)) {
                    return new DBAlreadyExistsException(error, reason);
                }
            }
            return FeignException.errorStatus(methodKey, response);
        } catch (IOException e) {
            return FeignException.errorStatus(methodKey, response);
        }
    }

}
