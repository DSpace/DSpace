/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion.oaire;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.suggestion.SuggestionEvidence;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of { @see org.dspace.app.suggestion.oaire.Approver} which filter ImportRecords
 * based on Author's name.
 * 
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 *
 */
public class AuthorNamesApprover implements Approver {

    private List<String> contributorMetadata;

    private List<String> names;

    @Autowired
    private ItemService itemService;

    /**
     * returns the metadata key of the Item which to base the filter on
     * @return metadata key
     */
    public List<String> getContributorMetadata() {
        return contributorMetadata;
    }

    /**
     * set the metadata key of the Item which to base the filter on
     * @return metadata key
     */
    public void setContributorMetadata(List<String> contributorMetadata) {
        this.contributorMetadata = contributorMetadata;
    }

    /**
     * return the metadata key of ImportRecord which to base the filter on
     * @return
     */
    public List<String> getNames() {
        return names;
    }

    /**
     * set the metadata key of ImportRecord which to base the filter on
     */
    public void setNames(List<String> names) {
        this.names = names;
    }

    /**
     * Method which is responsible to evaluate ImportRecord based on authors name.
     * This method extract the researcher name from Item using contributorMetadata fields
     * and try to match them with values extract from ImportRecord using metadata keys defined
     * in names.
     * ImportRecords which don't match will be discarded.
     * 
     * @param importRecord the import record to check
     * @param researcher DSpace item
     * @return the generated evidence or null if the record must be discarded
     */
    @Override
    public SuggestionEvidence filter(Item researcher, ImportRecord importRecord) {
        List<ImportRecord> filteredRecords = new ArrayList<>();
        List<String> authors = searchMetadataValues(researcher);
        List<String> metadataAuthors = new ArrayList<>();
        Collection<MetadatumDTO> metadata = new ArrayList<>();
        for (String contributorMetadatum : contributorMetadata) {
            String[] fields = contributorMetadatum.split("\\.");
            if (fields.length == 2) {
                metadata.addAll(importRecord.getValue(fields[0], fields[1], null));
            } else {
                metadata.addAll(importRecord.getValue(fields[0], fields[1], fields[2]));
            }
        }
        if (metadata != null) {
            for (MetadatumDTO metadatum : metadata) {
                metadataAuthors.add(metadatum.getValue());
            }
        }
        for (String metadataAuthor : metadataAuthors) {
            int idx = authors.indexOf(metadataAuthor);
            if (idx != -1) {
                filteredRecords.add(importRecord);
                return new SuggestionEvidence(this.getClass().getSimpleName(), 100 - (20 * idx / authors.size()),
                        "The author " + metadataAuthor + " matches the name with idx " + idx
                                + " of the ones stored in the researcher profile [" + StringUtils.join(authors, ", ")
                                + "]");
            }
        }
        return null;
    }

    /**
     * Return list of Item metadata values starting from metadata keys defined in class level variable names.
     * 
     * @param researcher DSpace item
     * @return list of metadata values
     */
    public List<String> searchMetadataValues(Item researcher) {
        List<String> authors = new ArrayList<String>();
        for (String name : names) {
            List<MetadataValue> values = itemService.getMetadataByMetadataString(researcher, name);
            if (values != null) {
                for (MetadataValue v : values) {
                    authors.add(v.getValue());
                }
            }
        }
        return authors;
    }

}
