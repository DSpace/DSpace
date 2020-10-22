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
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.openaire.service.OpenAireImportMetadataSourceServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;

public class OAIREPublicationLoader {

    private List<String> names;

    @Autowired
    private OpenAireImportMetadataSourceServiceImpl openaireImportService;

    @Autowired
    private ItemService itemService;

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    public List<ImportRecord> getImportRecords(Item researcher) {
        List<String> searchValues = searchMetadataValues(researcher);
        List<ImportRecord> matchingRecords = new ArrayList<>();
        try {
            for (String searchValue : searchValues) {
                matchingRecords.addAll(openaireImportService.getRecords(searchValue, 0, 9999));
            }
            System.out.println("Found " + matchingRecords.size() + " records to process for researcher "
                    + researcher.getID().toString());
            List<ImportRecord> toReturn = removeDuplicates(matchingRecords);
            System.out.println("Filter " + (matchingRecords.size() - toReturn.size()) + " records");
            return toReturn;
        } catch (MetadataSourceException e) {
            throw new RuntimeException("Fail to import metadata from OpenAIRE");
        }
    }

    private List<ImportRecord> removeDuplicates(List<ImportRecord> matchingRecords) {
        List<ImportRecord> filteredRecords = new ArrayList<>();
        for (ImportRecord currentRecord : matchingRecords) {
            if (!isDuplicate(currentRecord, filteredRecords)) {
                filteredRecords.add(currentRecord);
            }
        }
        return filteredRecords;
    }


    public boolean isDuplicate(ImportRecord dto, List<ImportRecord> importRecords) {
        String currentItemId = getIdFromImportRecord(dto);
        if (currentItemId == null) {
            return false;
        }
        for (ImportRecord importRecord : importRecords) {
            if (currentItemId.equals(getIdFromImportRecord(importRecord))) {
                return true;
            }
        }
        return false;
    }

    private String getIdFromImportRecord(ImportRecord dto) {
        Collection<MetadatumDTO> metadata = dto.getValue("dc", "identifier", "other");
        // FIXME: which behaviour if the entry doesn't have ID?
        if (metadata == null) {
            return null;
        } else if (metadata.isEmpty() || metadata.size() > 1) {
            return null;
        } else {
            return metadata.iterator().next().getValue();
        }
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
