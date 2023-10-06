/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.action;

import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.model.Notification;
import org.dspace.content.Item;
import org.dspace.content.QAEvent;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.qaevent.service.QAEventService;
import org.dspace.services.ConfigurationService;
import org.dspace.web.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

public class LDNCorrectionAction implements LDNAction {

    private static final Logger log = LogManager.getLogger(LDNEmailAction.class);

    private String qaEventTopic;

    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    protected ItemService itemService;
    @Autowired
    private QAEventService qaEventService;

    @Override
    public ActionStatus execute(Notification notification, Item item) throws Exception {
        ActionStatus result = ActionStatus.ABORT;
        Context context = ContextUtil.obtainCurrentRequestContext();
        String itemName = itemService.getName(item);
        String value = "";
        QAEvent qaEvent = null;
        if (notification.getObject().getIetfCiteAs() != null) {
            value = notification.getObject().getIetfCiteAs();
            qaEvent = new QAEvent(QAEvent.COAR_NOTIFY,
                notification.getObject().getId(), item.getID().toString(), itemName,
                this.getQaEventTopic(), 1d,
                "{\"abstracts[0]\": \"" + value + "\"}"
                , new Date());
        } else if (notification.getObject().getAsRelationship() != null) {
            String type = notification.getObject().getAsRelationship();
            value = notification.getObject().getAsObject();
            qaEvent = new QAEvent(QAEvent.COAR_NOTIFY,
                notification.getObject().getId(), item.getID().toString(), itemName,
                this.getQaEventTopic(), 1d,
                "{\"pids[0].value\":\"" + value + "\"," +
                "\"pids[0].type\":\"" +  type + "\"}"
                , new Date());
        }
        qaEventService.store(context, qaEvent);
        result = ActionStatus.CONTINUE;

        return result;
    }

    public String getQaEventTopic() {
        return qaEventTopic;
    }

    public void setQaEventTopic(String qaEventTopic) {
        this.qaEventTopic = qaEventTopic;
    }

}
