/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.migration;

import static org.dspace.content.authority.Choices.CF_ACCEPTED;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Shared helper that applies the upsert logic for a single
 * {@link DirectionalMigrationDefinition} against a relationship.
 *
 * <p>For each relationship, this migrator:</p>
 * <ol>
 *   <li>Determines the owner and related items from the ownerSide config</li>
 *   <li>Derives the display value from the directional value
 *       (leftwardValue or rightwardValue), falling back to the related item's
 *       {@code dc.title}</li>
 *   <li>Sets the authority to the related item's UUID</li>
 *   <li>Position-based upsert against STORED metadata only (virtual metadata
 *       disabled): if a value already occupies the relationship's place it is
 *       updated with the authority; otherwise a new value is added at that place</li>
 *   <li>Uses {@code CF_ACCEPTED} as confidence</li>
 * </ol>
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class DirectionalMetadataMigrator {

    @Autowired
    private ItemService itemService;

    /**
     * Migrate a single relationship according to the given directional definition.
     *
     * @param context the DSpace context
     * @param relationship the relationship to migrate
     * @param def the directional migration definition
     * @param handler the script handler for logging
     * @throws SQLException if a database error occurs
     */
    public void migrate(Context context, Relationship relationship,
                        DirectionalMigrationDefinition def,
                        DSpaceRunnableHandler handler)
        throws SQLException {

        Item ownerItem;
        Item relatedItem;
        int place;
        String directionalValue;

        // ownerSide is validated at bean-init time by DirectionalMigrationDefinition.
        String ownerSide = def.getOwnerSide();
        boolean isRight = "RIGHT".equalsIgnoreCase(ownerSide);

        if (isRight) {
            ownerItem = relationship.getRightItem();
            relatedItem = relationship.getLeftItem();
            place = relationship.getRightPlace();
            directionalValue = relationship.getRightwardValue();
        } else {
            ownerItem = relationship.getLeftItem();
            relatedItem = relationship.getRightItem();
            place = relationship.getLeftPlace();
            directionalValue = relationship.getLeftwardValue();
        }

        String value = directionalValue;
        if (StringUtils.isBlank(value)) {
            // Fallback assumes the related item exposes dc.title; entity types
            // without dc.title yield no value here and are reported via logError below.
            value = itemService.getMetadataFirstValue(relatedItem, "dc", "title", null, Item.ANY);
        }

        if (StringUtils.isBlank(value)) {
            handler.logError("Relationship id=" + relationship.getID()
                + ": could not determine value for related item " + relatedItem.getID());
            return;
        }

        String authority = relatedItem.getID().toString();

        // Read STORED metadata only (virtual metadata disabled). The upsert must operate
        // on persistent values; if virtual metadata were included, the place lookup would
        // match the relationship's own relationship-derived (virtual) value rather than a
        // real stored value, making the result depend on runtime config.
        List<MetadataValue> existing = itemService.getMetadata(ownerItem,
            def.getSchema(), def.getElement(), def.getQualifier(), Item.ANY, false);

        // Position-based upsert: if a stored value already occupies this place, set the
        // authority on it (update); otherwise add a new value at this place.
        MetadataValue toUpdate = null;
        for (MetadataValue mv : existing) {
            if (mv.getPlace() == place) {
                toUpdate = mv;
                break;
            }
        }

        String fieldLabel = def.getSchema() + "." + def.getElement()
            + (def.getQualifier() != null ? "." + def.getQualifier() : "");

        // Ensure the target metadata field is configured for authority control.
        // Without authority.controlled.<schema>.<element>.<qualifier> = true,
        // the UI will not recognise authority values on this field.
        ConfigurationService configurationService = DSpaceServicesFactory.getInstance()
            .getConfigurationService();
        String authorityControlledKey = "authority.controlled."
            + def.getSchema() + "." + def.getElement();
        if (def.getQualifier() != null) {
            authorityControlledKey += "." + def.getQualifier();
        }
        if (!configurationService.getBooleanProperty(authorityControlledKey, false)) {
            handler.logWarning("Relationship id=" + relationship.getID()
                + ": target field " + fieldLabel + " is not configured for authority control"
                + " (" + authorityControlledKey + " not set to true)."
                + " Authority metadata will be written but may not be recognised by the UI.");
        }

        if (toUpdate != null) {
            toUpdate.setValue(value);
            toUpdate.setAuthority(authority);
            toUpdate.setConfidence(CF_ACCEPTED);
            handler.logInfo("Updated metadata at place=" + toUpdate.getPlace()
                + " for relationship id=" + relationship.getID()
                + " -> " + fieldLabel + " on item " + ownerItem.getID()
                + " [value=" + value + ", authority=" + authority + "]");
        } else {
            itemService.addMetadata(context, ownerItem, def.getSchema(), def.getElement(),
                def.getQualifier(), null, value, authority, CF_ACCEPTED, place);
            handler.logInfo("Added metadata for relationship id=" + relationship.getID()
                + " -> " + fieldLabel + " on item " + ownerItem.getID()
                + " [value=" + value + ", authority=" + authority + ", place=" + place + "]");
        }
    }
}
