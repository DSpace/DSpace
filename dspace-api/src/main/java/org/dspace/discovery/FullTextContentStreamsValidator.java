/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import org.apache.log4j.Logger;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.content.Bitstream;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;

/**
 * Construct a <code>ContentStream</code> from a <code>File</code>
 */
public class FullTextContentStreamsValidator 
{
    private static final Logger log = Logger.getLogger(FullTextContentStreamsValidator.class);
    public boolean isAccessibleToAnonymousUser(Context context, Bitstream bit) {
        try {
            Group anonymous = EPersonServiceFactory.getInstance().getGroupService().findByName(context, Group.ANONYMOUS);
            return AuthorizeServiceFactory.getInstance().getAuthorizeService().getAuthorizedGroups(context, bit, Constants.READ).contains(anonymous);
        } catch (Exception e) {
            log.error("Error checking bitstream permissions" , e);
            return false;
        }
    }
}

