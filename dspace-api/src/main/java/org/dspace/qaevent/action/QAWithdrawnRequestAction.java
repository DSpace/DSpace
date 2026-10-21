/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.action;

import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.qaevent.QualityAssuranceAction;
import org.dspace.qaevent.service.dto.CorrectionTypeMessageDTO;
import org.dspace.qaevent.service.dto.QAMessageDTO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * QAWithdrawnRequestAction is an implementation of the QualityAssuranceAction interface.
 * It is responsible for applying a correction to withdraw a specified item.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 * @author Stefano Maffei (steph-ieffam - stefano.maffei at 4science.com)
 */
public class QAWithdrawnRequestAction implements QualityAssuranceAction {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private ItemService itemService;

    /**
     * Optional metadata field where the reason provided with the withdrawal request is
     * stored on the item, so that it can be shown on its tombstone page. When it is not
     * configured the reason is not persisted.
     */
    private String reasonMetadataField;

    @Override
    public void applyCorrection(Context context, Item item, Item relatedItem, QAMessageDTO message) {
        try {
            storeReason(context, item, message);
            itemService.withdraw(context, item);
        } catch (SQLException | AuthorizeException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Stores the reason carried by the withdrawal request on the configured metadata field,
     * if any. The metadata is added before the withdrawal, so that it is persisted by the
     * update performed by {@link ItemService#withdraw(Context, Item)}.
     *
     * @param context the DSpace context
     * @param item    the item being withdrawn
     * @param message the message of the quality assurance event
     * @throws SQLException if a database error occurs
     */
    private void storeReason(Context context, Item item, QAMessageDTO message) throws SQLException {
        if (StringUtils.isBlank(reasonMetadataField) || !(message instanceof CorrectionTypeMessageDTO)) {
            return;
        }

        String reason = ((CorrectionTypeMessageDTO) message).getReason();
        if (StringUtils.isBlank(reason)) {
            return;
        }

        MetadataFieldName metadataField = new MetadataFieldName(reasonMetadataField);
        itemService.clearMetadata(context, item, metadataField.schema, metadataField.element,
            metadataField.qualifier, Item.ANY);
        itemService.addMetadata(context, item, metadataField.schema, metadataField.element,
            metadataField.qualifier, null, reason);
    }

    public String getReasonMetadataField() {
        return reasonMetadataField;
    }

    public void setReasonMetadataField(String reasonMetadataField) {
        this.reasonMetadataField = reasonMetadataField;
    }

}
