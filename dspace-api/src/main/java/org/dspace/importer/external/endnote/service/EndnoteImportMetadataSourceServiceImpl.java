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

public class EndnoteImportMetadataSourceServiceImpl extends AbstractPlainMetadataSource {

    @Override
    public String getImportSource() {
        return "EndnoteMetadataSource";
    }

    @Override
    protected List<PlainMetadataSourceDto> readData(InputStream fileInpuStream) throws FileSourceException {
        List<PlainMetadataSourceDto> list = new ArrayList<>();
        try {
            int lineForDebug = 3;
            List<PlainMetadataKeyValueItem> tokenized = tokenize(fileInpuStream);
            List<PlainMetadataKeyValueItem> tmpList = new ArrayList<>();
            for (PlainMetadataKeyValueItem item : tokenized) {
                if (item.getKey() == null || item.getKey().isEmpty()) {
                    throw new FileSourceException("Null or empty key expected on line "
                    + lineForDebug + ". Keys cannot be null nor empty");
                }
                if ("EF".equals(item.getKey())) {
                    break;
                }
                if ("ER".equals(item.getKey())) {
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
            throw new FileSourceException("Error reading file");
        }
        return list;
    }


    private List<PlainMetadataKeyValueItem> tokenize(InputStream fileInpuStream)
        throws IOException, FileSourceException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(fileInpuStream));
        String line;
        line = reader.readLine();
        if (line == null || !line.startsWith("FN")) {
            throw new FileSourceException("Invalid endNote file");
        }
        line = reader.readLine();
        if (line == null || !line.startsWith("VR")) {
            throw new FileSourceException("Invalid endNote file");
        }
        Pattern pattern = Pattern.compile("(^[A-Z]{2}) ?(.*)$");
        List<PlainMetadataKeyValueItem> list = new ArrayList<PlainMetadataKeyValueItem>();
        while ((line = reader.readLine()) != null) {
            line = line.trim();
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
