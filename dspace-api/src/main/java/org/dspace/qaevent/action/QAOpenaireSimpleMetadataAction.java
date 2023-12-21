/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.action;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.qaevent.QualityAssuranceAction;
import org.dspace.qaevent.service.dto.OpenaireMessageDTO;
import org.dspace.qaevent.service.dto.QAMessageDTO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link QualityAssuranceAction} that add a simple metadata to the given
 * item.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class QAOpenaireSimpleMetadataAction implements QualityAssuranceAction {
    private String metadata;
    private String metadataSchema;
    private String metadataElement;
    private String metadataQualifier;
    @Autowired
    private ItemService itemService;

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
        String[] split = metadata.split("\\.");
        this.metadataSchema = split[0];
        this.metadataElement = split[1];
        if (split.length == 3) {
            this.metadataQualifier = split[2];
        }
    }

    @Override
    public void applyCorrection(Context context, Item item, Item relatedItem, QAMessageDTO message) {
        try {
            itemService.addMetadata(context, item, metadataSchema, metadataElement, metadataQualifier, null,
                ((OpenaireMessageDTO) message).getAbstracts());
            itemService.update(context, item);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }
    }
}
