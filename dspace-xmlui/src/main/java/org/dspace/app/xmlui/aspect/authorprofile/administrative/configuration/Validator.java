/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authorprofile.administrative.configuration;

import org.dspace.app.xmlui.wing.Message;
import org.dspace.content.AuthorProfile;
import org.dspace.core.Context;

/**
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public interface Validator {

    public String getClientComponentClass();

    public boolean hasClientComponent();

    public String getClientComponent();

    public boolean validate(AuthorProfile ap, Context context, String input);

    public Message getErrorMessage(String prefix, String suffix);
}
