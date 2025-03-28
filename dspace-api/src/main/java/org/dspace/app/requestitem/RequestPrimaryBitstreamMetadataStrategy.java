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
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;

/**
 * Try to look to an Item's primary bitstreams metadata for the corresponding author name and email.
 * Failover to the RequestItemSubmitterStrategy.
 *
 */
public class RequestPrimaryBitstreamMetadataStrategy extends RequestItemSubmitterStrategy {

    protected String emailMetadata;
    protected String fullNameMetadata;

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    @Autowired(required = true)
    protected ItemService itemService;

    @Autowired(required = true)
    protected BitstreamService bitstreamService;

    public RequestPrimaryBitstreamMetadataStrategy() {
    }

    @Override
    @NonNull
    public List<RequestItemAuthor> getRequestItemAuthor(Context context, Item item)
        throws SQLException {
        List<RequestItemAuthor> authors;

        if (emailMetadata != null) {
            // Consider only the original bundles.
            List<Bundle> bundles = item.getBundles(Constants.DEFAULT_BUNDLE_NAME);
            // Check for primary bitstreams first.
            Bitstream bitstream = bundles.stream()
                .map(bundle -> bundle.getPrimaryBitstream())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
            if (bitstream == null) {
                // If there is no primary bitstream,
                // take the first bitstream in the bundles.
                bitstream = bundles.stream()
                    .map(bundle -> bundle.getBitstreams())
                    .flatMap(List::stream)
                    .findFirst()
                    .orElse(null);
            }
            if (bitstream != null) {
                List<MetadataValue> vals = bitstreamService.getMetadataByMetadataString(bitstream, emailMetadata);

                List<MetadataValue> nameVals;
                if (null != fullNameMetadata) {
                    nameVals = bitstreamService.getMetadataByMetadataString(bitstream, fullNameMetadata);
                } else {
                    nameVals = Collections.EMPTY_LIST;
                }
                boolean useNames = vals.size() == nameVals.size();
                if (!vals.isEmpty()) {
                    authors = new ArrayList<>(vals.size());
                    for (int authorIndex = 0; authorIndex < vals.size(); authorIndex++) {
                        String email = vals.get(authorIndex).getValue();
                        String fullname = null;
                        if (useNames) {
                            fullname = nameVals.get(authorIndex).getValue();
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
            return Collections.EMPTY_LIST;
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
                //First get help desk name and email
                String email = configurationService.getProperty("mail.helpdesk");
                String name = configurationService.getProperty("mail.helpdesk.name");
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

    public void setEmailMetadata(@NonNull String emailMetadata) {
        this.emailMetadata = emailMetadata;
    }

    public void setFullNameMetadata(@NonNull String fullNameMetadata) {
        this.fullNameMetadata = fullNameMetadata;
    }

}
