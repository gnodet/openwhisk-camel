package org.jboss.fuse.openwhisk.camel.feed.provider.db;

import com.google.gson.JsonObject;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

/**
 *
 */
public interface CouchDBClient {

    @RequestLine("PUT /{db}")
    JsonObject createDB(@Param("db") String db) throws DBAlreadyExistsException;

    @RequestLine("DELETE /{db}")
    JsonObject deleteDB(@Param("db") String db) throws DBDoesNotExistsException;

    @RequestLine("POST /{db}/_find")
    @Headers("Content-Type: application/json")
    JsonObject allDocs(@Param("db") String db, JsonObject request);

    @RequestLine("PUT /{db}/{docid}")
    @Headers("Content-Type: application/json")
    JsonObject saveDoc(@Param("db") String db,
                       @Param("docid") String docid,
                       JsonObject doc);

    @RequestLine("DELETE /{db}/{docid}?rev={rev}")
    @Headers("Content-Type: application/json")
    JsonObject deleteDoc(@Param("db") String db,
                         @Param("docid") String docid,
                         @Param("rev") String rev);

    @RequestLine("GET /{db}/{docid}")
    @Headers("Content-Type: application/json")
    JsonObject getDocumentById(@Param("db") String db,
                               @Param("docid") String docid) throws DocumentNotFoundException;
}
