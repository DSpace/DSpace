/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Try to look to an item metadata for the corresponding author name and email.
 * Failover to the RequestItemSubmitterStrategy.
 *
 * @author Andrea Bollini
 */
public class RequestItemMetadataStrategy extends RequestItemSubmitterStrategy {

    protected String emailMetadata;
    protected String fullNameMetadata;

    @Autowired(required = true)
    protected ItemService itemService;

    public RequestItemMetadataStrategy() {
    }

    @Override
    public List<RequestItemAuthor> getRequestItemAuthor(Context context, Item item)
        throws SQLException {
        if (emailMetadata != null) {
            List<MetadataValue> vals = itemService.getMetadataByMetadataString(item, emailMetadata);
            if (vals.size() > 0) {
                List<RequestItemAuthor> authors = new ArrayList<>(vals.size());
                for (MetadataValue datum : vals) {
                    String email = datum.getValue();
                    String fullname = null;
                    if (fullNameMetadata != null) {
                        List<MetadataValue> nameVals = itemService.getMetadataByMetadataString(item, fullNameMetadata);
                        if (!nameVals.isEmpty()) {
                            fullname = nameVals.get(0).getValue();
                        }
                    }

                    if (StringUtils.isBlank(fullname)) {
                        fullname = I18nUtil.getMessage(
                                "org.dspace.app.requestitem.RequestItemMetadataStrategy.unnamed",
                                context);
                    }
                    RequestItemAuthor author = new RequestItemAuthor(
                            fullname, email);
                    authors.add(author);
                }
                return authors;
            }
        }
        return super.getRequestItemAuthor(context, item);
    }

    public void setEmailMetadata(String emailMetadata) {
        this.emailMetadata = emailMetadata;
    }

    public void setFullNameMetadata(String fullNameMetadata) {
        this.fullNameMetadata = fullNameMetadata;
    }

}
