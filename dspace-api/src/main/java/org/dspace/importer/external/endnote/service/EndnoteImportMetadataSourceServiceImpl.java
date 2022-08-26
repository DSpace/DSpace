/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.endnote.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dspace.importer.external.exception.FileSourceException;
import org.dspace.importer.external.metadatamapping.MetadataFieldConfig;
import org.dspace.importer.external.metadatamapping.contributor.MetadataContributor;
import org.dspace.importer.external.service.components.AbstractPlainMetadataSource;
import org.dspace.importer.external.service.components.dto.PlainMetadataKeyValueItem;
import org.dspace.importer.external.service.components.dto.PlainMetadataSourceDto;

/**
 * Implements a metadata importer for Endnote files
 *
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 */
public class EndnoteImportMetadataSourceServiceImpl extends AbstractPlainMetadataSource {

    @Override
    public String getImportSource() {
        return "EndnoteMetadataSource";
    }

    /**
     * This method map the data present in the inputStream, then return a list PlainMetadataSourceDto.
     * Any PlainMetadataSourceDto will be used to create a single {@link org.dspace.importer.external.datamodel.ImportRecord}
     * 
     * @param inputStream the inputStream of the Endnote file
     * @return List of {@link org.dspace.importer.external.service.components.dto.PlainMetadataSourceDto}
     * @throws FileSourceException
     * @see org.dspace.importer.external.service.components.AbstractPlainMetadataSource
     */
    @Override
    protected List<PlainMetadataSourceDto> readData(InputStream fileInpuStream) throws FileSourceException {
        List<PlainMetadataSourceDto> list = new ArrayList<>();
        try {
            // row start from 3, because the first 2 (FN and VR) will be removed by tokenize
            int lineForDebug = 3;
            List<PlainMetadataKeyValueItem> tokenized = tokenize(fileInpuStream);
            List<PlainMetadataKeyValueItem> tmpList = new ArrayList<>();
            // iterate over key/value pairs, create a new PlainMetadataSourceDto on "ER" rows (which means "new record)
            // and stop on EF (end of file).
            for (PlainMetadataKeyValueItem item : tokenized) {
                if (item.getKey() == null || item.getKey().isEmpty()) {
                    throw new FileSourceException("Null or empty key expected on line "
                    + lineForDebug + ". Keys cannot be null nor empty");
                }
                if ("EF".equals(item.getKey())) {
                    // end of file
                    break;
                }
                if ("ER".equals(item.getKey())) {
                    // new ImportRecord start from here (ER is a content delimiter)
                    // save the previous, then create a new one
                    PlainMetadataSourceDto dto = new PlainMetadataSourceDto();
                    dto.setMetadata(new ArrayList<>(tmpList));
                    list.add(dto);
                    tmpList = new ArrayList<>();
                } else {
                    if (item.getValue() == null || item.getValue().isEmpty()) {
                        throw new FileSourceException("Null or empty value expected on line "
                        + lineForDebug + ". Value expected");
                    }
                    tmpList.add(item);
                }
                lineForDebug++;
            }
        } catch (Exception e) {
            throw new FileSourceException("Error reading file", e);
        }
        return list;
    }


    /**
     * This method iterate over file rows, split content in a list of key/value items through RexExp
     * and save the content sequentially.
     * Key "FN" and "VR", which is a preamble in Endnote, will be checked but not saved.
     * 
     * @param fileInpuStream the inputStream of the Endnote file
     * @return A list of key/value items which map the file's row sequentially
     * @throws IOException
     * @throws FileSourceException
     */
    private List<PlainMetadataKeyValueItem> tokenize(InputStream fileInpuStream)
        throws IOException, FileSourceException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(fileInpuStream));
        String line;
        line = reader.readLine();
        // FN and VR works as preamble, just check and skip them
        if (line == null || !line.startsWith("FN")) {
            throw new FileSourceException("Invalid endNote file");
        }
        line = reader.readLine();
        if (line == null || !line.startsWith("VR")) {
            throw new FileSourceException("Invalid endNote file");
        }
        // split any row into first part ^[A-Z]{2} used as key (the meaning of the data)
        // and second part ?(.*) used as value (the data)
        Pattern pattern = Pattern.compile("(^[A-Z]{2}) ?(.*)$");
        List<PlainMetadataKeyValueItem> list = new ArrayList<PlainMetadataKeyValueItem>();
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            // skip empty lines
            if (line.isEmpty() || line.equals("")) {
                continue;
            }
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                PlainMetadataKeyValueItem item = new PlainMetadataKeyValueItem();
                item.setKey(matcher.group(1));
                item.setValue(matcher.group(2));
                list.add(item);
            }
        }
        return list;
    }

    @Override
    public void setMetadataFieldMap(Map<MetadataFieldConfig,
        MetadataContributor<PlainMetadataSourceDto>> metadataFieldMap) {
        super.setMetadataFieldMap(metadataFieldMap);
    }

}
