/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

import javax.servlet.http.HttpServletRequest;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

public interface PostLoggedOutAction {

	public void loggedOut(Context context, HttpServletRequest request,
			EPerson eperson);
}
