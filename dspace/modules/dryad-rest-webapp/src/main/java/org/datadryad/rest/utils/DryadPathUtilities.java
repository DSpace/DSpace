/*
 */
package org.datadryad.rest.utils;

import java.util.Map;
import org.datadryad.rest.models.Manuscript;
import org.datadryad.rest.models.Organization;
import org.dspace.submit.utils.DryadJournalSubmissionUtils;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DryadPathUtilities {
    public static String getOutputDirectory(Organization organization) {
        // Requires configuration to be loaded and will change with atmire authority control
        if(organization == null) {
            throw new IllegalArgumentException("Must provide an organization object");
        }

        final String organizationCode = organization.organizationCode;
        if(organizationCode == null) {
            throw new IllegalArgumentException("Organization must have a code to get output directory");
        }
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
}
