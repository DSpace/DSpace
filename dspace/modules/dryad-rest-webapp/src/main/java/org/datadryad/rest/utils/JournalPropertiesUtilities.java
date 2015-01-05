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
public class JournalPropertiesUtilities {
    /**
     * Get the metadata directory for writing legacy xml files, based on an
     * organization.
     * @param organization the organization, with an organizationCode to look up
     * @return absolute directory in string format where the organizations XML files are stored
     */
    public static String getOutputDirectory(Organization organization) {
        // Requires configuration to be loaded and will change with atmire authority control
        if(organization == null) {
            throw new IllegalArgumentException("Must provide an organization object");
        }

        final String organizationCode = organization.organizationCode;
        return getOutputDirectory(organizationCode);
    }

    /**
     * Uses DryadJournalSubmissionUtils.journalProperties to find a matching
     * journal code.
     * @param organizationCode case-sensitive organization (journal) code to look up
     * @return a Map of journal properties for the journal with the code, or null if not found
     */
    private static Map<String,String> getPropertiesByOrganizationCode(String organizationCode) {
        // The journal properties is a Map of Journal Name strings to Maps of properties
        // So to look up by journal code we need iterate over map values
        Map<String,String> properties = null;
        Map<String, Map<String,String>> allJournalProperties = DryadJournalSubmissionUtils.journalProperties;
        for(String journalName : allJournalProperties.keySet() ) {
            Map<String, String> journalProperites = allJournalProperties.get(journalName);
            if(journalProperites.containsKey(DryadJournalSubmissionUtils.JOURNAL_ID)) {
                String journalCode = journalProperites.get(DryadJournalSubmissionUtils.JOURNAL_ID);
                if(journalCode.equals(organizationCode)) {
                    properties = journalProperites;
                    break;
                }
            }
        }
        return properties;
    }

    /**
     * Get the metadata directory for storing XML files based on an
     * organization (journal) code.
     * @param organizationCode case-sensitive code to look for
     * @return Absolute directory path in String format, or null if not defined
     * @throws IllegalArgumentException if code is empty or not found
     */
    public static String getOutputDirectory(String organizationCode) {
        if(organizationCode == null || organizationCode.length() == 0) {
            throw new IllegalArgumentException("Attempted to get output directory for empty code");
        }
        Map<String, String> properties = getPropertiesByOrganizationCode(organizationCode);
        if(properties == null) {
            throw new IllegalArgumentException("Organization code " + organizationCode + " not found");
        }
        final String directory = properties.get(DryadJournalSubmissionUtils.METADATADIR);
        return directory;
    }

    /**
     * Generate a valid base filename for saving a manuscript to the filesystem. Does
     * not include directory name or extension
     * @param manuscript manuscript with an ID
     * @return an escaped filename, using DryadJournalSubmission.escapeFilename.
     */
    public static String getTargetBaseFilename(Manuscript manuscript) {
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
