/*
 */
package org.datadryad.rest.handler;

import java.io.IOException;
import java.text.SimpleDateFormat;
import org.apache.log4j.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import org.datadryad.rest.storage.StoragePath;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 * @param <T> A type that can be serialized to JSON with jackson
 */
public class LoggingHandler<T> implements HandlerInterface<T>{
    private static final Logger log = Logger.getLogger(LoggingHandler.class);
    ObjectWriter writer;

    public LoggingHandler() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setConfig(mapper.getSerializationConfig().with(new SimpleDateFormat("yyyy-MM-dd")));
        this.writer = mapper.writer(new DefaultPrettyPrinter());
    }

    private void logObject(String method, T object) {
        String objectString = "";
        try {
            objectString = writer.writeValueAsString(object);
        } catch (IOException ex) {
            log.error("IO Exception writing logging json object", ex);
        }
        String message = String.format("%s\n%s", method, objectString);
        log.info(message);
    }

    @Override
    public void handleCreate(StoragePath path, T object) throws HandlerException {
        String method = "REST Create: " + path.toString();
        logObject(method, object);
    }

    @Override
    public void handleUpdate(StoragePath path, T object) throws HandlerException {
        String method = "REST Update: " + path.toString();
        logObject(method, object);
    }

    @Override
    public void handleDelete(StoragePath path, T object) throws HandlerException {
        String method = "REST Delete: " + path.toString();
        logObject(method, object);
    }

}
