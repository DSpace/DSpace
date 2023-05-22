/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.ris.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Resource;

import org.dspace.importer.external.exception.FileSourceException;
import org.dspace.importer.external.service.components.AbstractPlainMetadataSource;
import org.dspace.importer.external.service.components.dto.PlainMetadataKeyValueItem;
import org.dspace.importer.external.service.components.dto.PlainMetadataSourceDto;

/**
 * Implements a metadata importer for RIS files
 * Implementations insprider by BTE DataLoader {@link https://github.com/EKT/Biblio-Transformation-Engine/blob/master/bte-io/src/main/java/gr/ekt/bteio/loaders/RISDataLoader.java}
 * 
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 */
public class RisImportMetadataSourceServiceImpl extends AbstractPlainMetadataSource {

    @Override
    public String getImportSource() {
        return "RISMetadataSource";
    }

    @Override
    protected List<PlainMetadataSourceDto> readData(InputStream inputStream) throws FileSourceException {
        return aggregateData(inputStream);
    }

    /**
     * This method map the data present in the inputStream, then return a list PlainMetadataSourceDto.
     * Any PlainMetadataSourceDto will be used to create a single {@link org.dspace.importer.external.datamodel.ImportRecord}
     * 
     * @see org.dspace.importer.external.service.components.AbstractPlainMetadataSource
     * 
     * @param inputStream the inputStream of the RIS file
     * @return List of {@link org.dspace.importer.external.service.components.dto.PlainMetadataSourceDto}
     * @throws FileSourceException
     */
    private List<PlainMetadataSourceDto> aggregateData(InputStream inputStream) throws FileSourceException {
        List<PlainMetadataSourceDto> metadata = new ArrayList<>();
        //map any line of the field to a key/value pair
        List<PlainMetadataKeyValueItem> notAggregatedItems = notAggregatedData(inputStream);
        List<PlainMetadataKeyValueItem> aggregatedTmpList = null;
        Iterator<PlainMetadataKeyValueItem> itr = notAggregatedItems.iterator();
        // iterate over the list of key/value items
        // create a new PlainMetadataSourceDto (which map and ImportRecord)
        // any times the key is "TY" (content separator in RIS)
        while (itr.hasNext()) {
            PlainMetadataKeyValueItem item = itr.next();
            if ("TY".equals(item.getKey())) {
                if (aggregatedTmpList != null) {
                    PlainMetadataSourceDto dto = new PlainMetadataSourceDto();
                    dto.setMetadata(new ArrayList<>(aggregatedTmpList));
                    metadata.add(dto);
                }
                aggregatedTmpList = new ArrayList<>();
                aggregatedTmpList.add(item);
            } else {
                if (aggregatedTmpList != null) {
                    aggregatedTmpList.add(item);
                    // save last iteration metadata
                    if (!itr.hasNext()) {
                        PlainMetadataSourceDto dto = new PlainMetadataSourceDto();
                        dto.setMetadata(new ArrayList<>(aggregatedTmpList));
                        metadata.add(dto);
                    }
                }
            }
        }
        return metadata;
    }

    /**
     * This method transform any row of the RIS file into a PlainMetadataKeyValueItem,
     * splitting the row sequentially through a RegExp without take care of the means of the data.
     * In this way, all entries present in the file are mapped in the resulting list.
     * 
     * @param inputStream the inputStrem of the file
     * @return A list
     * @throws FileSourceException
     */
    private List<PlainMetadataKeyValueItem> notAggregatedData(InputStream inputStream) throws FileSourceException {
        LinkedList<PlainMetadataKeyValueItem> items = new LinkedList<>();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.equals("") || line.matches("^\\s*$")) {
                    continue;
                }
                //match valid RIS entry
                Pattern risPattern = Pattern.compile("^([A-Z][A-Z0-9])  - (.*)$");
                Matcher risMatcher = risPattern.matcher(line);
                if (risMatcher.matches()) {
                    PlainMetadataKeyValueItem keyValueItem = new PlainMetadataKeyValueItem();
                    keyValueItem.setValue(risMatcher.group(2));
                    keyValueItem.setKey(risMatcher.group(1));
                    items.add(keyValueItem);
                } else {
                    if (!items.isEmpty()) {
                        items.getLast().setValue(items.getLast().getValue().concat(line));
                    }
                }
            }
        } catch (Exception e) {
            throw new FileSourceException("Cannot parse RIS file", e);
        }
        return items;
    }

    /**
     * Set the MetadataFieldMapping containing the mapping between RecordType
     * (in this case PlainMetadataSourceDto.class) and Metadata
     *
     * @param metadataFieldMap The configured MetadataFieldMapping
     */
    @Override
    @SuppressWarnings("unchecked")
    @Resource(name = "risMetadataFieldMap")
    public void setMetadataFieldMap(@SuppressWarnings("rawtypes") Map metadataFieldMap) {
        super.setMetadataFieldMap(metadataFieldMap);
    }

}
