/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.correctiontype;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.dspace.content.QAEvent.INTERNAL_ITEM_SOURCE;
import static org.dspace.core.Constants.READ;

import java.sql.SQLException;
import java.util.Date;

import com.google.gson.Gson;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.QAEvent;
import org.dspace.core.Context;
import org.dspace.qaevent.service.QAEventActionService;
import org.dspace.qaevent.service.QAEventService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation class for {@link CorrectionType}
 * that will withdrawn target item if it archived and wasn't withdrawn alredy.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class WithdrawnCorrectionType implements CorrectionType, InitializingBean {

    private String id;
    private String topic;

    @Autowired
    private QAEventService qaEventService;
    @Autowired
    private AuthorizeService authorizeService;
    @Autowired
    private QAEventActionService qaEventActionService;

    @Override
    public boolean isAllowed(Context context, Item targetItem) throws AuthorizeException, SQLException {
        authorizeService.authorizeAction(context, targetItem, READ);
        return targetItem.isArchived() && !targetItem.isWithdrawn();
    }

    @Override
    public QAEvent createCorrection(Context context, Item targetItem) {
        QAEvent qaEvent = new QAEvent(INTERNAL_ITEM_SOURCE,
                                      "handle:" + targetItem.getHandle(),
                                      targetItem.getID().toString(),
                                      targetItem.getName(),
                                      this.getTopic(), 1.0,
                                      new Gson().toJson(new Object()),
                                      new Date()
                                      );

        qaEventService.store(context, qaEvent);
        qaEventActionService.accept(context, qaEvent);
        return qaEvent;
    }

    @Override
    public boolean isAllowed(Context context, Item targetItem, Item relatedItem)
            throws AuthorizeException, SQLException {
        return isAllowed(context, targetItem);
    }

    @Override
    public QAEvent createCorrection(Context context, Item targetItem, Item relatedItem) {
        return createCorrection(context, targetItem);
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

    @Override
    public String getCreationForm() {
        return EMPTY;
    }

    @Override
    public String getDiscoveryConfiguration() {
        return EMPTY;
    }

}
