/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.CrosswalkObjectNotSupported;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.integration.crosswalks.model.TemplateLine;
import org.dspace.content.integration.crosswalks.model.TemplateLineGroup;
import org.dspace.content.integration.crosswalks.virtualfields.VirtualField;
import org.dspace.content.integration.crosswalks.virtualfields.VirtualFieldMapper;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.CrisConstants;
import org.dspace.services.ConfigurationService;
import org.springframework.core.convert.converter.Converter;

/**
 * Implementation of {@StreamDisseminationCrosswalk} to produce an output from
 * an Item starting from a template.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ReferCrosswalk implements StreamDisseminationCrosswalk, FileNameDisseminator {

    private static Logger log = Logger.getLogger(ReferCrosswalk.class);

    private static final Pattern FIELD_PATTERN = Pattern.compile("@[a-zA-Z0-9\\-.*]+(\\(.*\\))?@");


    private final ConfigurationService configurationService;

    private final ItemService itemService;

    private final VirtualFieldMapper virtualFieldMapper;

    private Converter<String, String> converter;

    private Consumer<List<String>> linesPostProcessor;


    private final String templateFileName;

    private final String mimeType;

    private final String fileName;

    private List<TemplateLine> templateLines;

    public ReferCrosswalk(ConfigurationService configurationService, ItemService itemService,
        VirtualFieldMapper virtualFieldMapper, String templateFileName, String mimeType, String fileName,
        String valueDelimiter) {
        super();
        this.configurationService = configurationService;
        this.itemService = itemService;
        this.virtualFieldMapper = virtualFieldMapper;
        this.templateFileName = templateFileName;
        this.mimeType = mimeType;
        this.fileName = fileName;
    }

    @PostConstruct
    private void postConstruct() throws IOException {

        String parent = configurationService.getProperty("dspace.dir") + File.separator + "config" + File.separator;
        File templateFile = new File(parent, templateFileName);

        try (FileReader fileReader = new FileReader(templateFile);
            BufferedReader templateReader = new BufferedReader(fileReader)) {

            templateLines = templateReader.lines()
                .map(this::buildTemplateLine)
                .collect(Collectors.toList());

        }
    }

    @Override
    public void disseminate(Context context, DSpaceObject dso, OutputStream out)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {

        if (dso.getType() != Constants.ITEM) {
            throw new CrosswalkObjectNotSupported("ReferCrosswalk can only crosswalk an Item.");
        }

        Item item = (Item) dso;

        List<String> lines = new ArrayList<String>();
        appendLines(context, item, lines);

        if (linesPostProcessor != null) {
            linesPostProcessor.accept(lines);
        }

        try (OutputStreamWriter osw = new OutputStreamWriter(out, UTF_8);
            BufferedWriter writer = new BufferedWriter(osw)) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
            writer.flush();
        }

    }

    @Override
    public boolean canDisseminate(Context context, DSpaceObject dso) {
        return dso.getType() == Constants.ITEM;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public String getMIMEType() {
        return mimeType;
    }

    private TemplateLine buildTemplateLine(String templateLine) {

        Matcher matcher = FIELD_PATTERN.matcher(templateLine);
        if (!matcher.find()) {
            return new TemplateLine(templateLine);
        }

        String beforeField = templateLine.substring(0, matcher.start());
        String afterField = templateLine.substring(matcher.end());
        String field = templateLine.substring(matcher.start() + 1, matcher.end() - 1);

        TemplateLine templateLineObj = new TemplateLine(beforeField, afterField, field);
        if (templateLineObj.isVirtualField()) {
            String virtualFieldName = templateLineObj.getVirtualFieldName();
            if (!virtualFieldMapper.contains(virtualFieldName)) {
                throw new IllegalStateException("Unknown virtual field found in the template '" + templateFileName
                    + "': " + virtualFieldName);
            }
        }

        return templateLineObj;
    }

    private void appendLines(Context context, Item item, List<String> lines) throws IOException {
        TemplateLineGroup currentGroup = null;

        for (TemplateLine line : templateLines) {

            if (line.isMetadataGroupStartField()) {
                String groupName = line.getMetadataGroupFieldName();
                currentGroup = new TemplateLineGroup(groupName, getMetadataGroupSize(item, groupName));
                continue;
            }

            if (line.isMetadataGroupEndField()) {
                appendMetadataGroupLines(context, item, currentGroup, lines);
                currentGroup = null;
                continue;
            }

            if (currentGroup != null) {
                currentGroup.addTemplateLines(line);
                continue;
            }

            if (StringUtils.isBlank(line.getField())) {
                lines.add(line.getBeforeField());
                continue;
            }

            List<String> metadataValues = getMetadataValuesForLine(context, line, item);
            for (String metadataValue : metadataValues) {
                appendLine(lines, line, metadataValue);
            }
        }
    }

    private int getMetadataGroupSize(Item item, String metadataGroupFieldName) {
        return itemService.getMetadataByMetadataString(item, metadataGroupFieldName).size();
    }

    private List<String> getMetadataValuesForLine(Context context, TemplateLine line, Item item) {
        if (line.isVirtualField()) {
            VirtualField virtualField = virtualFieldMapper.getVirtualField(line.getVirtualFieldName());
            String[] values = virtualField.getMetadata(context, item, line.getField());
            return values != null ? Arrays.asList(values) : Collections.emptyList();
        } else {
            return itemService.getMetadataByMetadataString(item, line.getField()).stream()
                .map(MetadataValue::getValue)
                .collect(Collectors.toList());
        }
    }

    private void appendMetadataGroupLines(Context context, Item item, TemplateLineGroup lineGroup, List<String> lines)
        throws IOException {

        List<TemplateLine> groupLines = lineGroup.getTemplateLines();
        int groupSize = lineGroup.getGroupSize();

        Map<String, List<String>> metadataValues = new HashMap<>();

        for (int i = 0; i < groupSize; i++) {

            for (TemplateLine line : groupLines) {

                String field = line.getField();

                if (StringUtils.isBlank(line.getField())) {
                    lines.add(line.getBeforeField());
                    continue;
                }

                List<String> metadata = null;
                if (metadataValues.containsKey(field)) {
                    metadata = metadataValues.get(field);
                } else {
                    metadata = getMetadataValuesForLine(context, line, item);
                    metadataValues.put(field, metadata);
                }

                if (metadata.size() <= i) {
                    log.warn("The cardinality of metadata group " + lineGroup.getGroupName()
                        + " is inconsistent for item with id " + item.getID());
                    continue;
                }

                String metadataValue = metadata.get(i);
                if (!CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE.equals(metadataValue)) {
                    appendLine(lines, line, metadataValue);
                }

            }
        }

    }

    private void appendLine(List<String> lines, TemplateLine line, String value) {
        String valueToAdd = converter != null ? converter.convert(value) : value;
        lines.add(line.getBeforeField() + valueToAdd + line.getAfterField());
    }

    public void setConverter(Converter<String, String> converter) {
        this.converter = converter;
    }

    public void setLinesPostProcessor(Consumer<List<String>> linesPostProcessor) {
        this.linesPostProcessor = linesPostProcessor;
    }

}
