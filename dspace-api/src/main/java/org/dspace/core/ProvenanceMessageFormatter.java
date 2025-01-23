/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.InstallItemService;
import org.dspace.eperson.EPerson;

/**
 * The ProvenanceMessageProvider providing methods to generate provenance messages for DSpace items.
 * It loads message templates
 * from a JSON file and formats messages based on the context, including user details and timestamps.
 *
 * @author Michaela Paurikova (dspace at dataquest.sk)
 */
public class ProvenanceMessageFormatter {
    private InstallItemService installItemService;

    public ProvenanceMessageFormatter() {}

    public String getMessage(Context context, String messageTemplate, Item item, Object... args)
            throws SQLException, AuthorizeException {
        // Initialize InstallItemService if it is not initialized.
        if (installItemService == null) {
            installItemService = ContentServiceFactory.getInstance().getInstallItemService();
        }
        String msg = getMessage(context, messageTemplate, args);
        msg = msg + "\n" + installItemService.getBitstreamProvenanceMessage(context, item);
        return msg;
    }

    public String getMessage(Context context, String messageTemplate, Object... args) {
        EPerson currentUser = context.getCurrentUser();
        String timestamp = DCDate.getCurrent().toString();
        String details = validateMessageTemplate(messageTemplate, args);
        return String.format("%s by %s (%s) on %s",
                details,
                currentUser.getFullName(),
                currentUser.getEmail(),
                timestamp);
    }

    public String getMessage(Item item) {
        String msg = "Item was in collections:\n";
        List<Collection> collsList = item.getCollections();
        for (Collection coll : collsList) {
            msg = msg + coll.getName() + " (ID: " + coll.getID() + ")\n";
        }
        return msg;
    }

    public String getMessage(Bitstream bitstream) {
        // values of deleted bitstream
        String msg = bitstream.getName() + ": " +
                bitstream.getSizeBytes() + " bytes, checksum: " +
                bitstream.getChecksum() + " (" +
                bitstream.getChecksumAlgorithm() + ")\n";
        return msg;
    }

    public String getMessage(List<ResourcePolicy> resPolicies) {
        return resPolicies.stream()
                .filter(rp -> rp.getAction() == Constants.READ)
                .map(rp -> String.format("[%s, %s, %d, %s, %s, %s, %s]",
                        rp.getRpName(), rp.getRpType(), rp.getAction(),
                        rp.getEPerson() != null ? rp.getEPerson().getEmail() : null,
                        rp.getGroup() != null ? rp.getGroup().getName() : null,
                        rp.getStartDate() != null ? rp.getStartDate().toString() : null,
                        rp.getEndDate() != null ? rp.getEndDate().toString() : null))
                .collect(Collectors.joining(";"));
    }

    public String getMetadata(String oldMtdKey, String oldMtdValue) {
        return oldMtdKey + ": " + oldMtdValue;
    }

    public String getMetadataField(MetadataField metadataField) {
        return metadataField.toString()
                .replace('_', '.');
    }

    private String validateMessageTemplate(String messageTemplate, Object... args) {
        if (messageTemplate == null) {
            throw new IllegalArgumentException("The provenance message template is null!");
        }
        return String.format(messageTemplate, args);
    }
}
