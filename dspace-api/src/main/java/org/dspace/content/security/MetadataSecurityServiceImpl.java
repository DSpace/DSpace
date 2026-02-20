/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.security;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.service.MetadataExposureService;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.security.service.MetadataSecurityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.services.ConfigurationService;
import org.dspace.services.RequestService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link MetadataSecurityService}.
 *
 * @author Mykhaylo Boychuk (4science.it)
 * @author Luca Giamminonni (4science.it)
 */
public class MetadataSecurityServiceImpl implements MetadataSecurityService {

    @Autowired
    private ItemService itemService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private MetadataExposureService metadataExposureService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private ConfigurationService configurationService;

    private DCInputsReader dcInputsReader;

    @PostConstruct
    private void setup() throws DCInputsReaderException {
        this.dcInputsReader = new DCInputsReader();
    }

    @Override
    public List<MetadataValue> getPermissionFilteredMetadataValues(Context context, Item item) {
        List<MetadataValue> values = itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY, Item.ANY, true);
        return getPermissionFilteredMetadata(context, item, values);
    }

    @Override
    public List<MetadataValue> getPermissionAndLangFilteredMetadataFields(Context context, Item item) {
        String language = context != null ? context.getCurrentLocale().getLanguage() : Item.ANY;

        List<MetadataValue> values = itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY, language, true);
        return getPermissionFilteredMetadata(context, item, values);
    }

    private List<MetadataValue> getPermissionFilteredMetadata(Context context, Item item,
                                                              List<MetadataValue> metadataValues) {

        if (item.isWithdrawn() && isNotAdmin(context, item)) {
            return List.of();
        }

        Optional<List<DCInputSet>> inputs = submissionDefinitionInputs();
        if (inputs.isPresent()) {
            return getFromSubmission(context, item, inputs.get(), metadataValues);
        }

        return metadataValues.stream()
                             .filter(value -> isMetadataValueVisible(context, item, value))
                             .collect(Collectors.toList());

    }

    private boolean canEditItem(Context context, Item item) {
        if (context == null) {
            return false;
        }
        try {
            return this.itemService.canEdit(context, item);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isMetadataValueVisible(Context context, Item item, MetadataValue value) {
        return isMetadataFieldVisible(context, item, value.getMetadataField());
    }

    private boolean isMetadataFieldVisible(Context context, Item item,
                                           MetadataField metadataField) {

        if (isPublicMetadataField(metadataField)) {
            return true;
        }

        if (isMetadataFieldVisibleFor(context, item, metadataField)) {
            return true;
        }

        return false;
    }

    private boolean isMetadataFieldVisibleFor(Context context, Item item, MetadataField metadataField) {
        return isNotHidden(context, metadataField) || canEditItem(context, item);
    }

    private boolean isPublicMetadataField(MetadataField metadataField) {

        return getPublicMetadataFromConfig().stream()
                           .anyMatch(publicField -> metadataMatch(metadataField, publicField));
    }

    private boolean metadataMatch(MetadataField metadataField, String publicField) {
        if (metadataField == null || publicField == null) {
            return false;
        }
        if (publicField.contains(".*")) {
            final String exactMatch = publicField.replace(".*", "");
            String qualifiedMatch = exactMatch + ".";
            return exactMatch.equals(metadataField.toString('.')) ||
                StringUtils.startsWith(metadataField.toString('.'), qualifiedMatch);
        } else {
            return publicField.equals(metadataField.toString('.'));
        }
    }

    private List<String> getPublicMetadataFromConfig() {
        return List.of(configurationService.getArrayProperty("metadata.publicField"));
    }

    private Optional<List<DCInputSet>> submissionDefinitionInputs() {
        return Optional.ofNullable(requestService.getCurrentRequest())
                       .map(rq -> (String) rq.getAttribute("submission-name"))
                       .map(this::dcInputsSet);
    }

    private List<DCInputSet> dcInputsSet(final String sd) {
        try {
            return dcInputsReader.getInputsBySubmissionName(sd);
        } catch (DCInputsReaderException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private boolean isNotAdmin(Context context, Item item) {
        try {
            return context == null || !authorizeService.isAdmin(context, item);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private List<MetadataValue> getFromSubmission(Context context, Item item,
                                                  final List<DCInputSet> dcInputSets,
                                                  final List<MetadataValue> metadataValues) {

        List<MetadataValue> filteredMetadataValues = new ArrayList<>();

        for (MetadataValue metadataValue : metadataValues) {
            MetadataField field = metadataValue.getMetadataField();
            if (dcInputsContainsField(dcInputSets, field)
                || isMetadataFieldVisible(context, item, field)) {
                filteredMetadataValues.add(metadataValue);
            }
        }

        return filteredMetadataValues;
    }

    private boolean dcInputsContainsField(List<DCInputSet> dcInputSets, MetadataField metadataField) {
        return dcInputSets.stream().anyMatch((input) -> input.isFieldPresent(metadataField.toString('.')));
    }

    private boolean isNotHidden(Context context, MetadataField metadataField) {
        try {
            return metadataField != null &&
                !metadataExposureService.isHidden(context, metadataField.getMetadataSchema().getName(),
                                                  metadataField.getElement(), metadataField.getQualifier());
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }
}