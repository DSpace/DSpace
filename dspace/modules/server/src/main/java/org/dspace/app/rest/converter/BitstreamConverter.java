/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.sql.SQLException;
// UMD Customization
import java.util.Date;
// End UMD Customization
import java.util.List;

// UMD Customization
import org.apache.commons.lang3.time.DateFormatUtils;
// End UMD Customization
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.CheckSumRest;
import org.dspace.app.rest.projection.Projection;
// UMD Customization
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.ResourcePolicyService;
// End UMD Customization
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
// UMD Customization
import org.dspace.content.DSpaceObject;
import org.dspace.eperson.Group;
import org.springframework.beans.factory.annotation.Autowired;
// End UMD Customization
import org.springframework.stereotype.Component;


/**
 * This is the converter from/to the Bitstream in the DSpace API data model and the REST data model
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class BitstreamConverter extends DSpaceObjectConverter<Bitstream, BitstreamRest> {
    // UMD Customization
    @Autowired(required = true)
    protected ResourcePolicyService resourcePolicyService;
    // End UMD Customization

    @Override
    public BitstreamRest convert(org.dspace.content.Bitstream obj, Projection projection) {
        BitstreamRest b = super.convert(obj, projection);
        b.setSequenceId(obj.getSequenceID());
        List<Bundle> bundles = null;
        try {
            bundles = obj.getBundles();
        } catch (SQLException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        if (bundles != null && bundles.size() > 0) {
            b.setBundleName(bundles.get(0).getName());
        }
        CheckSumRest checksum = new CheckSumRest();
        checksum.setCheckSumAlgorithm(obj.getChecksumAlgorithm());
        checksum.setValue(obj.getChecksum());
        b.setCheckSum(checksum);
        b.setSizeBytes(obj.getSizeBytes());

        // UMD Customization
        b.setEmbargoRestriction(getEmbargoRestriction(obj));
        // End UMD Customization
        return b;
    }

    @Override
    protected BitstreamRest newInstance() {
        return new BitstreamRest();
    }

    @Override
    public Class<Bitstream> getModelClass() {
        return Bitstream.class;
    }

    // UMD Customization

    /**
     * Returns the "ETD Embargo" ResourcPolicy for the given DSpaceObject,
     * or null if there is no such policy.
     *
     * @param object the DSpaceObject to check for an ETD Embargo
     * @return the "ETD Embargo" ResourcPolicy for the given DSpaceObject,
     * or null if there is no such policy.
     */
    protected ResourcePolicy getEtdEmbargo(DSpaceObject object) {
        List<ResourcePolicy> policies = object.getResourcePolicies();

        for (ResourcePolicy policy: policies) {
            Group group = policy.getGroup();
            if (group != null) {
                if (group.getName().equals("ETD Embargo")) {
                    return policy;
                }
            }
        }
        return null;
    }

    /**
     * Returns one of the following Strings, based on whether the given
     * DSpace object has an embargo-based restriction:
     *
     * <ul>
     * <li> A date string (in "yyyy-MM-dd" format) - The lift date of the embargo
     * <li> "FOREVER" - the embargo never ends
     * <li> "NONE" - there is no embargo (or the embargo lift date has passed)
     * </ul>
     *
     * @param object the DSpaceObject to get the embargo restriction of. Should
     * not be null.
     */
    protected String getEmbargoRestriction(DSpaceObject object) {
        ResourcePolicy etdEmbargoPolicy = getEtdEmbargo(object);

        if ((etdEmbargoPolicy != null) && (resourcePolicyService.isDateValid(etdEmbargoPolicy))) {
            Date liftDate = etdEmbargoPolicy.getEndDate();
            if (liftDate != null) {
                return DateFormatUtils.format(liftDate, "yyyy-MM-dd");
            } else {
                return "FOREVER";
            }
        }

        return "NONE";
    }

    // End UMD Customization
}
