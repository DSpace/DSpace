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
        QAEvent qaEvent = new QAEvent(QAEvent.COAR_NOTIFY,
            notification.getObject().getId(), item.getID().toString(), item.getName(),
            this.getQaEventTopic(), 0d,
            "{\"abstracts[0]\": \"" + notification.getObject().getIetfCiteAs() + "\"}"
            , new Date());
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
