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

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.dspace.importer.external.exception.FileSourceException;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.contributor.MetadataContributor;
import org.dspace.importer.external.service.components.AbstractPlainMetadataSource;
import org.dspace.importer.external.service.components.MetadataSource;
import org.dspace.importer.external.service.components.dto.PlainMetadataKeyValueItem;
import org.dspace.importer.external.service.components.dto.PlainMetadataSourceDto;

/**
 * This class is an implementation of {@link MetadataSource} which extends {@link AbstractPlainMetadataSource}
 * in order to parse "character separated" files like csv, tsv, etc using the Live Import framework.
 * 
 * @author Pasquale Cavallo
 *
 */
public class CharacterSeparatedImportMetadataSourceServiceImpl extends AbstractPlainMetadataSource {

    private char separator = ',';

    private char quoteCharacter = '"';

    private char escapeCharacter = '\\';

    private Integer skipLines = 1;

    private String importSource = "CsvMetadataSource";

    /**
     * Set the number of lines to skip at the start of the file. This method is suitable,
     * for example, to skip file headers.
     * 
     * @param skipLines number of the line at the start of the file to skip.
     */
    public void setSkipLines(Integer skipLines) {
        this.skipLines = skipLines;
    }

    /**
     * 
     * @return the number of the lines to skip
     */
    public Integer getSkipLines() {
        return skipLines;
    }

    /**
     * Method to inject the separator
     * This must be the ASCII integer
     * related to the char.
     * In example, 9 for tab, 44 for comma
     */
    public void setSeparator(char separator) {
        this.separator = separator;
    }

    /**
     * Method to inject the escape character, usually ". This must be the ASCII integer
     * related to the char.
     * In example, 9 for tab, 44 for comma
     *
     */
    public void setQuoteCharacter(char quoteCharacter) {
        this.quoteCharacter = quoteCharacter;
    }

    /**
     * Method to inject the escape character, usually \. This must be the ASCII integer
     * related to the char.
     * In example, 9 for tab, 44 for comma
     * 
     */
    public void setEscapeCharacter(char escapeCharacter) {
        this.escapeCharacter = escapeCharacter;
    }

    @Override
    public String getImportSource() {
        return importSource;
    }

    /**
     * Method to set the name of the source
     */
    public void setImportSource(String importSource) {
        this.importSource = importSource;
    }


    /**
     * The method process any kind of "character separated" files, like CSV, TSV, and so on.
     * It return a List of PlainMetadataSourceDto.
     * Using the superclass methods AbstractPlainMetadataSource.getRecord(s), any of this
     * element will then be converted in an {@link org.dspace.importer.external.datamodel.ImportRecord}.

     * Columns will be identified by their position, zero based notation.
     * Separator character and escape character MUST be defined at class level. Number of lines to skip (headers)
     * could also be defined in the field skipLines.
     * 
     * @param InputStream The inputStream of the file
     * @return A list of PlainMetadataSourceDto
     * @throws FileSourceException if, for any reason, the file is not parsable

     */
    @Override
    protected List<PlainMetadataSourceDto> readData(InputStream inputStream) throws FileSourceException {
        List<PlainMetadataSourceDto> plainMetadataList = new ArrayList<>();
        CSVParser parser = new CSVParserBuilder().withSeparator(separator).withQuoteChar(quoteCharacter)
                .withEscapeChar(escapeCharacter).build();
        try (
                InputStreamReader inputReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                CSVReader csvReader = new CSVReaderBuilder(inputReader).withCSVParser(parser).build()) {
            // read all row
            List<String[]> lines = csvReader.readAll();
            int listSize = lines == null ? 0 : lines.size();
            int count = skipLines;
            // iterate over row (skipping the first skipLines)
            while (count < listSize) {
                String [] items = lines.get(count);
                List<PlainMetadataKeyValueItem> keyValueList = new ArrayList<>();
                if (items != null) {
                    int size = items.length;
                    int index = 0;
                    //iterate over column in the selected row
                    while (index < size) {
                        //create key/value item for the specifics row/column
                        PlainMetadataKeyValueItem keyValueItem = new PlainMetadataKeyValueItem();
                        keyValueItem.setKey(String.valueOf(index));
                        keyValueItem.setValue(items[index]);
                        keyValueList.add(keyValueItem);
                        index++;
                    }
                    //save all column key/value for the given row
                    PlainMetadataSourceDto dto = new PlainMetadataSourceDto();
                    dto.setMetadata(keyValueList);
                    plainMetadataList.add(dto);
                }
                count++;
            }
        } catch (IOException | CsvException e) {
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
