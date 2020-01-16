/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.discovery.IndexableObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the community in the DSpace API data model and
 * the REST data model
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class CommunityConverter
    extends DSpaceObjectConverter<org.dspace.content.Community, org.dspace.app.rest.model.CommunityRest>
    implements IndexableObjectConverter<Community, CommunityRest> {

    @Autowired
    private ConverterService converter;

    @Override
    public CommunityRest convert(org.dspace.content.Community obj, Projection projection) {
        CommunityRest com = super.convert(obj, projection);
        Bitstream logo = obj.getLogo();
        if (logo != null) {
            com.setLogo(converter.toRest(logo, projection));
        }
        List<Collection> collections = obj.getCollections();
        List<CollectionRest> collectionsRest = new ArrayList<>();
        if (collections != null) {
            for (Collection col : collections) {
                collectionsRest.add(converter.toRest(col, projection));
            }
        }
        com.setCollections(collectionsRest);

        List<Community> subCommunities = obj.getSubcommunities();
        List<CommunityRest> communityRest = new ArrayList<>();
        if (subCommunities != null) {
            for (Community scom : subCommunities) {
                CommunityRest scomrest = this.convert(scom, projection);
                communityRest.add(scomrest);
            }
        }
        com.setSubCommunities(communityRest);

        return com;
    }

    @Override
    protected CommunityRest newInstance() {
        return new CommunityRest();
    }

    @Override
    public Class<org.dspace.content.Community> getModelClass() {
        return org.dspace.content.Community.class;
    }

    @Override
    public boolean supportsModel(IndexableObject idxo) {
        return idxo.getIndexedObject() instanceof Community;
    }
}
