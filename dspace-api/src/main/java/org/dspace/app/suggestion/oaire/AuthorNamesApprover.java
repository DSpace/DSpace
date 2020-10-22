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

import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.springframework.beans.factory.annotation.Autowired;

public class AuthorNamesApprover {

    private List<String> contributorMetadata;

    private List<String> names;

    @Autowired
    private ItemService itemService;

    public List<String> getContributorMetadata() {
        return contributorMetadata;
    }

    public void setContributorMetadata(List<String> contributorMetadata) {
        this.contributorMetadata = contributorMetadata;
    }

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    public List<ImportRecord> filter(Item researcher, List<ImportRecord> importRecords) {
        List<ImportRecord> filteredRecords = new ArrayList<>();
        List<String> authors = searchMetadataValues(researcher);
        for (ImportRecord importRecord : importRecords) {
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
                if (authors.contains(metadataAuthor)) {
                    filteredRecords.add(importRecord);
                    break;
                }
            }
        }
        return filteredRecords;
    }

    public List<String> searchMetadataValues(Item researcher) {
        List<String> authors = new ArrayList<String>();
        for (String name : names) {
            String value = itemService.getMetadata(researcher, name);
            if (value != null) {
                authors.add(value);
            }
        }
        return authors;
    }

}
