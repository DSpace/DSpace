package org.dspace.authenticate;

import javax.servlet.http.HttpServletRequest;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

public interface PostLoggedOutAction {

	public void loggedOut(Context context, HttpServletRequest request,
			EPerson eperson);
}
