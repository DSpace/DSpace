/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.auth;

import org.dspace.core.Constants;
import org.dspace.core.DSpaceContext;
import org.dspace.orm.entity.Eperson;
import org.dspace.orm.entity.IDSpaceObject;
import org.dspace.services.AuthorizationService;
import org.dspace.services.ContextService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * @author João Melo <jmelo@lyncode.com>
 */
public class DSpaceAuthorizationService implements AuthorizationService {
	@Autowired ContextService contextService;
	
	@Override
	public void authorizedAnyOf(IDSpaceObject dspaceObject, int[] actions)
			throws AuthorizationException {

		AuthorizationException ex = null;

        for (int i = 0; i < actions.length; i++)
        {
            try
            {
            	this.authorized(dspaceObject, actions[i]);
                return;
            } 
            catch (AuthorizationException e)
            {
                if (ex == null)
                {
                    ex = e;
                }
            }
        }

        throw new AuthorizationException(actions, contextService.getContext().getCurrentEperson(), dspaceObject);
	}

	@Override
	public void authorized(IDSpaceObject object, int action)
			throws AuthorizationException {
		authorized(object, action, true);
	}

	@Override
	public void authorized(IDSpaceObject object, int action, boolean inheritance)
			throws AuthorizationException {
		DSpaceContext c = contextService.getContext();
		if (object == null)
        {
            // action can be -1 due to a null entry
            String actionText;

            if (action == -1)
            {
                actionText = "null";
            } else
            {
                actionText = Constants.actionText[action];
            }

            Eperson e = c.getCurrentEperson();
            int userid;

            if (e == null)
            {
                userid = 0;
            } else
            {
                userid = e.getID();
            }

            throw new AuthorizationException(action, e, object);
        }

        if (!authorize(object, action, c.getCurrentEperson(), inheritance))
        {
            // denied, assemble and throw exception
            int otype = object.getType();
            int oid = object.getID();
            int userid;
            Eperson e = c.getCurrentEperson();

            if (e == null)
            {
                userid = 0;
            } else
            {
                userid = e.getID();
            }

            //            AuthorizeException j = new AuthorizeException("Denied");
            //            j.printStackTrace();
            // action can be -1 due to a null entry
            String actionText;

            if (action == -1)
            {
                actionText = "null";
            } else
            {
                actionText = Constants.actionText[action];
            }

            throw new AuthorizationException(action, e, object);
        }
	}

	private boolean authorize(IDSpaceObject o, int action,
			Eperson e, boolean useInheritance) {
		DSpaceContext c = contextService.getContext();
		
		if (o == null)
        {
            return false;
        }

        // is authorization disabled for this context?
        if (c.ignoreAuthorization())
        {
            return true;
        }

        // is eperson set? if not, userid = 0 (anonymous)
        /*
        int userid = 0;
        if (e != null)
        {
            userid = e.getID();

            // perform isAdmin check to see
            // if user is an Admin on this object
            DSpaceObject testObject = useInheritance ? o.getAdminObject(action) : null;

            if (isAdmin(c, testObject))
            {
                return true;
            }
        }

        for (ResourcePolicy rp : getPoliciesActionFilter(c, o, action))
        {
            // check policies for date validity
            if (rp.isDateValid())
            {
                if ((rp.getEPersonID() != -1) && (rp.getEPersonID() == userid))
                {
                    return true; // match
                }

                if ((rp.getGroupID() != -1)
                        && (Group.isMember(c, rp.getGroupID())))
                {
                    // group was set, and eperson is a member
                    // of that group
                    return true;
                }
            }
        }

        // default authorization is denial
        
        */ // FIXME: Finish implementation
        return false;
	}

}
