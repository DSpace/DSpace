/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.SortedSet;

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

    private String activityStreamType;
    private String coarNotifyType;

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
        Set<String> notificationType = notification.getType();
        if (notificationType == null) {
            return result;
        }
        ArrayList<String> arrayList = new ArrayList<String>(notificationType);
        // sorting the list
        Collections.sort(arrayList);
        //String[] notificationTypeArray = notificationType.stream().toArray(String[]::new);
        this.setActivityStreamType(arrayList.get(0));
        this.setCoarNotifyType(arrayList.get(1));
        if (this.getActivityStreamType() == null || this.getCoarNotifyType() == null) {
            if (this.getActivityStreamType() == null) {
                log.warn("Correction Action can't be executed: activityStreamType is null");
            }
            if (this.getCoarNotifyType() == null) {
                log.warn("Correction Action can't be executed: coarNotifyType is null");
            }
            return result;
        }
        if ("Announce".equalsIgnoreCase(this.getActivityStreamType())) {
            if (this.getCoarNotifyType().equalsIgnoreCase("coar-notify:ReviewAction")) {
                /* new qa event ENRICH/MORE/REVIEW
                 * itemService.addMetadata(context, item, "datacite",
                    "relation", "isReviewedBy", null, this.getIsReviewedBy());
                */
                QAEvent qaEvent = new QAEvent(QAEvent.OPENAIRE_SOURCE,
                    notification.getObject().getId(), item.getID().toString(), item.getName(),
                    "ENRICH/MORE/REVIEW", 0d,
                    "{\"abstracts[0]\": \"" + notification.getObject().getIetfCiteAs() + "\"}"
                    , new Date());
                qaEventService.store(context, qaEvent);
                result = ActionStatus.CONTINUE;
            }
            if (this.getCoarNotifyType().equalsIgnoreCase("coar-notify:EndorsementAction")) {
                // new qa event ENRICH/MORE/ENDORSEMENT
                QAEvent qaEvent = new QAEvent(QAEvent.OPENAIRE_SOURCE,
                    notification.getObject().getId(), item.getID().toString(), item.getName(),
                    "ENRICH/MORE/ENDORSEMENT", 0d,
                    "{\"abstracts[0]\": \"" + notification.getObject().getIetfCiteAs() + "\"}"
                    , new Date());
                qaEventService.store(context, qaEvent);
                result = ActionStatus.CONTINUE;
            }
        }
        return result;
    }

    public String getActivityStreamType() {
        return activityStreamType;
    }

    public void setActivityStreamType(String activityStreamType) {
        this.activityStreamType = activityStreamType;
    }

    public String getCoarNotifyType() {
        return coarNotifyType;
    }

    public void setCoarNotifyType(String coarNotifyType) {
        this.coarNotifyType = coarNotifyType;
    }

}
