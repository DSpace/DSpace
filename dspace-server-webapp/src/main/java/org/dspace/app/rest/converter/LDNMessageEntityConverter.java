/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.UUID;

import org.dspace.app.ldn.LDNMessageEntity;
import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.rest.model.LDNMessageEntityRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.DSpaceObject;
import org.dspace.discovery.IndexableObject;
import org.springframework.stereotype.Component;

/**
 * Converter to translate between {@link LDNMessageEntity} and {@link LDNMessageEntityRest} representations.
 *  @author Stefano Maffei (stefano.maffei at 4science.com)
 */
@Component
public class LDNMessageEntityConverter implements IndexableObjectConverter<LDNMessageEntity, LDNMessageEntityRest> {

    @Override
    public LDNMessageEntityRest convert(LDNMessageEntity obj, Projection projection) {
        LDNMessageEntityRest ldnRest = new LDNMessageEntityRest();
        ldnRest.setNotificationId(obj.getID());
        ldnRest.setId(obj.getID());
        ldnRest.setQueueStatus(obj.getQueueStatus());
        ldnRest.setQueueStatusLabel(LDNMessageEntity.getQueueStatus(obj));
        ldnRest.setContext(getObjectIdentifier(obj.getContext()));
        ldnRest.setObject(getObjectIdentifier(obj.getObject()));
        ldnRest.setActivityStreamType(obj.getActivityStreamType());
        ldnRest.setCoarNotifyType(obj.getCoarNotifyType());
        ldnRest.setTarget(getObjectIdentifier(obj.getTarget()));
        ldnRest.setOrigin(getObjectIdentifier(obj.getOrigin()));
        ldnRest.setInReplyTo(getObjectIdentifier(obj.getInReplyTo()));
        ldnRest.setQueueAttempts(obj.getQueueAttempts());
        ldnRest.setQueueLastStartTime(obj.getQueueLastStartTime());
        ldnRest.setQueueTimeout(obj.getQueueTimeout());
        ldnRest.setMessage(obj.getMessage());
        ldnRest.setNotificationType(LDNMessageEntity.getNotificationType(obj));
        return ldnRest;
    }

    private UUID getObjectIdentifier(DSpaceObject dso) {
        return dso == null ? null : dso.getID();
    }

    private Integer getObjectIdentifier(NotifyServiceEntity nse) {
        return nse == null ? null : nse.getID();
    }

    private String getObjectIdentifier(LDNMessageEntity msg) {
        return msg == null ? null : msg.getID();
    }

    @Override
    public Class<LDNMessageEntity> getModelClass() {
        return LDNMessageEntity.class;
    }

    @Override
    public boolean supportsModel(IndexableObject idxo) {
        return idxo.getIndexedObject() instanceof LDNMessageEntity;
    }

}
