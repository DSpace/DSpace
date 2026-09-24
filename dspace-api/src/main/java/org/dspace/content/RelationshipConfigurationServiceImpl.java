/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipConfigurationService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * POC bridge: keep existing CRIS authority configuration intact. Explicit Spring
 * definitions take precedence. The fallback stores an "authority:<field>" key on
 * the relationship, rather than inferring its meaning from a surviving metadata row.
 * Repositories can replace that fallback with a bean using the same stable key.
 *
 * @author Ben Bosman (ben . bosman at atmire.com)
 */
public class RelationshipConfigurationServiceImpl implements RelationshipConfigurationService {
    private static final String AUTHORITY_PREFIX = "authority:";

    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;
    @Autowired
    private ItemService itemService;
    @Autowired(required = false)
    private List<RelationshipTypeConfiguration> configurations = new ArrayList<>();

    @Override
    public RelationshipTypeConfiguration findForMetadataValue(MetadataValue value) {
        String field = fieldName(value);
        RelationshipTypeConfiguration match = null;
        for (RelationshipTypeConfiguration candidate : configurations) {
            if (candidate.mapsField(field)) {
                if (match != null) {
                    throw new IllegalArgumentException("Ambiguous relationship configuration for field " + field);
                }
                match = candidate;
            }
        }
        return match != null ? match : fromAuthority(field);
    }

    @Override
    public RelationshipTypeConfiguration getByKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Missing relationship configuration key");
        }
        RelationshipTypeConfiguration match = null;
        for (RelationshipTypeConfiguration candidate : configurations) {
            if (key.equals(candidate.getId())) {
                if (match != null) {
                    throw new IllegalArgumentException("Duplicate relationship configuration key " + key);
                }
                match = candidate;
            }
        }
        if (match == null && key.startsWith(AUTHORITY_PREFIX)) {
            match = fromAuthority(key.substring(AUTHORITY_PREFIX.length()));
        }
        if (match == null) {
            throw new IllegalArgumentException("Unknown relationship configuration " + key);
        }
        return match;
    }

    private RelationshipTypeConfiguration fromAuthority(String field) {
        String fieldKey = field.replace('.', '_');
        if (!choiceAuthorityService.isChoicesConfigured(fieldKey, Constants.ITEM, (Collection) null)) {
            return null;
        }
        String targetType = choiceAuthorityService.getLinkedEntityType(fieldKey);
        if (targetType == null || targetType.isBlank()) {
            return null;
        }
        RelationshipTypeConfiguration configuration = new RelationshipTypeConfiguration();
        configuration.setId(AUTHORITY_PREFIX + field);
        configuration.setLeftMetadataField(field);
        // This compatibility adapter mirrors the existing single entity-type lookup.
        // Use an explicit Spring definition for multiple allowed types or inverse anchors.
        configuration.setRightEntityTypes(List.of(targetType));
        return configuration;
    }

    @Override
    public void validate(Context context, RelationshipTypeConfiguration configuration, Item left, Item right)
        throws SQLException {
        if (left == null || right == null || configuration == null || configuration.getId() == null) {
            throw new IllegalArgumentException("A relationship requires two items and configured semantics");
        }
        if (!left.isArchived() || !right.isArchived()) {
            throw new IllegalArgumentException("Concrete relationships require two archived items");
        }
        validateType(configuration.getLeftEntityTypes(), left, "left");
        validateType(configuration.getRightEntityTypes(), right, "right");
    }

    private void validateType(List<String> allowed, Item item, String side) {
        if (!allowed.isEmpty() && !allowed.contains(itemService.getEntityTypeLabel(item))) {
            throw new IllegalArgumentException("Item type is not allowed on the " + side + " side of the relationship");
        }
    }

    public static String fieldName(MetadataValue value) {
        return value.getSchema() + "." + value.getElement()
            + (value.getQualifier() == null ? "" : "." + value.getQualifier());
    }
}
