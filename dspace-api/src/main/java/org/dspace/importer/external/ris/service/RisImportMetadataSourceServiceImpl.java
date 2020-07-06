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

    protected List<PlainMetadataSourceDto> readData(InputStream inputStream) throws FileSourceException {
        return aggreageteData(inputStream);
    }

    private List<PlainMetadataSourceDto> aggreageteData(InputStream inputStream) throws FileSourceException {
        List<PlainMetadataSourceDto> metadata = new ArrayList<>();
        List<PlainMetadataKeyValueItem> notAggregatedItems = notAggregatedData(inputStream);
        List<PlainMetadataKeyValueItem> aggregatedTmpList = null;
        Iterator<PlainMetadataKeyValueItem> itr = notAggregatedItems.iterator();
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
            throw new FileSourceException("Cannot parse RIS file");
        }
        return items;
    }

    /**
     * Retrieve the MetadataFieldMapping containing the mapping between RecordType
     * (in this case PlainMetadataSourceDto.class) and Metadata
     *
     * @return The configured MetadataFieldMapping
     */
    @Override
    @SuppressWarnings("unchecked")
    @Resource(name = "risMetadataFieldMap")
    public void setMetadataFieldMap(@SuppressWarnings("rawtypes") Map metadataFieldMap) {
        super.setMetadataFieldMap(metadataFieldMap);
    }

}
