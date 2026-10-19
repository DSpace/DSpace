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

/**
 * Context repeater to iterate over array context properties of a received
 * notification. The returned notification iterator is a notification with
 * context array elements hoisted onto the root of the notification context.
 */
public class LDNContextRepeater {

    private final static Logger log = LogManager.getLogger(LDNContextRepeater.class);

    private final static String CONTEXT = "context";

    private String repeatOver;

    /**
     * @return String
     */
    public String getRepeatOver() {
        return repeatOver;
    }

    /**
     * @param repeatOver
     */
    public void setRepeatOver(String repeatOver) {
        this.repeatOver = repeatOver;
    }

    /**
     * @param  notification
     * @return Iterator<Notification>
     */
    public Iterator<Notification> iterator(Notification notification) {
        return new NotificationIterator(notification, repeatOver);
    }

    /**
     * Private inner class defining the notification iterator.
     */
    private class NotificationIterator implements Iterator<Notification> {

        private final List<Notification> notifications;

        /**
         * Convert notification to JsonNode in order to clone for each context array
         * element. Each element is then hoisted to the root of the cloned notification
         * context.
         *
         * @param notification received notification
         * @param repeatOver   which context property to repeat over
         */
        private NotificationIterator(Notification notification, String repeatOver) {
            this.notifications = new ArrayList<>();

            if (Objects.nonNull(repeatOver)) {
                ObjectMapper objectMapper = new ObjectMapper();

                JsonNode notificationNode = objectMapper.valueToTree(notification);

                log.debug("Notification {}", notificationNode);

                JsonNode topContextNode = notificationNode.get(CONTEXT);
                if (topContextNode.isNull()) {
                    log.warn("Notification is missing context");
                    return;
                }

                JsonNode contextArrayNode = topContextNode.get(repeatOver);
                if (contextArrayNode == null || contextArrayNode.isNull()) {
                    log.error("Notification context {} is not defined", repeatOver);
                    return;
                }

                if (contextArrayNode.isArray()) {

                    for (JsonNode contextNode : ((ArrayNode) contextArrayNode)) {

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

        /**
         * @return boolean
         */
        @Override
        public boolean hasNext() {
            return !this.notifications.isEmpty();
        }

        /**
         * @return Notification
         */
        @Override
        public Notification next() {
            return this.notifications.remove(0);
        }

    }

}