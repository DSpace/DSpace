/*
 */
package org.datadryad.rest.handler;

import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.datadryad.rest.storage.StoragePath;
/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public abstract class AbstractHandlerGroup<T> implements HandlerGroupInterface<T> {
    private static Logger log = Logger.getLogger(AbstractHandlerGroup.class);

    private List<HandlerInterface<T>> handlers = new ArrayList<HandlerInterface<T>>();

    @Override
    public void addHandler(HandlerInterface<T> handler) {
        handlers.add(handler);
    }

    @Override
    public void removeHandler(HandlerInterface<T> handler) {
        handlers.remove(handler);
    }

    @Override
    public void handleObjectCreated(StoragePath path, T object) {
        for(HandlerInterface<T> handler : this.handlers) {
            try {
                handler.handleCreate(path, object);
            } catch (HandlerException ex) {
                log.error("Exception handling object creation: ", ex);
            }
        }
    }

    @Override
    public void handleObjectUpdated(StoragePath path, T object) {
        for(HandlerInterface<T> handler : this.handlers) {
            try {
                handler.handleUpdate(path, object);
            } catch (HandlerException ex) {
                log.error("Exception handling object update: ", ex);
            }
        }
    }

    @Override
    public void handleObjectDeleted(StoragePath path, T object) {
        for(HandlerInterface<T> handler : this.handlers) {
            try {
                handler.handleDelete(path, object);
            } catch (HandlerException ex) {
                log.error("Exception handling object deletion: ", ex);
            }
        }
    }
}
