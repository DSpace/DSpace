/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authorprofile.administrative.configuration;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.content.AuthorProfile;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

/**
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class DuplicateAuthorNameVariant implements Validator {

    private static final Logger log = Logger.getLogger(DuplicateAuthorNameVariant.class);

    @Override
    public String getClientComponentClass() {
        return null;
    }

    @Override
    public boolean hasClientComponent() {
        return false;
    }

    @Override
    public String getClientComponent() {
        return null;
    }

    @Override
    public boolean validate(AuthorProfile ap,Context context,String input) {
        try {
            AuthorProfile ap2= AuthorProfile.findByVariant(context, StringUtils.strip(input.replaceAll("\\s{2,}", " ")));
            return ap2==null||(ap != null && ap.equals(ap2));
        } catch (Exception e) {
            log.error(LogManager.getHeader(context, "Error while validation duplicate author names", ""), e);

            return true;
        }
    }

    @Override
    public Message getErrorMessage(String prefix, String suffix) {
        return AbstractDSpaceTransformer.message(prefix + suffix + ".DuplicateAuthorNameVariant");
    }
}
