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
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.nbevent.NBEventActionService;
import org.dspace.app.nbevent.service.NBEventService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.NBEvent;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * implementation class for {@link CorrectionType}
 * that will add a new relation metadata to target item if not exist.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class AddRelationCorrectionType implements CorrectionType, InitializingBean {
    private String id;
    private String topic;
    private String discoveryConfiguration;
    private String creationForm;
    private String targetMetadata;

    @Autowired
    private ItemService itemService;

    @Autowired
    private NBEventService nbEventService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private NBEventActionService nbEventActionService;

    @Override
    public void afterPropertiesSet() throws Exception {
        setTopic(topic.concat(targetMetadata));
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
    public String getDiscoveryConfiguration() {
        return discoveryConfiguration;
    }

    public void setDiscoveryConfiguration(String discoveryConfiguration) {
        this.discoveryConfiguration = discoveryConfiguration;
    }

    @Override
    public String getCreationForm() {
        return creationForm;
    }

    public void setCreationForm(String creationForm) {
        this.creationForm = creationForm;
    }

    public void setTargetMetadata(String targetMetadata) {
        this.targetMetadata = targetMetadata;
    }

    @Override
    public boolean isAllowed(Context context, Item targetItem) throws AuthorizeException, SQLException {
        authorizeService.authorizeAction(context, targetItem, Constants.READ);

        if (!targetItem.isArchived() || targetItem.isWithdrawn() || !targetItem.isDiscoverable()) {
            return false;
        }

        if (StringUtils.equalsAny(
            itemService.getEntityType(targetItem), "PersonalArchive", "PersonalPath")
        ) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isAllowed(Context context, Item targetItem, Item relatedItem)
        throws AuthorizeException, SQLException {
        if (isAllowed(context, targetItem)) {
            if (isMetadataExisted(targetItem, relatedItem.getID().toString())) {
                throw new IllegalArgumentException("the provided related item<" +
                    relatedItem.getID() + "> is already linked");
            }
            return true;
        }
        return false;
    }

    @Override
    public NBEvent createCorrection(Context context, Item targetItem, Item relatedItem) {
        NBEvent nbEvent = new NBEvent("handle:" + targetItem.getHandle(), targetItem.getID().toString(),
            targetItem.getName(), this.getTopic(), 1.0, new Gson().toJson(new Object()), new Date());
        nbEvent.setRelated(relatedItem.getID().toString());

        nbEventService.store(context, nbEvent);
        nbEventActionService.accept(context, nbEvent);
        return nbEvent;
    }

    private boolean isMetadataExisted(Item targetItem, String value) {
        return targetItem
            .getMetadata()
            .stream()
            .filter(metadataValue ->
                metadataValue.getMetadataField().toString('.').equals(targetMetadata))
            .anyMatch(metadataValue ->
                metadataValue.getValue().equals(value) &&
                    metadataValue.getAuthority().equals(value));
    }
}
