package org.dspace.importer.external.metadatamapping.contributor;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.service.components.dto.PlainMetadataKeyValueItem;
import org.dspace.importer.external.service.components.dto.PlainMetadataSourceDto;


/**
 * This class implements functionalities to handle common situation regarding plain metadata.
 * In some scenario, like csv or tsv, the format don't allow lists.
 * We can use this MetadataContribut to parse a given plain metadata and split it into
 * related list, based on the delimiter. No escape character is present.
 * Default values are comma (,) for delimiter, and double quote (") for escape character
 * 
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 *
 */
public class EnhancedSimpleMetadataContributor extends SimpleMetadataContributor {

    private char delimiter = ',';

    private char escape = '"';

    private boolean useEnhancer;

    public void setDelimiter(int delimiter) {
        this.delimiter = (char)delimiter;
    }

    public char getDelimiter() {
        return delimiter;
    }

    public void setEscape(int escape) {
        this.escape = (char)escape;
    }

    public char getEscape() {
        return escape;
    }

    public void setUseEnhancer(boolean useEnhancer) {
        this.useEnhancer = useEnhancer;
    }

    public boolean isUseEnhancer() {
        return useEnhancer;
    }

    @Override
    public Collection<MetadatumDTO> contributeMetadata(PlainMetadataSourceDto t) {
        Collection<MetadatumDTO> values = null;
        if (!useEnhancer) {
            values = super.contributeMetadata(t);
        } else {
            values = new LinkedList<>();
            for (PlainMetadataKeyValueItem metadatum : t.getMetadata()) {
                if (getKey().equals(metadatum.getKey())) {
                    String[] splitted = splitToRecord(metadatum.getValue());
                    for (String value : splitted) {
                        MetadatumDTO dcValue = new MetadatumDTO();
                        dcValue.setValue(value);
                        dcValue.setElement(getField().getElement());
                        dcValue.setQualifier(getField().getQualifier());
                        dcValue.setSchema(getField().getSchema());
                        values.add(dcValue);
                    }
                }
            }
        }
        return values;
    }

    private String[] splitToRecord(String value) {
        List<String[]> rows;
        try (CSVReader csvReader = new CSVReader(new StringReader(value),
            delimiter, escape);) {
            rows = csvReader.readAll();
        } catch (IOException e) {
            //fallback, use the inpu as value
            return new String[] { value };
        }
        //must be one row
        return rows.get(0);
    }

}
