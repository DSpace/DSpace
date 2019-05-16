/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.ResourcePolicyRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the Collection in the DSpace API data model and
 * the REST data model
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class CollectionConverter
    extends DSpaceObjectConverter<org.dspace.content.Collection, org.dspace.app.rest.model.CollectionRest>
    implements IndexableObjectConverter<Collection, CollectionRest> {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(CollectionConverter.class);

    @Autowired
    private BitstreamConverter bitstreamConverter;
    @Autowired
    private CollectionService collectionService;
    @Autowired
    private RequestService requestService;
    @Autowired
    private AuthorizeService authorizeService;
    @Autowired
    private ResourcePolicyConverter resourcePolicyConverter;

    @Override
    public org.dspace.content.Collection toModel(org.dspace.app.rest.model.CollectionRest obj) {
        return (org.dspace.content.Collection) super.toModel(obj);
    }

    @Override
    public CollectionRest fromModel(org.dspace.content.Collection obj) {
        CollectionRest col = (CollectionRest) super.fromModel(obj);
        Bitstream logo = obj.getLogo();
        if (logo != null) {
            col.setLogo(bitstreamConverter.convert(logo));
        }

        col.setDefaultAccessConditions(getDefaultBitstreamPoliciesForCollection(obj.getID()));

        return col;
    }

    private List<ResourcePolicyRest> getDefaultBitstreamPoliciesForCollection(UUID uuid) {

        Context context = null;
        Request currentRequest = requestService.getCurrentRequest();
        if (currentRequest != null) {
            HttpServletRequest request = currentRequest.getHttpServletRequest();
            context = ContextUtil.obtainContext(request);
        } else {
            context = new Context();
        }
        Collection collection = null;
        List<ResourcePolicy> defaultCollectionPolicies = null;
        try {
            collection = collectionService.find(context, uuid);
            defaultCollectionPolicies = authorizeService.getPoliciesActionFilter(context, collection,
                                                                                 Constants.DEFAULT_BITSTREAM_READ);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }

        List<ResourcePolicyRest> results = new ArrayList<ResourcePolicyRest>();

        for (ResourcePolicy pp : defaultCollectionPolicies) {
            ResourcePolicyRest accessCondition = resourcePolicyConverter.convert(pp);
            if (accessCondition != null) {
                results.add(accessCondition);
            }
        }
        return results;
    }

    @Override
    protected CollectionRest newInstance() {
        return new CollectionRest();
    }

    @Override
    protected Class<org.dspace.content.Collection> getModelClass() {
        return org.dspace.content.Collection.class;
    }

    @Override
    public boolean supportsModel(IndexableObject idxo) {
        return idxo instanceof Collection;
    }
}
