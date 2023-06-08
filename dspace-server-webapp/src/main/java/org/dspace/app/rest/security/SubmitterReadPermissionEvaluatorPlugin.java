/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * {@link RestObjectPermissionEvaluatorPlugin} class that evaluates if eperson is submitter of an item and authorize
 * accordingly.
 *
 * @author Bui Thai Hai (thaihai.bui@dlcorp.com.vn)
 */


//TODO: rename this to fit the use case
@Component
public class SubmitterReadPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private static final Logger log = LoggerFactory.getLogger(SubmitterReadPermissionEvaluatorPlugin.class);

    @Autowired
    private RequestService requestService;

    @Autowired
    ItemService its;
    @Autowired
    BundleService bds;
    @Autowired
    BitstreamService bis;

    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId, String targetType,
            DSpaceRestPermission permission) {

        DSpaceRestPermission restPermission = DSpaceRestPermission.convert(permission);
        if (!DSpaceRestPermission.READ.equals(restPermission) && restPermission != null) {
            return false;
        }
        if (!StringUtils.equalsIgnoreCase(targetType, ItemRest.NAME)
                // && !StringUtils.equalsIgnoreCase(targetType, BundleRest.NAME)
                // && !StringUtils.equalsIgnoreCase(targetType, BitstreamRest.NAME)
            ) {
            return false;
        }

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getHttpServletRequest());

        EPerson ePerson = context.getCurrentUser();
        if (ePerson == null) {
            return false;
        }

        // Allow submitter to access their item page
        UUID dsoId = UUID.fromString(targetId.toString());
        Item item;
        try {
            item = its.find(context, dsoId);
            if (item != null && item.getSubmitter().equals(ePerson)) {
                return true;
            }
            //Commenting these out since most test involving authorization expect that submitter cannot "READ" bundle
            // bitstream, leaving them in just in case we want submitters to see metadata for these sub-objects as well
            // *Note: Downloading bitstream is still blocked due to a AuthorizeException in the download process which
            //        demands explicit "READ" on bitstream
            // *Note2: uncomment line
            //        demands explicit "READ" on bitstream
            /*Bundle bundle = bds.find(context, dsoId);
            if (bundle != null && bundle.getItems().stream().anyMatch(x -> x.getSubmitter().equals(ePerson))) {
                return true;
            }
            Bitstream bitstream = bis.find(context, dsoId);
            if (bitstream != null && bitstream.getBundles().stream().anyMatch(
                x -> x.getItems().stream().anyMatch(y -> y.getSubmitter().equals(ePerson)))) {
                return true;
            }*/
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }

        return false;
    }
}
