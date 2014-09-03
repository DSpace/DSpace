/*
 */
package org.datadryad.rest.utils;

import java.util.Map;
import org.datadryad.rest.models.Manuscript;
import org.datadryad.rest.models.Organization;
import org.datadryad.rest.storage.StoragePath;
import org.dspace.submit.utils.DryadJournalSubmissionUtils;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DryadPathUtilities {
    private static final String ORGANIZATION_KEY = "organizationCode";
    public static String getOutputDirectory(Organization organization) {
        // Requires configuration to be loaded and will change with atmire authority control
        if(organization == null) {
            throw new IllegalArgumentException("Must provide an organization object");
        }

        final String organizationCode = organization.organizationCode;
        return getOutputDirectory(organizationCode);
    }

    public static String getOutputDirectory(String organizationCode) {
        if(organizationCode == null) {
            throw new IllegalArgumentException("Organization must have a code to get output directory");
        }
        // Unfortunately, the journal properties interface expects a full name
        // and not a code/key. So we must consider that...

        Map<String, String> properties = DryadJournalSubmissionUtils.getPropertiesByJournal(organizationCode);
        if(properties == null) {
            throw new IllegalArgumentException("Organization code " + organizationCode + " not found by DryadJournalSubmissionUtils");
        }
        final String directory = properties.get(DryadJournalSubmissionUtils.METADATADIR);
        return directory;
    }

    public static String getTargetFilename(Manuscript manuscript) {
        if(manuscript == null) {
            throw new IllegalArgumentException("Must provide a manuscript object");
        }
        final String manuscriptId = manuscript.manuscriptId;
        if(manuscriptId == null || manuscriptId.length() == 0) {
            throw new IllegalArgumentException("manuscriptId is empty");
        }
        return DryadJournalSubmissionUtils.escapeFilename(manuscriptId);
    }

    public static String getOrganizationCode(StoragePath path) {
        int index = path.getKeyPath().indexOf(ORGANIZATION_KEY);
        if(index != -1) {
            return path.getValuePath().get(index);
        } else {
            return null;
        }
    }
}
