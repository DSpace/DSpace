/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.action;

import java.util.Date;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.model.Notification;
import org.dspace.content.Item;
import org.dspace.content.QAEvent;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.qaevent.service.QAEventService;
import org.dspace.qaevent.service.dto.NotifyMessageDTO;
import org.dspace.services.ConfigurationService;
import org.dspace.web.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Implementation for LDN Correction Action. It creates a QA Event according to the LDN Message received *
 * @author Francesco Bacchelli (francesco.bacchelli at 4science.it)
 *
 */
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
        QAEvent qaEvent = null;
        if (notification.getObject() != null) {
            String citeAs = notification.getObject().getIetfCiteAs();
            if (citeAs == null || citeAs.isEmpty()) {
                citeAs = notification.getObject().getId();
            }
            NotifyMessageDTO message = new NotifyMessageDTO();
            message.setHref(citeAs);
            message.setRelationship(notification.getObject().getAsRelationship());
            if (notification.getOrigin() != null) {
                message.setServiceId(notification.getOrigin().getId());
                message.setServiceName(notification.getOrigin().getInbox());
            }
            Gson gson = new Gson();
            // "oai:www.dspace.org:" + item.getHandle(),
            qaEvent = new QAEvent(QAEvent.COAR_NOTIFY_SOURCE,
                notification.getObject().getId(), item.getID().toString(), itemName,
                this.getQaEventTopic(), 1d,
                gson.toJson(message)
                , new Date());
            qaEventService.store(context, qaEvent);
            result = ActionStatus.CONTINUE;
        }

        return result;
    }

    public String getQaEventTopic() {
        return qaEventTopic;
    }

    public void setQaEventTopic(String qaEventTopic) {
        this.qaEventTopic = qaEventTopic;
    }

}
