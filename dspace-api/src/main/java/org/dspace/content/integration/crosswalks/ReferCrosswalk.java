/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.collections4.iterators.EmptyIterator.emptyIterator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.CrosswalkMode;
import org.dspace.content.crosswalk.CrosswalkObjectNotSupported;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.integration.crosswalks.evaluators.ConditionEvaluator;
import org.dspace.content.integration.crosswalks.evaluators.ConditionEvaluatorMapper;
import org.dspace.content.integration.crosswalks.model.TemplateLine;
import org.dspace.content.integration.crosswalks.virtualfields.VirtualField;
import org.dspace.content.integration.crosswalks.virtualfields.VirtualFieldMapper;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.CrisConstants;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResultIterator;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private DiscoveryConfigurationService searchConfigurationService;

    @Autowired
    private VirtualFieldMapper virtualFieldMapper;

    @Autowired
    private ConditionEvaluatorMapper conditionEvaluatorMapper;

    private Converter<String, String> converter;

    private Consumer<List<String>> linesPostProcessor;

    private String multipleItemsTemplateFileName;

    private String templateFileName;

    private String mimeType;

    private String fileName;

    private String entityType;


    private List<TemplateLine> templateLines;

    private List<TemplateLine> multipleItemsTemplateLines;

    private CrosswalkMode crosswalkMode;

    @PostConstruct
    private void postConstruct() throws IOException {
        String parent = configurationService.getProperty("dspace.dir") + File.separator + "config" + File.separator;
        File templateFile = new File(parent, templateFileName);
        this.templateLines = readTemplateLines(templateFile);

        if (StringUtils.isNotBlank(multipleItemsTemplateFileName)) {
            File multipleItemsTemplateFile = new File(parent, multipleItemsTemplateFileName);
            this.multipleItemsTemplateLines = readTemplateLines(multipleItemsTemplateFile);
        }
    }

    @Override
    public void disseminate(Context context, DSpaceObject dso, OutputStream out)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {

        if (!canDisseminate(context, dso)) {
            throw new CrosswalkObjectNotSupported("Can only crosswalk an Item with the configured type: " + entityType);
        }

        List<String> lines = getItemLines(context, dso, true);

        if (linesPostProcessor != null) {
            linesPostProcessor.accept(lines);
        }

        writeLines(out, lines);

    }

    @Override
    public void disseminate(Context context, Iterator<? extends DSpaceObject> dsoIterator, OutputStream out)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {

        if (CollectionUtils.isEmpty(multipleItemsTemplateLines)) {
            throw new UnsupportedOperationException("No template defined for multiple items");
        }

        List<String> lines = new ArrayList<String>();

        for (TemplateLine line : multipleItemsTemplateLines) {

            if (line.isTemplateField()) {

                while (dsoIterator.hasNext()) {

                    DSpaceObject dso = dsoIterator.next();
                    if (!canDisseminate(context, dso)) {
                        throw new CrosswalkObjectNotSupported(
                            "Can only crosswalk items with the configured type: " + entityType);
                    }

                    List<String> singleTemplateLines = getSingleItemLines(context, dso, line);
                    for (String singleTemplateLine : singleTemplateLines) {
                        lines.add(line.getBeforeField() + singleTemplateLine);
                    }

                }

            } else {
                lines.add(line.getBeforeField());
            }
        }

        if (linesPostProcessor != null) {
            linesPostProcessor.accept(lines);
        }

        writeLines(out, lines);

    }

    @Override
    public boolean canDisseminate(Context context, DSpaceObject dso) {
        return dso.getType() == Constants.ITEM && hasExpectedEntityType((Item) dso);
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public String getMIMEType() {
        return mimeType;
    }

    private List<TemplateLine> readTemplateLines(File templateFile) throws IOException, FileNotFoundException {
        try (BufferedReader templateReader = new BufferedReader(new FileReader(templateFile))) {
            return templateReader.lines()
                .map(this::buildTemplateLine)
                .collect(Collectors.toList());
        }
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

        if (templateLineObj.isIfGroupField()) {
            String conditionName = templateLineObj.getIfConditionName();
            if (!conditionEvaluatorMapper.contains(conditionName)) {
                throw new IllegalStateException("Unknown condition evaluator found in the template '" + templateFileName
                    + "': " + conditionName);
            }
        }

        return templateLineObj;
    }

    private List<String> getItemLines(Context context, DSpaceObject dso, boolean findRelatedItems)
        throws CrosswalkObjectNotSupported, IOException {

        if (dso.getType() != Constants.ITEM) {
            throw new CrosswalkObjectNotSupported("ReferCrosswalk can only crosswalk an Item.");
        }

        Item item = (Item) dso;

        List<String> lines = new ArrayList<String>();
        appendLines(context, item, templateLines.iterator(), lines, findRelatedItems);

        return lines;
    }

    private List<String> getSingleItemLines(Context context, DSpaceObject dso, TemplateLine line)
        throws CrosswalkObjectNotSupported, IOException {

        List<String> singleItemLines = getItemLines(context, dso, false);
        if (singleItemLines.size() > 0) {
            String lastLine = singleItemLines.get(singleItemLines.size() - 1);
            singleItemLines.set(singleItemLines.size() - 1, lastLine + line.getAfterField());
        }

        return singleItemLines;
    }

    private void appendLines(Context context, Item item, Iterator<TemplateLine> iterator, List<String> lines,
        boolean findRelatedItems) throws IOException {

        while (iterator.hasNext()) {

            TemplateLine templateLine = iterator.next();

            if (templateLine.isMetadataGroupStartField()) {
                handleMetadataGroup(context, item, iterator, templateLine.getMetadataGroupFieldName(), lines);
                continue;
            }

            if (templateLine.isRelationGroupStartField()) {
                handleRelationGroup(context, item, iterator, templateLine.getRelationName(), lines, findRelatedItems);
                continue;
            }

            if (templateLine.isIfGroupStartField()) {
                handleIfGroup(context, item, iterator, templateLine, lines, findRelatedItems);
                continue;
            }

            if (StringUtils.isBlank(templateLine.getField())) {
                lines.add(templateLine.getBeforeField());
                continue;
            }

            List<String> metadataValues = getMetadataValuesForLine(context, templateLine, item);
            for (String metadataValue : metadataValues) {
                appendLine(lines, templateLine, metadataValue);
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

    private void handleMetadataGroup(Context context, Item item, Iterator<TemplateLine> iterator, String groupName,
        List<String> lines) throws IOException {

        List<TemplateLine> groupLines = getGroupLines(iterator, line -> line.isMetadataGroupEndField());
        int groupSize = getMetadataGroupSize(item, groupName);

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
                    log.warn("The cardinality of metadata group " + groupName + " is inconsistent for item with id "
                        + item.getID());
                    continue;
                }

                String metadataValue = metadata.get(i);
                if (!CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE.equals(metadataValue)) {
                    appendLine(lines, line, metadataValue);
                }

            }
        }

    }

    private void handleRelationGroup(Context context, Item item, Iterator<TemplateLine> iterator, String relationName,
        List<String> lines, boolean findRelatedItems) throws IOException {

        List<TemplateLine> groupLines = getGroupLines(iterator, line -> line.isRelationGroupEndField(relationName));

        if (!findRelatedItems) {
            return;
        }

        Iterator<Item> relatedItems = findRelatedItems(context, item, relationName);

        while (relatedItems.hasNext()) {
            Item relatedItem = relatedItems.next();
            Iterator<TemplateLine> lineIterator = groupLines.iterator();
            appendLines(context, relatedItem, lineIterator, lines, findRelatedItems);
        }

    }

    private void handleIfGroup(Context context, Item item, Iterator<TemplateLine> iterator, TemplateLine conditionLine,
        List<String> lines, boolean findRelatedItems) throws IOException {

        String condition = conditionLine.getIfCondition();
        String conditionName = conditionLine.getIfConditionName();

        List<TemplateLine> groupLines = getGroupLines(iterator, line -> line.isIfGroupEndField(condition));

        ConditionEvaluator evaluator = conditionEvaluatorMapper.getConditionEvaluator(conditionName);
        if (evaluator.test(context, item, condition)) {
            appendLines(context, item, groupLines.iterator(), lines, findRelatedItems);
        }

    }

    private List<TemplateLine> getGroupLines(Iterator<TemplateLine> iterator, Predicate<TemplateLine> breakPredicate) {
        List<TemplateLine> templateLines = new ArrayList<TemplateLine>();
        while (iterator.hasNext()) {
            TemplateLine line = iterator.next();
            if (breakPredicate.test(line)) {
                break;
            }
            templateLines.add(line);
        }
        return templateLines;
    }

    private Iterator<Item> findRelatedItems(Context context, Item item, String relationName) {

        if (isMetadataField(relationName)) {
            return findByAuthorities(context, item, relationName);
        }

        return findByRelation(context, item, relationName);
    }

    private boolean isMetadataField(String relationName) {
        return relationName.contains("-");
    }

    private Iterator<Item> findByAuthorities(Context context, Item item, String metadataField) {
        return itemService.getMetadataByMetadataString(item, metadataField.replaceAll("-", ".")).stream()
            .map(MetadataValue::getAuthority)
            .filter(Objects::nonNull)
            .map(authority -> findById(context, authority))
            .filter(Objects::nonNull)
            .iterator();
    }

    private Iterator<Item> findByRelation(Context context, Item item, String relationName) {
        String entityType = itemService.getMetadataFirstValue(item, "relationship", "type", null, Item.ANY);
        if (entityType == null) {
            log.warn("The item with id " + item.getID() + " has no relationship.type. No related items is found.");
            return emptyIterator();
        }

        DiscoveryConfiguration discoveryConfiguration = findDiscoveryConfiguration(entityType, relationName);
        if (discoveryConfiguration == null) {
            log.warn("No discovery configuration found for relation " + relationName + " for item with id "
                + item.getID() + " and type " + entityType + ". No related items is found.");
            return emptyIterator();
        }

        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.setDiscoveryConfigurationName(discoveryConfiguration.getId());
        List<String> defaultFilterQueries = discoveryConfiguration.getDefaultFilterQueries();
        for (String defaultFilterQuery : defaultFilterQueries) {
            discoverQuery.addFilterQueries(MessageFormat.format(defaultFilterQuery, item.getID()));
        }

        return new DiscoverResultIterator<Item, UUID>(context, discoverQuery);
    }

    private DiscoveryConfiguration findDiscoveryConfiguration(String entityType, String relationName) {
        String configurationName = "RELATION." + entityType + "." + relationName;
        return searchConfigurationService.getDiscoveryConfigurationByName(configurationName);
    }

    private void appendLine(List<String> lines, TemplateLine line, String value) {
        String valueToAdd = converter != null ? converter.convert(value) : value;
        lines.add(line.getBeforeField() + valueToAdd + line.getAfterField());
    }

    private void writeLines(OutputStream out, List<String> lines) throws IOException {
        try (OutputStreamWriter osw = new OutputStreamWriter(out, UTF_8);
            BufferedWriter writer = new BufferedWriter(osw)) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
            writer.flush();
        }
    }

    private Item findById(Context context, String id) {
        try {
            return itemService.findByIdOrLegacyId(context, id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean hasExpectedEntityType(Item item) {
        String relationshipType = itemService.getMetadataFirstValue(item, "relationship", "type", null, Item.ANY);
        return Objects.equals(relationshipType, entityType);
    }

    public void setConverter(Converter<String, String> converter) {
        this.converter = converter;
    }

    public void setLinesPostProcessor(Consumer<List<String>> linesPostProcessor) {
        this.linesPostProcessor = linesPostProcessor;
    }

    public void setMultipleItemsTemplateFileName(String multipleItemsTemplateFileName) {
        this.multipleItemsTemplateFileName = multipleItemsTemplateFileName;
    }

    public String getTemplateFileName() {
        return templateFileName;
    }

    public void setTemplateFileName(String templateFileName) {
        this.templateFileName = templateFileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
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
