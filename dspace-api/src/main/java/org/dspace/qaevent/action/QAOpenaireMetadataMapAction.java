/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.action;

import java.sql.SQLException;
import java.util.Map;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.qaevent.QualityAssuranceAction;
import org.dspace.qaevent.service.dto.OpenaireMessageDTO;
import org.dspace.qaevent.service.dto.QAMessageDTO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link QualityAssuranceAction} that add a specific metadata on the given
 * item based on the OPENAIRE message type.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class QAOpenaireMetadataMapAction implements QualityAssuranceAction {
    public static final String DEFAULT = "default";

    private Map<String, String> types;
    @Autowired
    private ItemService itemService;

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    public Map<String, String> getTypes() {
        return types;
    }

    public void setTypes(Map<String, String> types) {
        this.types = types;
    }

    /**
     * Apply the correction on one metadata field of the given item based on the
     * openaire message type.
     */
    @Override
    public void applyCorrection(Context context, Item item, Item relatedItem, QAMessageDTO message) {

        if (!(message instanceof OpenaireMessageDTO)) {
            throw new IllegalArgumentException("Unsupported message type: " + message.getClass());
        }

        OpenaireMessageDTO openaireMessage = (OpenaireMessageDTO) message;

        try {
            String targetMetadata = types.get(openaireMessage.getType());
            if (targetMetadata == null) {
                targetMetadata = types.get(DEFAULT);
            }
            String[] metadata = splitMetadata(targetMetadata);
            itemService.addMetadata(context, item, metadata[0], metadata[1], metadata[2], null,
                openaireMessage.getValue());
            itemService.update(context, item);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }

    }

    public String[] splitMetadata(String metadata) {
        String[] result = new String[3];
        String[] split = metadata.split("\\.");
        result[0] = split[0];
        result[1] = split[1];
        if (split.length == 3) {
            result[2] = split[2];
        }
        return result;
    }
}
