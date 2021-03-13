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
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
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
        List<RequestItemAuthor> authors;
        if (emailMetadata != null) {
            List<MetadataValue> vals = itemService.getMetadataByMetadataString(item, emailMetadata);
            if (vals.size() > 0) {
                authors = new ArrayList<>(vals.size());
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
            } else {
                return Collections.EMPTY_LIST;
            }
        } else {
            // Uses the basic strategy to look for the original submitter
            authors = super.getRequestItemAuthor(context, item);

            // Remove from the list authors that do not have email addresses.
            for (RequestItemAuthor author : authors) {
                if (null == author.getEmail()) {
                    authors.remove(author);
                }
            }

            if (authors.isEmpty()) { // No author email addresses!  Fall back
                String email = null;
                String name = null;
                ConfigurationService configurationService
                        = DSpaceServicesFactory.getInstance().getConfigurationService();
                //First get help desk name and email
                email = configurationService.getProperty("mail.helpdesk");
                name = configurationService.getProperty("mail.helpdesk.name");
                // If help desk mail is null get the mail and name of admin
                if (email == null) {
                    email = configurationService.getProperty("mail.admin");
                    name = configurationService.getProperty("mail.admin.name");
                }
                authors.add(new RequestItemAuthor(name, email));
            }
            return authors;
        }
    }

    public void setEmailMetadata(String emailMetadata) {
        this.emailMetadata = emailMetadata;
    }

    public void setFullNameMetadata(String fullNameMetadata) {
        this.fullNameMetadata = fullNameMetadata;
    }

}
