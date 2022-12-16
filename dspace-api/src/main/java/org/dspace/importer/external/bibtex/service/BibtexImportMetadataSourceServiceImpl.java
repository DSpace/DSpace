/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.bibtex.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Resource;

import org.dspace.importer.external.exception.FileSourceException;
import org.dspace.importer.external.service.components.AbstractPlainMetadataSource;
import org.dspace.importer.external.service.components.dto.PlainMetadataKeyValueItem;
import org.dspace.importer.external.service.components.dto.PlainMetadataSourceDto;
import org.jbibtex.BibTeXDatabase;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.BibTeXParser;
import org.jbibtex.Key;
import org.jbibtex.ParseException;
import org.jbibtex.Value;

/**
 * Implements a metadata importer for BibTeX files
 *
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 */
public class BibtexImportMetadataSourceServiceImpl extends AbstractPlainMetadataSource {


    /**
     * The string that identifies this import implementation as
     * MetadataSource implementation
     *
     * @return the identifying uri
     */
    @Override
    public String getImportSource() {
        return "BibTeXMetadataSource";
    }

    @Override
    protected List<PlainMetadataSourceDto> readData (InputStream
        inputStream) throws FileSourceException {
        List<PlainMetadataSourceDto> list = new ArrayList<>();
        BibTeXDatabase database;
        try {
            database = parseBibTex(inputStream);
        } catch (IOException | ParseException e) {
            throw new FileSourceException("Unable to parse file with BibTeX parser");
        }
        if (database == null || database.getEntries() == null) {
            throw new FileSourceException("File results in an empty list of metadata");
        }
        if (database.getEntries() != null) {
            for (Entry<Key, BibTeXEntry> entry : database.getEntries().entrySet()) {
                PlainMetadataSourceDto item = new PlainMetadataSourceDto();
                List<PlainMetadataKeyValueItem> keyValues = new ArrayList<>();
                item.setMetadata(keyValues);
                PlainMetadataKeyValueItem keyValueItem = new PlainMetadataKeyValueItem();
                keyValueItem.setKey(entry.getValue().getType().getValue());
                keyValueItem.setValue(entry.getKey().getValue());
                keyValues.add(keyValueItem);
                PlainMetadataKeyValueItem typeItem = new PlainMetadataKeyValueItem();
                typeItem.setKey("type");
                typeItem.setValue(entry.getValue().getType().getValue());
                keyValues.add(typeItem);
                if (entry.getValue().getFields() != null) {
                    for (Entry<Key,Value> subentry : entry.getValue().getFields().entrySet()) {
                        PlainMetadataKeyValueItem innerItem = new PlainMetadataKeyValueItem();
                        innerItem.setKey(subentry.getKey().getValue().toLowerCase());
                        String latexString = subentry.getValue().toUserString();
                        try {
                            org.jbibtex.LaTeXParser laTeXParser = new org.jbibtex.LaTeXParser();
                            List<org.jbibtex.LaTeXObject> latexObjects = laTeXParser.parse(latexString);
                            org.jbibtex.LaTeXPrinter laTeXPrinter = new org.jbibtex.LaTeXPrinter();
                            String plainTextString = laTeXPrinter.print(latexObjects);
                            innerItem.setValue(plainTextString.replaceAll("\n", " "));
                        } catch (ParseException e) {
                            innerItem.setValue(latexString);
                        }
                        keyValues.add(innerItem);
                    }
                }
                list.add(item);
            }
        }
        return list;
    }

    private BibTeXDatabase parseBibTex(InputStream inputStream) throws IOException, ParseException {
        Reader reader = new InputStreamReader(inputStream);
        BibTeXParser bibtexParser = new BibTeXParser();
        return bibtexParser.parse(reader);
    }


    /**
     * Set the MetadataFieldMapping containing the mapping between RecordType
     * (in this case PlainMetadataSourceDto.class) and Metadata
     *
     * @param metadataFieldMap The configured MetadataFieldMapping
     */
    @Override
    @SuppressWarnings("unchecked")
    @Resource(name = "bibtexMetadataFieldMap")
    public void setMetadataFieldMap(@SuppressWarnings("rawtypes") Map metadataFieldMap) {
        super.setMetadataFieldMap(metadataFieldMap);
    }

}
