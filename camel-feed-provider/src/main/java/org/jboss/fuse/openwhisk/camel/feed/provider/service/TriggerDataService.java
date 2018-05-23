package org.jboss.fuse.openwhisk.camel.feed.provider.service;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jboss.fuse.openwhisk.camel.feed.provider.db.CouchDBClient;
import org.jboss.fuse.openwhisk.camel.feed.provider.db.DBAlreadyExistsException;
import org.jboss.fuse.openwhisk.camel.feed.provider.db.DocumentNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TriggerDataService {

    private static final String[] SELECTOR_FIELDS = {"authKey", "routeUrl", "mapping", "binary", "triggerName", "triggerShortName"};

    private final Logger log = LoggerFactory.getLogger(TriggerDataService.class);
    private final CouchDBClient couchDBClient;
    private final Gson gson;

    @Value("${DATABASE}")
    private String database;

    public TriggerDataService(CouchDBClient couchDBClient, Gson gson) {
        this.couchDBClient = couchDBClient;
        this.gson = gson;
    }

    @PostConstruct
    protected void init() {
        try {
            JsonObject response = couchDBClient.createDB(database);
            Objects.equals(response.get("ok").getAsBoolean(), true);
        } catch (DBAlreadyExistsException e) {
            log.info("DB {} already exists, skipping creation", database);
        }
    }

    /**
     * @param triggerData
     * @return
     */
    public boolean saveOrUpdate(TriggerData triggerData) {
        String docId = Utils.base64Encoded(triggerData.getTriggerName());
        JsonObject doc = null;
        log.info("Saving Document {} with ID {}", triggerData, docId);
        Optional<TriggerData> optionalTriggerData = getDocumentById(docId);
        if (optionalTriggerData.isPresent()) {
            String revision = optionalTriggerData.get().getRevision();
            triggerData.setRevision(revision);
            doc = couchDBClient.saveDoc(database, docId, (JsonObject) new Gson().toJsonTree(triggerData));
            log.info("Updated Document {}", doc);
        } else {
            doc = couchDBClient.saveDoc(database, docId, (JsonObject) new Gson().toJsonTree(triggerData));
            log.info("Saved Document {}", doc);
        }
        return doc != null && doc.get("ok").getAsBoolean();
    }

    /**
     * @param docId
     * @return
     */
    public boolean deleleteDoc(String docId) {
        Optional<TriggerData> optionalTriggerData = getDocumentById(docId);
        if (optionalTriggerData.isPresent()) {
            String revision = optionalTriggerData.get().getRevision();
            JsonObject response = couchDBClient.deleteDoc(database, docId, revision);
            return response != null && response.get("ok").getAsBoolean();
        } else {
            log.warn("Document with ID {} not found in DB {}", docId, database);
            return false;
        }
    }

    /**
     * @param docId
     * @return
     */
    public Optional<TriggerData> getDocumentById(String docId) {
        log.info("Getting Document with ID {} ", docId);
        TriggerData triggerData = null;
        try {
            JsonObject doc = couchDBClient.getDocumentById(database, docId);
            log.info("Document Retrieved {}", doc);
            if (doc != null) {
                triggerData = gson.fromJson(doc, TriggerData.class);
            }
        } catch (DocumentNotFoundException e) {
            log.warn("Document with ID {} not found in DB {}", docId, database);
        }
        return Optional.ofNullable(triggerData);
    }

    /**
     * TODO - need to optimize for getting reactively with backpressure
     *
     * @return
     */
    public Stream<TriggerData> findAll() {
        log.info("Finding All Documents");
        String regEx = "^(.*)";
        String fieldName = "triggerName";
        JsonObject request = requestSelector(fieldName, regEx);
        JsonObject response = couchDBClient.allDocs(database, request);
        return extractDocs(response, "Got {} documents");
    }

    public Stream<TriggerData> findAllByDestination(String destinationName) {
        log.info("Finding All triggers for Destination {} ", destinationName);
        String fieldName = "triggerName";
        JsonObject request = requestSelector(fieldName, destinationName);
        log.info("find query " + request);
        JsonObject response = couchDBClient.allDocs(database, request);
        return extractDocs(response, "Got {} documents for query by destination");
    }

    private Stream<TriggerData> extractDocs(JsonObject response, String s) {
        if (response.has("docs")) {
            JsonArray jsonElements = response.get("docs").getAsJsonArray();
            log.info(s, jsonElements.size());
            return StreamSupport.stream(jsonElements.spliterator(), false).map(e -> {
                log.info("JSON Element {}", e);
                return gson.fromJson(e, TriggerData.class);
            });
        } else {
            return Stream.empty();
        }
    }

    private JsonObject requestSelector(String fieldName, String regEx) {
        JsonObject request = new JsonObject();
        JsonObject triggerNameSelector = new JsonObject();
        JsonObject triggerRegEx = new JsonObject();
        triggerRegEx.addProperty("$regex", regEx);
        triggerNameSelector.add(fieldName, triggerRegEx);
        request.add("selector", triggerNameSelector);
        JsonArray fields = new JsonArray();
        for (String field : SELECTOR_FIELDS) {
            fields.add(field);
        }
        return request;
    }

}
