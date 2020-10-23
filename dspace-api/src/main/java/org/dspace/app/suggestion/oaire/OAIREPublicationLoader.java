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

/**
 * Class responsible to load and manage ImportRecords from OpenAIRE
 *
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 *
 */
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

    /**
     * Load metadata from OpenAIRE using the import service. The service use the value
     * get from metadata key defined in class level variable names as author to query OpenAIRE.
     * 
     * @see org.dspace.importer.external.openaire.service.OpenAireImportMetadataSourceServiceImpl
     * @param researcher item to extract metadata from
     * @return list of ImportRecord
     */
    public List<ImportRecord> getImportRecords(Item researcher) {
        List<String> searchValues = searchMetadataValues(researcher);
        List<ImportRecord> matchingRecords = new ArrayList<>();
        try {
            for (String searchValue : searchValues) {
                matchingRecords.addAll(openaireImportService.getRecords(searchValue, 0, 9999));
            }
            List<ImportRecord> toReturn = removeDuplicates(matchingRecords);
            return toReturn;
        } catch (MetadataSourceException e) {
            throw new RuntimeException("Fail to import metadata from OpenAIRE");
        }
    }

    /**
     * This method remove duplicates from importRecords list.
     * An element is a duplicate if in the list exist another element
     * with the same value of the metadatum 'dc.identifier.other'
     *
     * @param importRecords list of ImportRecord
     * @return list of ImportRecords without duplicates
     */
    private List<ImportRecord> removeDuplicates(List<ImportRecord> importRecords) {
        List<ImportRecord> filteredRecords = new ArrayList<>();
        for (ImportRecord currentRecord : importRecords) {
            if (!isDuplicate(currentRecord, filteredRecords)) {
                filteredRecords.add(currentRecord);
            }
        }
        return filteredRecords;
    }


    /**
     * Check if the ImportRecord is already present in the list.
     * The comparison is made on the value of metadatum with key 'dc.identifier.other'
     * 
     * @param dto An importRecord instance
     * @param importRecords a list of importRecord
     * @return true if dto is already present in importRecords, false otherwise
     */
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

    /**
     * Extract the Item ID from an Import Record
     * 
     * @param dto ImportRecord
     * @return Item id
     */
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

    /**
     * Return list of Item metadata values starting from metadata keys defined in class level variable names.
     * 
     * @param researcher DSpace item
     * @return list of metadata values
     */
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
