/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.csv.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import au.com.bytecode.opencsv.CSVReader;
import org.dspace.importer.external.exception.FileSourceException;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.contributor.MetadataContributor;
import org.dspace.importer.external.service.components.AbstractPlainMetadataSource;
import org.dspace.importer.external.service.components.dto.PlainMetadataKeyValueItem;
import org.dspace.importer.external.service.components.dto.PlainMetadataSourceDto;



public class CharacterSeparatedImportMetadataSourceServiceImpl extends AbstractPlainMetadataSource {

    private String separator = ",";

    private String escapeCharacter = "\"";

    private String importSource = "CsvMetadataSource";

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    @Override
    public String getImportSource() {
        return importSource;
    }

    public void setImportSource(String importSource) {
        this.importSource = importSource;
    }

    public void setEscapeCharacter(String escapeCharacter) {
        this.escapeCharacter = escapeCharacter;
    }

    @Override
    protected List<PlainMetadataSourceDto> readData(InputStream inputStream) throws FileSourceException {
        List<PlainMetadataSourceDto> plainMetadataList = new ArrayList<>();
        try (CSVReader csvReader = new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8),
            separator.charAt(0), escapeCharacter.charAt(0));) {
            List<String[]> lines = csvReader.readAll();
            for (String [] items : lines) {
                List<PlainMetadataKeyValueItem> keyValueList = new ArrayList<>();
                if (items != null) {
                    int size = items.length;
                    int count = 0;
                    while (count < size) {
                        PlainMetadataKeyValueItem keyValueItem = new PlainMetadataKeyValueItem();
                        keyValueItem.setKey(String.valueOf(count));
                        keyValueItem.setValue(items[count]);
                        keyValueList.add(keyValueItem);
                        count++;
                    }
                    PlainMetadataSourceDto dto = new PlainMetadataSourceDto();
                    dto.setMetadata(keyValueList);
                    plainMetadataList.add(dto);
                }
            }
        } catch (IOException e) {
            throw new FileSourceException("Error reading file", e);
        }
        return plainMetadataList;
    }

    @Override
    public void setMetadataFieldMap(Map<MetadataFieldConfig,
        MetadataContributor<PlainMetadataSourceDto>> metadataFieldMap) {
        super.setMetadataFieldMap(metadataFieldMap);
    }

}
