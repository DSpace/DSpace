/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.correctiontype;

import static org.dspace.content.QAEvent.DSPACE_USERS_SOURCE;
import static org.dspace.core.Constants.READ;

import java.sql.SQLException;
import java.util.Date;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.QAEvent;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.qaevent.service.QAEventService;
import org.dspace.qaevent.service.dto.CorrectionTypeMessageDTO;
import org.dspace.qaevent.service.dto.QAMessageDTO;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation class for {@link CorrectionType}
 * that will withdrawn target item if it archived and wasn't withdrawn alredy.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class WithdrawnCorrectionType implements CorrectionType, InitializingBean {

    public static final String WITHDRAWAL_REINSTATE_GROUP = "qaevents.withdraw-reinstate.group";

    private String id;
    private String topic;
    private String creationForm;

    @Autowired
    private GroupService groupService;
    @Autowired
    private QAEventService qaEventService;
    @Autowired
    private AuthorizeService authorizeService;
    @Autowired
    private ConfigurationService configurationService;


    @Override
    public boolean isAllowed(Context context, Item targetItem) throws SQLException {
        if (targetItem.isWithdrawn() || !targetItem.isArchived()) {
            return false;
        }
        try {
            authorizeService.authorizeAction(context, targetItem, READ);
        } catch (AuthorizeException e) {
            return false;
        }
        boolean isAdmin = authorizeService.isAdmin(context);
        if (!currentUserIsMemberOfwithdrawalReinstateGroup(context) && !isAdmin) {
            return false;
        }
        long tot = qaEventService.countSourcesByTarget(context, targetItem.getID());
        return tot == 0;
    }

    private boolean currentUserIsMemberOfwithdrawalReinstateGroup(Context context) throws SQLException {
        String groupName = configurationService.getProperty(WITHDRAWAL_REINSTATE_GROUP);
        if (StringUtils.isBlank(groupName)) {
            return false;
        }
        Group withdrawalReinstateGroup = groupService.findByName(context, groupName);
        return withdrawalReinstateGroup != null && groupService.isMember(context, withdrawalReinstateGroup);
    }

    @Override
    public QAEvent createCorrection(Context context, Item targetItem, QAMessageDTO reason) {
        ObjectNode reasonJson = createReasonJson(reason);
        QAEvent qaEvent = new QAEvent(DSPACE_USERS_SOURCE,
                                      context.getCurrentUser().getID().toString(),
                                      targetItem.getID().toString(),
                                      targetItem.getName(),
                                      this.getTopic(),
                                      1.0,
                                      reasonJson.toString(),
                                      new Date()
                                      );

        qaEventService.store(context, qaEvent);
        return qaEvent;
    }

    private ObjectNode createReasonJson(QAMessageDTO reason) {
        CorrectionTypeMessageDTO mesasge = (CorrectionTypeMessageDTO) reason;
        ObjectNode jsonNode = new ObjectMapper().createObjectNode();
        jsonNode.put("reason", mesasge.getReason());
        return jsonNode;
    }

    @Override
    public boolean isAllowed(Context context, Item targetItem, Item relatedItem)
            throws AuthorizeException, SQLException {
        return isAllowed(context, targetItem);
    }

    @Override
    public QAEvent createCorrection(Context context, Item targetItem, Item relatedItem, QAMessageDTO reason) {
        return createCorrection(context, targetItem, reason);
    }

    @Override
    public boolean isRequiredRelatedItem() {
        return false;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    @Override
    public void afterPropertiesSet() throws Exception {}

    public void setCreationForm(String creationForm) {
        this.creationForm = creationForm;
    }

}
