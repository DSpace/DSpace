/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.action;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.ldn.model.Notification;
import org.dspace.app.ldn.service.LDNMessageService;
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
    @Autowired
    private LDNMessageService ldnMessageService;

    @Override
    public ActionStatus execute(Notification notification, Item item) throws Exception {
        ActionStatus result;
        Context context = ContextUtil.obtainCurrentRequestContext();
        //FIXME the original id should be just an (optional) identifier/reference of the event in
        // the external system. The target Item should be passed as a constructor argument
        QAEvent qaEvent = new QAEvent(QAEvent.COAR_NOTIFY,
            "oai:localhost:" + item.getHandle(), item.getID().toString(), item.getName(),
            this.getQaEventTopic(), getScore(context, notification).doubleValue(),
            "{\"abstracts[0]\": \"" + notification.getObject().getIetfCiteAs() + "\"}"
            , new Date());
        qaEventService.store(context, qaEvent);
        result = ActionStatus.CONTINUE;

        return result;
    }

    private BigDecimal getScore(Context context, Notification notification) throws SQLException {

        if (notification.getOrigin() == null) {
            return BigDecimal.ZERO;
        }

        NotifyServiceEntity service = ldnMessageService.findNotifyService(context, notification.getOrigin());

        if (service == null) {
            return BigDecimal.ZERO;
        }

        return service.getScore();
    }

    public String getQaEventTopic() {
        return qaEventTopic;
    }

    public void setQaEventTopic(String qaEventTopic) {
        this.qaEventTopic = qaEventTopic;
    }

}
