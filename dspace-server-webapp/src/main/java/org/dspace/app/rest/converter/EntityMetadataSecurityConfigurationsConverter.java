/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;


import org.dspace.app.rest.model.EntityMetadataSecurityConfigurationRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.metadataSecurity.EntityMetadataSecurityConfiguration;
import org.springframework.stereotype.Component;


/**
 * Will convert an object of EntityMetadataSecurityConfiguration
 * to an object of EntityMetadataSecurityConfigurationRest}.
 *
 * @author Alba Aliu (alba.aliu@atis.al)
 */
@Component
public class EntityMetadataSecurityConfigurationsConverter
        implements DSpaceConverter<EntityMetadataSecurityConfiguration,
        EntityMetadataSecurityConfigurationRest> {
    @Override
    public EntityMetadataSecurityConfigurationRest convert(
            final EntityMetadataSecurityConfiguration modelObject,
            final Projection projection) {
        EntityMetadataSecurityConfigurationRest entityMetadataSecurityConfigurationRest =
                new EntityMetadataSecurityConfigurationRest();
        entityMetadataSecurityConfigurationRest.setId(modelObject.getEntityType());
        entityMetadataSecurityConfigurationRest.setProjection(projection);
        entityMetadataSecurityConfigurationRest.setMetadataSecurityDefault(modelObject.getMetadataSecurityDefault());
        entityMetadataSecurityConfigurationRest.setMetadataCustomSecurity(modelObject.getMetadataCustomSecurity());
        return entityMetadataSecurityConfigurationRest;
    }

    @Override
    public Class<EntityMetadataSecurityConfiguration> getModelClass() {
        return EntityMetadataSecurityConfiguration.class;
    }
}
