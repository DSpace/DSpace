/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
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

    private char quote = '"';

    private char escape = '\\';

    /**
     * This method could be used to set the delimiter used during parse
     * If no delimiter is set, comma will be used
     */
    public void setDelimiter(char delimiter) {
        this.delimiter = delimiter;
    }

    /**
     * This method could be used to get the delimiter used in this class
     */
    public char getDelimiter() {
        return delimiter;
    }

    /**
     * This method could be used to get the quote char used in this class
     */
    public char getQuote() {
        return quote;
    }

    /**
     * This method could be used to set the quote char used during parse
     * If no quote char is set, " will be used
     */
    public void setQuote(char quote) {
        this.quote = quote;
    }

    /**
     * Method to inject the escape character, usually the ". This must be the ASCII
     * integer related to the char.
     * In example, 9 for tab, 44 for comma If no escape is set, double quote will be used
     */
    public void setEscape(char escape) {
        this.escape = escape;
    }

    /**
     * Method to get the escape character.
     * 
     */
    public char getEscape() {
        return escape;
    }

    @Override
    public Collection<MetadatumDTO> contributeMetadata(PlainMetadataSourceDto t) {
        Collection<MetadatumDTO> values = null;
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
        return values;
    }

    private String[] splitToRecord(String value) {
        List<String[]> rows;
        // For example, list of author must be: Author 1, author 2, author 3
        // if author name contains comma, is important to escape its in
        // this way: Author 1, \"Author 2, something\", Author 3
        CSVParser parser = new CSVParserBuilder().withSeparator(delimiter).withQuoteChar(quote).withEscapeChar(escape)
                .build();
        try (   Reader inputReader = new StringReader(value);
                com.opencsv.CSVReader csvReader = new CSVReaderBuilder(inputReader).withCSVParser(parser).build()) {
            rows = csvReader.readAll();
        } catch (IOException | CsvException e) {
            //fallback, use the inpu as value
            return new String[] { value };
        }
        //must be one row
        return rows.get(0);
    }

}
