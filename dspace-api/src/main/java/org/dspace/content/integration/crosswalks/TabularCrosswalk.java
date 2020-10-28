/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.CrosswalkMode;
import org.dspace.content.crosswalk.CrosswalkObjectNotSupported;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.integration.crosswalks.model.TabularTemplateLine;
import org.dspace.content.integration.crosswalks.virtualfields.VirtualField;
import org.dspace.content.integration.crosswalks.virtualfields.VirtualFieldMapper;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.CrisConstants;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract class that implements {@StreamDisseminationCrosswalk} that provided common logic to
 * export items in tabular format.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public abstract class TabularCrosswalk implements StreamDisseminationCrosswalk, FileNameDisseminator {

    private static Logger log = Logger.getLogger(TabularCrosswalk.class);

    @Autowired
    protected ConfigurationService configurationService;

    @Autowired
    protected ItemService itemService;

    @Autowired
    protected VirtualFieldMapper virtualFieldMapper;


    protected DCInputsReader dcInputsReader;

    protected SubmissionConfigReader submissionConfigReader;


    protected String fileName;

    protected String templateFileName;

    private String entityType;

    private CrosswalkMode crosswalkMode;


    protected List<TabularTemplateLine> templateLines;


    public TabularCrosswalk() {

        try {
            this.dcInputsReader = new DCInputsReader();
            this.submissionConfigReader = new SubmissionConfigReader();
        } catch (DCInputsReaderException | SubmissionConfigReaderException e) {
            throw new RuntimeException(e);
        }
    }

    @PostConstruct
    private void postConstruct() throws IOException {
        String parent = configurationService.getProperty("dspace.dir") + File.separator + "config" + File.separator;
        File templateFile = new File(parent, templateFileName);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(templateFile)))) {
            templateLines = reader.lines()
                .map(TabularTemplateLine::fromLine)
                .collect(Collectors.toList());
        }
    }

    /**
     * Write the given rows into the given outputstream in a specific format.
     *
     * @param rows the rows to write
     * @param out the OutputStream to write into
     */
    protected abstract void writeRows(List<List<String>> rows, OutputStream out);

    /**
     * Returns the separator string of the values of the same field.
     *
     * @return the separator
     */
    protected abstract String getValuesSeparator();

    /**
     * Returns the separator string of the values of different metadata groups.
     *
     * @return the separator
     */
    protected abstract String getNestedValuesSeparator();

    /**
     * Returns the separator string of the values of the same metadata group.
     *
     * @return the separator
     */
    protected abstract String getInsideNestedSeparator();

    /**
     * Method to escape specific characters of the given value before to put it in a row.
     *
     * @return the string with the escaped chars
     */
    protected abstract String escapeValue(String value);

    @Override
    public void disseminate(Context context, DSpaceObject dso, OutputStream out)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {
        this.disseminate(context, Arrays.asList(dso).iterator(), out);
    }

    @Override
    public void disseminate(Context context, Iterator<? extends DSpaceObject> dsoIterator, OutputStream out)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {

        List<List<String>> rows = new ArrayList<>();
        rows.add(getHeader());

        while (dsoIterator.hasNext()) {
            DSpaceObject dso = dsoIterator.next();
            if (!canDisseminate(context, dso)) {
                throw new CrosswalkObjectNotSupported(
                    "Can only crosswalk an Item with the configured type: " + entityType);
            }
            rows.add(getRow(context, dso));
        }

        writeRows(rows, out);
    }

    @Override
    public boolean canDisseminate(Context context, DSpaceObject dso) {
        return dso.getType() == Constants.ITEM && hasExpectedEntityType((Item) dso);
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    public void setDCInputsReader(DCInputsReader dcInputsReader) {
        this.dcInputsReader = dcInputsReader;
    }

    private List<String> getHeader() {
        return templateLines.stream()
            .map(TabularTemplateLine::getLabel)
            .collect(Collectors.toList());
    }

    private List<String> getRow(Context context, DSpaceObject dso) throws CrosswalkObjectNotSupported {
        if (dso.getType() != Constants.ITEM) {
            throw new CrosswalkObjectNotSupported("Can only crosswalk an Item.");
        }

        Item item = (Item) dso;

        List<String> fields = new ArrayList<String>();
        for (TabularTemplateLine templateLine : templateLines) {
            List<String> metadataValue = getMetadataValuesForLine(context, templateLine, item);
            String separator = templateLine.isMetadataGroupField() ? getNestedValuesSeparator() : getValuesSeparator();
            fields.add(String.join(separator, metadataValue));
        }

        return fields.stream()
            .map(column -> escapeValue(column))
            .collect(Collectors.toList());
    }

    private List<String> getMetadataValuesForLine(Context context, TabularTemplateLine line, Item item) {
        if (line.isVirtualField()) {
            return getVirtualFieldValues(context, line, item);
        } else if (line.isMetadataGroupField()) {
            return getMetadataGroupValues(context, line, item);
        } else {
            return getMetadataValues(item, line.getField());
        }
    }

    private List<String> getVirtualFieldValues(Context context, TabularTemplateLine line, Item item) {
        VirtualField virtualField = virtualFieldMapper.getVirtualField(line.getVirtualFieldName());
        String[] values = virtualField.getMetadata(context, item, line.getField());
        return values != null ? Arrays.asList(values) : Collections.emptyList();
    }

    private List<String> getMetadataGroupValues(Context context, TabularTemplateLine line, Item item) {
        String metadataGroupFieldName = line.getMetadataGroupFieldName();

        List<String> metadataGroup = getMetadataGroup(item, metadataGroupFieldName);
        if (CollectionUtils.isEmpty(metadataGroup)) {
            return new ArrayList<>();
        }

        int groupSize = getMetadataGroupSize(item, metadataGroupFieldName);

        Map<String, List<String>> metadataValues = new HashMap<>();

        List<String> metadataGroupValues = new ArrayList<>();
        for (int i = 0; i < groupSize; i++) {

            String metadataGroupValue = "";
            for (String metadataGroupEntry : metadataGroup) {

                List<String> metadata = null;
                if (metadataValues.containsKey(metadataGroupEntry)) {
                    metadata = metadataValues.get(metadataGroupEntry);
                } else {
                    metadata = getMetadataValues(item, metadataGroupEntry);
                    metadataValues.put(metadataGroupEntry, metadata);
                }

                if (metadata.size() <= i) {
                    log.warn("The cardinality of metadata group " + metadataGroupFieldName
                        + " is inconsistent for item with id " + item.getID());
                    continue;
                }

                String metadataValue = metadata.get(i);
                if (!CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE.equals(metadataValue)) {
                    metadataGroupValue = metadataGroupValue + metadataValue;
                }
                metadataGroupValue = metadataGroupValue + getInsideNestedSeparator();

            }
            metadataGroupValues.add(removeLastSeparators(metadataGroupValue, getInsideNestedSeparator()));

        }

        return metadataGroupValues;
    }

    private String removeLastSeparators(String value, String separator) {
        String newValue = StringUtils.removeEnd(value, separator);
        return newValue.equals(value) ? value : removeLastSeparators(newValue, separator);
    }

    private List<String> getMetadataGroup(Item item, String groupName) {
        try {
            String submissionName = this.submissionConfigReader
                .getSubmissionConfigByCollection(item.getOwningCollection()).getSubmissionName();
            String formName = submissionName + "-" + groupName.replaceAll("\\.", "-");

            if (!this.dcInputsReader.hasFormWithName(formName)) {
                return new ArrayList<String>();
            }

            return this.dcInputsReader.getAllFieldNamesByFormName(formName);

        } catch (DCInputsReaderException e) {
            log.error("An error occurs reading the input configuration by group name " + groupName, e);
            return new ArrayList<String>();
        }
    }

    private List<String> getMetadataValues(Item item, String metadata) {
        return itemService.getMetadataByMetadataString(item, metadata).stream()
            .map(MetadataValue::getValue)
            .collect(Collectors.toList());
    }

    private int getMetadataGroupSize(Item item, String metadataGroupFieldName) {
        return itemService.getMetadataByMetadataString(item, metadataGroupFieldName).size();
    }

    private boolean hasExpectedEntityType(Item item) {
        String relationshipType = itemService.getMetadataFirstValue(item, "relationship", "type", null, Item.ANY);
        return Objects.equals(relationshipType, entityType);
    }

    public void setTemplateFileName(String templateFileName) {
        this.templateFileName = templateFileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Optional<String> getEntityType() {
        return Optional.ofNullable(entityType);
    }

    public void setCrosswalkMode(CrosswalkMode crosswalkMode) {
        this.crosswalkMode = crosswalkMode;
    }

    public CrosswalkMode getCrosswalkMode() {
        return this.crosswalkMode != null ? this.crosswalkMode : StreamDisseminationCrosswalk.super.getCrosswalkMode();
    }
}
