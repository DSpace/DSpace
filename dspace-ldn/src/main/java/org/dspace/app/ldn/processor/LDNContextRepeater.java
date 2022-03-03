/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.processor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.model.Context;
import org.dspace.app.ldn.model.Notification;

public class LDNContextRepeater {

    private final static Logger log = LogManager.getLogger(LDNContextRepeater.class);

    private final static String CONTEXT = "context";

    private String repeatOver;

    public String getRepeatOver() {
        return repeatOver;
    }

    public void setRepeatOver(String repeatOver) {
        this.repeatOver = repeatOver;
    }

    public Iterator<Notification> iterator(Notification notification) {
        return new NotificationIterator(notification, repeatOver);
    }

    private class NotificationIterator implements Iterator<Notification> {

        private final List<Notification> notifications;

        private NotificationIterator(Notification notification, String repeatOver) {
            this.notifications = new ArrayList<>();

            if (Objects.nonNull(repeatOver)) {
                ObjectMapper objectMapper = new ObjectMapper();

                JsonNode notificationNode = objectMapper.valueToTree(notification);

                log.info("Notification {}", notificationNode);

                JsonNode topContextNode = notificationNode.get(CONTEXT);
                if (topContextNode.isNull()) {
                    log.warn("Notification is missing context");
                    return;
                }

                log.info("Top context {}", topContextNode);

                JsonNode contextArrayNode = topContextNode.get(repeatOver);
                if (contextArrayNode.isNull()) {
                    log.error("Notification context {} is not defined", repeatOver);
                    return;
                }

                if (contextArrayNode.isArray()) {

                    for (JsonNode contextNode : ((ArrayNode) contextArrayNode)) {

                        log.info("Context {}", contextNode);

                        try {
                            Context context = objectMapper.treeToValue(contextNode, Context.class);

                            Notification copy = objectMapper.treeToValue(notificationNode, Notification.class);

                            copy.setContext(context);

                            this.notifications.add(copy);
                        } catch (JsonProcessingException e) {
                            log.error("Failed to copy notification");
                        }

                    }

                } else {
                    log.error("Notification context {} is not an array", repeatOver);
                }

            } else {
                this.notifications.add(notification);
            }
        }

        @Override
        public boolean hasNext() {
            return !this.notifications.isEmpty();
        }

        @Override
        public Notification next() {
            return this.notifications.remove(0);
        }

    }

}
