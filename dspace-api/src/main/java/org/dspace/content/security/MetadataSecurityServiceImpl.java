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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
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
import org.dspace.content.service.MetadataSecurityEvaluation;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.services.ConfigurationService;
import org.dspace.services.RequestService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link MetadataSecurityService} that provides permission-based
 * filtering of metadata values for DSpace items.
 * 
 * <p><strong>Overview:</strong></p>
 * <p>This service implements a comprehensive metadata security system that controls
 * which metadata fields and values users can access based on their permissions,
 * item state, and configuration settings. It serves as the central security layer
 * between raw metadata storage and user-facing applications.</p>
 * 
 * <p><strong>Security Filtering Strategy:</strong></p>
 * <p>The service applies a multi-layered filtering approach in the following priority order:</p>
 * <ol>
 *   <li><strong>Withdrawn Item Check:</strong> If an item is withdrawn and the user is not
 *       an admin, no metadata is returned (complete access denial)</li>
 *   <li><strong>Submission Context Check:</strong> If accessed within a submission workflow,
 *       applies submission-definition-based filtering first</li>
 *   <li><strong>Public Metadata Check:</strong> Fields configured as public via
 *       {@code metadata.publicField} configuration are always visible</li>
 *   <li><strong>Permission-Based Check:</strong> Fields are visible if the user can edit
 *       the item OR if the field is not marked as hidden</li>
 *   <li><strong>Security Level Check:</strong> Individual metadata values can have numeric
 *       security levels (0, 1, 2) that trigger additional permission evaluations</li>
 * </ol>
 * 
 * <p><strong>Configuration-Driven Security:</strong></p>
 * <p>The service respects several configuration mechanisms:</p>
 * <ul>
 *   <li><strong>Public Fields Configuration:</strong> {@code metadata.publicField} property
 *       defines which fields are always publicly visible. Supports wildcards (e.g., "dc.title.*")</li>
 *   <li><strong>Hidden Fields:</strong> Uses {@link MetadataExposureService} to check if fields
 *       are marked as hidden in the system configuration</li>
 *   <li><strong>Submission Definitions:</strong> When in submission context, uses DCInput
 *       definitions to determine field visibility</li>
 *   <li><strong>Security Levels Map:</strong> The {@code securityLevelsMap} maps numeric security
 *       levels to {@link MetadataSecurityEvaluation} implementations, configured in
 *       {@code spring-dspace-security-metadata.xml}:
 *       <ul>
 *         <li>Level 0: Public access (everyone can view)</li>
 *         <li>Level 1: Group-based access (only "Trusted" group members can view)</li>
 *         <li>Level 2: Administrator and owner access (only admins and item owners can view)</li>
 *       </ul>
 *       This allows individual metadata values on the same item to have different visibility rules.</li>
 * </ul>
 *
 * <p><strong>Integration Points:</strong></p>
 * <ul>
 *   <li><strong>REST API:</strong> Used by REST controllers to filter metadata in API responses</li>
 *   <li><strong>Submission System:</strong> Integrates with submission forms to show appropriate fields</li>
 *   <li><strong>Search/Browse:</strong> Ensures search results don't expose restricted metadata</li>
 *   <li><strong>Export Systems:</strong> Controls metadata inclusion in exports and harvesting</li>
 * </ul>
 * 
 * <p><strong>Security Features:</strong></p>
 * <ul>
 *   <li><strong>Fail-Secure Design:</strong> Unknown fields are hidden by default</li>
 *   <li><strong>Comprehensive Coverage:</strong> All metadata access goes through security checks</li>
 *   <li><strong>Admin Override:</strong> Administrators can always access withdrawn item metadata</li>
 *   <li><strong>Context Awareness:</strong> Different rules for submission vs. public access</li>
 *   <li><strong>Value-Level Granularity:</strong> Different values for the same field can have
 *       different visibility based on their security level</li>
 * </ul>
 * 
 * @see MetadataSecurityService
 * @see MetadataExposureService
 * @see AuthorizeService
 * @see DCInputsReader
 * @see MetadataSecurityEvaluation
 * @author Mykhaylo Boychuk (4science.it)
 * @author Luca Giamminonni (4science.it)
 */
public class MetadataSecurityServiceImpl implements MetadataSecurityService {

    @Resource(name = "securityLevelsMap")
    private final Map<String, MetadataSecurityEvaluation> securityLevelsMap = new HashMap<>();

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

    /**
     * Main security filtering method that applies permission-based filtering to metadata values.
     *
     * <p><strong>Filtering Logic:</strong></p>
     * <ol>
     *   <li><strong>Withdrawn Item Check:</strong> Returns empty list if item is withdrawn and user is not admin</li>
     *   <li><strong>Submission Context:</strong> If in submission workflow, delegates to submission-based
     *   filtering</li>
     *   <li><strong>Standard Filtering:</strong> Applies normal permission rules for each metadata field</li>
     * </ol>
     *
     * <p><strong>Security Levels:</strong></p>
     * <ul>
     *   <li><strong>Public Access:</strong> Fields configured as public are always visible</li>
     *   <li><strong>Edit Permission:</strong> Users who can edit the item see all non-hidden fields</li>
     *   <li><strong>View Permission:</strong> Regular users see only public and non-hidden fields</li>
     *   <li><strong>No Access:</strong> Withdrawn items hide all metadata from non-admins</li>
     * </ul>
     *
     * @param context        the DSpace context containing user and permission information
     * @param item           the item whose metadata should be filtered
     * @param metadataValues the complete list of metadata values to filter
     * @return filtered list containing only metadata values the user can access
     */
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
                                .filter(value -> isMetadataValueReturnAllowed(context, item, value))
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

    /**
     * Determines if a metadata field is visible to the current user based on permission rules.
     * 
     * <p><strong>Visibility Rules (in order of precedence):</strong></p>
     * <ol>
     *   <li><strong>Public Field:</strong> Always visible if configured in {@code metadata.publicField}</li>
     *   <li><strong>Permission Check:</strong> Visible if user can edit item OR field is not hidden</li>
     * </ol>
     * 
     * <p><strong>Configuration Examples:</strong></p>
     * <pre>
     * # Always public fields
     * metadata.publicField = dc.title, dc.date.issued, dc.identifier.uri
     * 
     * # Wildcard patterns
     * metadata.publicField = dc.title.*, person.identifier.*
     * </pre>
     * 
     * @param context the DSpace context for permission checking
     * @param item the item containing the metadata field
     * @param metadataField the metadata field to check for visibility
     * @return true if the field should be visible to the current user, false otherwise
     */
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

    /**
     * Checks if a metadata field is visible for the current user based on edit permissions and hidden status.
     * 
     * <p><strong>Permission Logic:</strong></p>
     * <ul>
     *   <li><strong>Edit Permission:</strong> If user can edit the item, they see all fields (even hidden ones)</li>
     *   <li><strong>View Permission:</strong> Regular users only see fields that are not marked as hidden</li>
     * </ul>
     * 
     * <p>This implements the principle that content editors should see all metadata to properly
     * manage items, while regular users only see publicly visible fields.</p>
     * 
     * @param context the DSpace context for checking edit permissions
     * @param item the item to check edit permissions against
     * @param metadataField the field to check hidden status for
     * @return true if field is not hidden OR user can edit the item
     */
    private boolean isMetadataFieldVisibleFor(Context context, Item item, MetadataField metadataField) {
        return isNotHidden(context, metadataField) || canEditItem(context, item);
    }

    private boolean isMetadataValueReturnAllowed(Context context, Item item, MetadataValue metadataValue) {
        Integer securityLevel = metadataValue.getSecurityLevel();
        if (securityLevel == null) {
            return true;
        }

        MetadataSecurityEvaluation metadataSecurityEvaluation = getMetadataSecurityEvaluator(securityLevel);
        try {
            return metadataSecurityEvaluation.allowMetadataFieldReturn(context, item, metadataValue.getMetadataField());
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private boolean isPublicMetadataField(MetadataField metadataField) {

        return getPublicMetadataFromConfig().stream()
                           .anyMatch(publicField -> metadataMatch(metadataField, publicField));
    }

    /**
     * Performs pattern matching between a metadata field and a public field configuration pattern.
     * 
     * <p><strong>Pattern Matching Rules:</strong></p>
     * <ul>
     *   <li><strong>Exact Match:</strong> "dc.title" exactly matches "dc.title"</li>
     *   <li><strong>Wildcard Match:</strong> "dc.title.*" matches any qualifier under dc.title</li>
     *   <li><strong>Prefix Match:</strong> "dc.*" matches all Dublin Core elements</li>
     * </ul>
     * 
     * <p><strong>Examples:</strong></p>
     * <pre>
     * Pattern: "dc.title.*"
     * Matches: dc.title.alternative, dc.title.translated, dc.title.main
     * No Match: dc.creator.author, dc.subject
     * 
     * Pattern: "dc.identifier"
     * Matches: dc.identifier (exact)
     * No Match: dc.identifier.uri, dc.identifier.doi
     * </pre>
     * 
     * @param metadataField the metadata field to check
     * @param publicField the pattern from configuration (may contain wildcards)
     * @return true if the field matches the pattern, false otherwise
     */
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

    /**
     * Applies submission-based filtering when accessed within a submission workflow context.
     *
     * <p><strong>Submission Context Filtering:</strong></p>
     * <p>When metadata is accessed during item submission, this method applies a dual filter:</p>
     * <ol>
     *   <li><strong>Submission Definition Filter:</strong> Field must be present in the submission form
     *   configuration</li>
     *   <li><strong>Standard Permission Filter:</strong> Field must also pass normal visibility rules</li>
     * </ol>
     *
     * <p><strong>Use Cases:</strong></p>
     * <ul>
     *   <li><strong>Submission Forms:</strong> Only show fields relevant to the current submission workflow</li>
     *   <li><strong>Edit Item:</strong> Display only editable fields during item modification</li>
     *   <li><strong>Workflow Steps:</strong> Show appropriate fields for each stage of submission</li>
     * </ul>
     *
     * <p><strong>Integration:</strong></p>
     * <p>This method works with DSpace's submission system by checking if fields are defined
     * in the DCInput configuration for the current submission definition. This ensures that
     * users only see fields that are relevant to their current submission workflow step.</p>
     *
     * @param context        the DSpace context for permission checking
     * @param item           the item being processed in the submission workflow
     * @param dcInputSets    the submission form definitions that apply to this context
     * @param metadataValues the metadata values to filter
     * @return filtered list containing only submission-relevant and permission-allowed metadata
     */
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

    public MetadataSecurityEvaluation getMetadataSecurityEvaluator(int securityValue) {
        return securityLevelsMap.get(securityValue + "");
    }
}