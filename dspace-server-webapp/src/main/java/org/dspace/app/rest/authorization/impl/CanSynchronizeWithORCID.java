/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The synchronization with ORCID feature. It can be used to verify
 * if the user can synchronize with ORCID.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
@Component
@AuthorizationFeatureDocumentation(name = CanSynchronizeWithORCID.NAME,
    description = "It can be used to verify if the user can synchronize with ORCID")
public class CanSynchronizeWithORCID implements AuthorizationFeature {

    public static final String NAME = "canSynchronizeWithORCID";

    @Autowired
    private ItemService itemService;

    @Autowired
    private ConfigurationService configurationService;

    /**
     * This method returns true if the BaseObjectRest object is an instance of
     * {@link ItemRest}, there is a current user in the {@link Context} and it is
     * the owner of that item. Otherwise this method returns false.
     */
    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {

        EPerson ePerson = context.getCurrentUser();

        if (!(object instanceof ItemRest) || Objects.isNull(ePerson)) {
            return false;
        }

        String id = ((ItemRest) object).getId();
        Item item = itemService.find(context, UUID.fromString(id));

        return isOrcidSynchronizationEnabled() && isDspaceObjectOwner(ePerson, item);
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] { ItemRest.CATEGORY + "." + ItemRest.NAME };
    }

    private boolean isOrcidSynchronizationEnabled() {
        return configurationService.getBooleanProperty("orcid.synchronization-enabled", true);
    }

    /**
     * This method returns true if the given eperson is not null and if the given
     * item has the metadata field dspace.object.owner with an authority equals to
     * the uuid of the eperson. Otherwise this method returns false.
     */
    private boolean isDspaceObjectOwner(EPerson eperson, Item item) {
        if (eperson == null) {
            return false;
        }
        List<MetadataValue> owners = itemService.getMetadataByMetadataString(item, "dspace.object.owner");
        Predicate<MetadataValue> checkOwner = v -> StringUtils.equals(v.getAuthority(), eperson.getID().toString());
        return owners.stream().anyMatch(checkOwner);
    }
}
