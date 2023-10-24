/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.correctiontype;

import java.sql.SQLException;
import java.util.Date;

import com.google.gson.Gson;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.QAEvent;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.qaevent.service.QAEventActionService;
import org.dspace.qaevent.service.QAEventService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation class for {@link CorrectionType}
 * that will reinstate target item if it's withdrawn.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class ReinstateCorrectionType implements CorrectionType, InitializingBean {

    private String id;
    private String topic;

    @Autowired
    private QAEventService qaEventService;
    @Autowired
    private AuthorizeService authorizeService;
    @Autowired
    private QAEventActionService qaEventActionService;

    @Override
    public boolean isAllowed(Context context, Item targetItem) throws SQLException, AuthorizeException {
        authorizeService.authorizeAction(context, targetItem, Constants.READ);
        return targetItem.isWithdrawn();
    }

    @Override
    public boolean isAllowed(Context context, Item targetItem, Item relatedItem) throws AuthorizeException,
        SQLException {
        return isAllowed(context, targetItem);
    }

    @Override
    public QAEvent createCorrection(Context context, Item targetItem) {
        QAEvent qaEvent = new QAEvent("handle:" + targetItem.getHandle(),
                                      targetItem.getID().toString(),
                                      null,
                                      targetItem.getName(),
                                      this.getTopic(),
                                      1.0,
                                      new Gson().toJson(new Object()),
                                      new Date()
                                      );

        qaEventService.store(context, qaEvent);
        qaEventActionService.accept(context, qaEvent);
        return qaEvent;
    }

    @Override
    public QAEvent createCorrection(Context context, Item targetItem, Item relatedItem) {
        return this.createCorrection(context, targetItem);
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

}
