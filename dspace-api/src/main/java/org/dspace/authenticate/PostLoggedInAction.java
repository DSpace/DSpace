package org.dspace.authenticate;

import javax.servlet.http.HttpServletRequest;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

public interface PostLoggedInAction {

	public void loggedIn(Context context, HttpServletRequest request,
			EPerson eperson);
}
