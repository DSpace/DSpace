/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword;

import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.LogManager;
import org.dspace.core.Constants;
import org.dspace.authenticate.AuthenticationManager;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;
import org.apache.log4j.Logger;
import org.purl.sword.base.*;

import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

/**
 * This class offers a thin wrapper for the default DSpace
 * authentication module for the SWORD implementation
 * 
 * @author Richard Jones
 *
 */
public class SWORDAuthenticator
{
	/** logger */
	private static Logger log = Logger.getLogger(SWORDAuthenticator.class);

	/**
	 * Does the given username and password authenticate for the
	 * given DSpace Context?
	 * 
	 * @param context
	 * @param un
	 * @param pw
	 * @return	true if yes, false if not
	 */
	public boolean authenticates(Context context, String un, String pw)
	{
		int auth = AuthenticationManager.authenticate(context, un, pw, null, null);
		if (auth == AuthenticationMethod.SUCCESS)
		{
			return true;
		}
		return false;
	}

	/**
	 * Construct the context object member variable of this class
	 * using the passed IP address as part of the loggable
	 * information
	 *
	 * @param ip	the ip address of the incoming request
	 * @throws org.purl.sword.base.SWORDException
	 */
	private Context constructContext(String ip)
		throws SWORDException
	{
		try
		{
            Context context = new Context();
            // Set the session ID and IP address
            context.setExtraLogInfo("session_id=0:ip_addr=" + ip);

            return context;
        }
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
			throw new SWORDException("There was a problem with the database", e);
		}
	}

	/**
	 * Authenticate the given service document request.  This extracts
	 * the appropriate information from the request and forwards to the
	 * appropriate authentication method.
	 *
	 * @param request
	 * @throws SWORDException
	 * @throws SWORDErrorException
	 * @throws SWORDAuthenticationException
	 */
	public SWORDContext authenticate(ServiceDocumentRequest request)
			throws SWORDException, SWORDErrorException, SWORDAuthenticationException
	{
		Context context = this.constructContext(request.getIPAddress());
		SWORDContext sc = null;
		try
        {
            sc = this.authenticate(context, request);
        }
        catch (SWORDException e)
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
            throw e;
        }
        catch (SWORDErrorException e)
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
            throw e;
        }
        catch (SWORDAuthenticationException e)
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
            throw e;
        }
        catch (RuntimeException e)
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
            throw e;
        }
		return sc; 
	}

	/**
	 * Authenticate the given ATOM document request.  This extracts the
	 * appropriate information from the request, and forwards to the
	 * appropriate authentication method.
	 *
	 * @param request
	 * @throws SWORDException
	 * @throws SWORDErrorException
	 * @throws SWORDAuthenticationException
	 */
	public SWORDContext authenticate(AtomDocumentRequest request)
			throws SWORDException, SWORDErrorException, SWORDAuthenticationException
	{
		Context context = this.constructContext(request.getIPAddress());
		SWORDContext sc = null;
		try
        {
            sc = this.authenticate(context, request);
        }
        catch (SWORDException e)
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
            throw e;
        }
        catch (SWORDErrorException e)
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
            throw e;
        }
        catch (SWORDAuthenticationException e)
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
            throw e;
        }
        catch (RuntimeException e)
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
            throw e;
        }
        return sc;
    }

	/**
	 * Authenticate the incoming service document request.  Calls:
	 *
	 * authenticatate(username, password, onBehalfOf)
	 *
	 * @param request
	 * @return	a SWORDContext object containing the relevant users
	 * @throws org.purl.sword.base.SWORDAuthenticationException
	 * @throws SWORDException
	 */
	private SWORDContext authenticate(Context context, AtomDocumentRequest request)
		throws SWORDAuthenticationException, SWORDException, SWORDErrorException
	{
		return this.authenticate(context, request.getUsername(), request.getPassword(), null, request.getIPAddress());
	}

	/**
	 * Authenticate the incoming service document request.  Calls:
	 *
	 * authenticatate(username, password, onBehalfOf)
	 *
	 * @param request
	 * @return	a SWORDContext object containing the relevant users
	 * @throws org.purl.sword.base.SWORDAuthenticationException
	 * @throws SWORDException
	 */
	private SWORDContext authenticate(Context context, ServiceDocumentRequest request)
		throws SWORDAuthenticationException, SWORDException, SWORDErrorException
	{
		return this.authenticate(context, request.getUsername(), request.getPassword(), request.getOnBehalfOf(), request.getIPAddress());
	}

	/**
	 * Authenticate the deposit request.
	 *
	 * @param deposit
	 * @throws SWORDException
	 * @throws SWORDErrorException
	 * @throws SWORDAuthenticationException
	 */
	public SWORDContext authenticate(Deposit deposit)
			throws SWORDException, SWORDErrorException, SWORDAuthenticationException
	{
		Context context = this.constructContext(deposit.getIPAddress());
		SWORDContext sc = null;
		try
		{
		    sc = this.authenticate(context, deposit);
		}
        catch (SWORDException e)
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
            throw e;
        }
        catch (SWORDErrorException e)
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
            throw e;
        }
        catch (SWORDAuthenticationException e)
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
            throw e;
        }
        catch (RuntimeException e)
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
            throw e;
        }
        return sc;
	}

	/**
	 * Authenticate the incoming deposit request.  Calls:
	 *
	 * authenticate(username, password, onBehalfOf)
	 *
	 * @param deposit
	 * @return	a SWORDContext object containing the relevant users
	 * @throws SWORDAuthenticationException
	 * @throws SWORDException
	 */
	private SWORDContext authenticate(Context context, Deposit deposit)
		throws SWORDAuthenticationException, SWORDException, SWORDErrorException
	{
		return this.authenticate(context, deposit.getUsername(), deposit.getPassword(), deposit.getOnBehalfOf(), deposit.getIPAddress());
	}

	/**
	 * Authenticate the given username/password pair, in conjunction with
	 * the onBehalfOf user.  The rules are that the username/password pair
	 * must successfully authenticate the user, and the onBehalfOf user
	 * must exist in the user database.
	 *
	 * @param un
	 * @param pw
	 * @param obo
	 * @return	a SWORD context holding the various user information
	 * @throws SWORDAuthenticationException
	 * @throws SWORDException
	 */
	private SWORDContext authenticate(Context context, String un, String pw, String obo, String ip)
		throws SWORDAuthenticationException, SWORDException, SWORDErrorException
	{
		// smooth out the OnBehalfOf request, so that empty strings are
		// treated as null
		if ("".equals(obo))
		{
			obo = null;
		}

		// first find out if we support on-behalf-of deposit
		boolean mediated = ConfigurationManager.getBooleanProperty("sword-server", "on-behalf-of.enable");
		if (!mediated && obo != null)
		{
			// user is trying to do a mediated deposit on a repository which does not support it
			log.error("Attempted mediated deposit on service not configured to do so");
			throw new SWORDErrorException(ErrorCodes.MEDIATION_NOT_ALLOWED, "Mediated deposit to this service is not permitted");
		}

		log.info(LogManager.getHeader(context, "sword_authenticate", "username=" + un + ",on_behalf_of=" + obo));
		
		try
		{
			// attempt to authenticate the primary user
			SWORDContext sc = new SWORDContext();
			EPerson ep = null;
			boolean authenticated = false;
			if (this.authenticates(context, un, pw))
			{
				// if authenticated, obtain the eperson object
				ep = context.getCurrentUser();

				if (ep != null)
				{
					authenticated = true;
					sc.setAuthenticated(ep);
					 // Set any special groups - invoke the authentication mgr.
		            int[] groupIDs = AuthenticationManager.getSpecialGroups(context, null);

		            for (int i = 0; i < groupIDs.length; i++)
		            {
		                context.setSpecialGroup(groupIDs[i]);
		                log.debug("Adding Special Group id="+String.valueOf(groupIDs[i]));
		            }
					
					sc.setAuthenticatorContext(context);
					sc.setContext(context);
				}

				// if there is an onBehalfOfuser, then find their eperson
				// record, and if it exists set it.  If not, then the
				// authentication process fails
				EPerson epObo = null;
				if (obo != null)
				{
					epObo = EPerson.findByEmail(context, obo);
					if (epObo == null)
					{
						epObo = EPerson.findByNetid(context, obo);
					}

					if (epObo != null)
					{
						sc.setOnBehalfOf(epObo);
						Context oboContext = this.constructContext(ip);
						oboContext.setCurrentUser(epObo);
		                // Set any special groups - invoke the authentication mgr.
	                    int[] groupIDs = AuthenticationManager.getSpecialGroups(oboContext, null);

	                    for (int i = 0; i < groupIDs.length; i++)
	                    {
	                        oboContext.setSpecialGroup(groupIDs[i]);
	                        log.debug("Adding Special Group id="+String.valueOf(groupIDs[i]));
	                    }
						sc.setContext(oboContext);
					}
					else
					{
						authenticated = false;
						throw new SWORDErrorException(ErrorCodes.TARGET_OWNER_UKNOWN, "unable to identify on-behalf-of user: " + obo);
					}
				}
			}

			if (!authenticated)
			{
				// decide what kind of error to throw
				if (ep != null)
				{
					log.info(LogManager.getHeader(context, "sword_unable_to_set_user", "username=" + un));
					throw new SWORDAuthenticationException("Unable to authenticate the supplied used");
				}
				else
				{
					// FIXME: this shouldn't ever happen now, but may as well leave it in just in case
					// there's a bug elsewhere
					log.info(LogManager.getHeader(context, "sword_unable_to_set_on_behalf_of", "username=" + un + ",on_behalf_of=" + obo));
					throw new SWORDAuthenticationException("Unable to authenticate the onBehalfOf account");
				}
			}
			
			return sc;
		}
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
			throw new SWORDException("There was a problem accessing the repository user database", e);
		}
		catch (AuthorizeException e)
		{
			log.error("caught exception: ", e);
			throw new SWORDAuthenticationException("There was a problem authenticating or authorising the user", e);
		}
	}

	/**
	 * Can the users contained in this object's member SWORDContext
	 * make a successful submission to the selected collection.
	 *
	 * See javadocs for individual canSubmitTo methods to see the conditions
	 * which are applied in each situation
	 *
	 * @return	true if yes, false if not
	 * @throws DSpaceSWORDException
	 */
	public boolean canSubmit(SWORDService swordService, Deposit deposit, DSpaceObject dso)
		throws DSpaceSWORDException, SWORDErrorException
	{
		// get the things we need out of the service
		SWORDContext swordContext = swordService.getSwordContext();

		// determine if we can submit
		boolean submit = this.canSubmitTo(swordContext, dso);

		if (submit)
		{
			swordService.message("User is authorised to submit to collection");
		}
		else
		{
			swordService.message("User is not authorised to submit to collection");
		}

		return submit;
	}

	/**
	 * Is the authenticated user a DSpace administrator?  This translates
	 * as asking the question of whether the given eperson is a member
	 * of the special DSpace group Administrator, with id 1
	 *
	 * @param swordContext
	 * @return	true if administrator, false if not
	 * @throws SQLException
	 */
	public boolean isUserAdmin(SWORDContext swordContext)
		throws DSpaceSWORDException
	{
		try
		{
			EPerson authenticated = swordContext.getAuthenticated();
			if (authenticated != null)
			{
				return AuthorizeManager.isAdmin(swordContext.getAuthenticatorContext());
			}
			return false;
		}
		catch (SQLException e)
		{
			log.error("Caught exception: ", e);
			throw new DSpaceSWORDException(e);
		}
	}

	/**
	 * Is the given onBehalfOf user DSpace administrator?  This translates
	 * as asking the question of whether the given eperson is a member
	 * of the special DSpace group Administrator, with id 1
	 *
	 * @param swordContext
	 * @return	true if administrator, false if not
	 * @throws SQLException
	 */
	public boolean isOnBehalfOfAdmin(SWORDContext swordContext)
		throws DSpaceSWORDException
	{
		EPerson onBehalfOf = swordContext.getOnBehalfOf();
		try
		{
			if (onBehalfOf != null)
			{
				return AuthorizeManager.isAdmin(swordContext.getOnBehalfOfContext());
			}
			return false;
		}
		catch (SQLException e)
		{
			log.error("Caught exception: ", e);
			throw new DSpaceSWORDException(e);
		}
	}

	/**
	 * Is the authenticated user a member of the given group
	 * or one of its sub groups?
	 *
	 * @param group
	 */
	public boolean isUserInGroup(SWORDContext swordContext, Group group)
	{
		EPerson authenticated = swordContext.getAuthenticated();
		if (authenticated != null)
		{
			return isInGroup(group, authenticated);
		}
		return false;
	}

	/**
	 * Is the onBehalfOf user a member of the given group or
	 * one of its sub groups?
	 *
	 * @param group
	 */
	public boolean isOnBehalfOfInGroup(SWORDContext swordContext, Group group)
	{
		EPerson onBehalfOf = swordContext.getOnBehalfOf();
		if (onBehalfOf != null)
		{
			return isInGroup(group, onBehalfOf);
		}
		return false;
	}

	/**
	 * Is the given eperson in the given group, or any of the groups
	 * that are also members of that group.  This method recurses
	 * until it has exhausted the tree of groups or finds the given
	 * eperson
	 *
	 * @param group
	 * @param eperson
	 * @return	true if in group, false if not
	 */
	public boolean isInGroup(Group group, EPerson eperson)
	{
		EPerson[] eps = group.getMembers();
		Group[] groups = group.getMemberGroups();

		// is the user in the current group
		for (int i = 0; i < eps.length; i++)
		{
			if (eperson.getID() == eps[i].getID())
			{
				return true;
			}
		}

		// is the eperson in the sub-groups (recurse)
		if (groups != null && groups.length > 0)
		{
			for (int j = 0; j < groups.length; j++)
			{
				if (isInGroup(groups[j], eperson))
				{
					return true;
				}
			}
		}

		// ok, we didn't find you
		return false;
	}

	/**
	 * Get an array of all the communities that the current SWORD
	 * context will allow deposit onto in the given DSpace context
	 *
	 * The user may submit to a community if the following conditions
	 * are met:
	 *
	 * IF: the authenticated user is an administrator
	 *   AND:
	 *      (the on-behalf-of user is an administrator
	 *	 	OR the on-behalf-of user is authorised to READ
	 *	 	OR the on-behalf-of user is null)
	 * OR IF: the authenticated user is authorised to READ
	 *   AND:
	 *	     (the on-behalf-of user is an administrator
	 *		  OR the on-behalf-of user is authorised to READ
	 *		  OR the on-behalf-of user is null)
	 *
	 * @param swordContext
	 * @return	the array of allowed collections
	 * @throws DSpaceSWORDException
	 */
	public List<Community> getAllowedCommunities(SWORDContext swordContext)
		throws DSpaceSWORDException
	{
		// a community is allowed if the following conditions are met
		//
		// - the authenticated user is an administrator
		// -- the on-behalf-of user is an administrator
		// -- the on-behalf-of user is authorised to READ
		// -- the on-behalf-of user is null
		// - the authenticated user is authorised to READ
		// -- the on-behalf-of user is an administrator
		// -- the on-behalf-of user is authorised to READ
		// -- the on-behalf-of user is null
		try
		{
			// locate all the top level communities
			Context context = swordContext.getContext();
			List<Community> allowed = new ArrayList<Community>();
			Community[] comms = Community.findAllTop(context);
			for (int i = 0; i < comms.length; i++)
			{
				boolean authAllowed = false;
				boolean oboAllowed = false;

				// check for obo null
				if (swordContext.getOnBehalfOf() == null)
				{
					oboAllowed = true;
				}

				// look up the READ policy on the community.  This will include determining if the user is an administrator
				// so we do not need to check that separately
				if (!authAllowed)
				{
					authAllowed = AuthorizeManager.authorizeActionBoolean(swordContext.getAuthenticatorContext(), comms[i], Constants.READ);
				}

				// if we have not already determined that the obo user is ok to submit, look up the READ policy on the
				// community.  THis will include determining if the user is an administrator.
				if (!oboAllowed)
				{
					oboAllowed = AuthorizeManager.authorizeActionBoolean(swordContext.getOnBehalfOfContext(), comms[i], Constants.READ);
				}

				// final check to see if we are allowed to READ
				if (authAllowed && oboAllowed)
				{
					allowed.add(comms[i]);
				}
			}
			return allowed;
		}
		catch (SQLException e)
		{
			log.error("Caught exception: ", e);
			throw new DSpaceSWORDException(e);
		}
	}

	/**
	 * Get an array of all the collections that the current SWORD
	 * context will allow deposit onto in the given DSpace context
	 *
	 * The user may submit to a community if the following conditions
	 * are met:
	 *
	 * IF: the authenticated user is an administrator
	 *   AND:
	 *      (the on-behalf-of user is an administrator
	 *	 	OR the on-behalf-of user is authorised to READ
	 *	 	OR the on-behalf-of user is null)
	 * OR IF: the authenticated user is authorised to READ
	 *   AND:
	 *	     (the on-behalf-of user is an administrator
	 *		  OR the on-behalf-of user is authorised to READ
	 *		  OR the on-behalf-of user is null)
	 *
	 * @param community
	 * @return	the array of allowed collections
	 * @throws DSpaceSWORDException
	 */
	public List<Community> getCommunities(SWORDContext swordContext, Community community)
		throws DSpaceSWORDException
	{
		// a community is allowed if the following conditions are met
		//
		// - the authenticated user is an administrator
		// -- the on-behalf-of user is an administrator
		// -- the on-behalf-of user is authorised to READ
		// -- the on-behalf-of user is null
		// - the authenticated user is authorised to READ
		// -- the on-behalf-of user is an administrator
		// -- the on-behalf-of user is authorised to READ
		// -- the on-behalf-of user is null
		try
		{
			Community[] comms = community.getSubcommunities();
			List<Community> allowed = new ArrayList<Community>();

			for (int i = 0; i < comms.length; i++)
			{
				boolean authAllowed = false;
				boolean oboAllowed = false;

				// check for obo null
				if (swordContext.getOnBehalfOf() == null)
				{
					oboAllowed = true;
				}

				// look up the READ policy on the community.  This will include determining if the user is an administrator
				// so we do not need to check that separately
				if (!authAllowed)
				{
					authAllowed = AuthorizeManager.authorizeActionBoolean(swordContext.getAuthenticatorContext(), comms[i], Constants.READ);
				}

				// if we have not already determined that the obo user is ok to submit, look up the READ policy on the
				// community.  THis will include determining if the user is an administrator.
				if (!oboAllowed)
				{
					oboAllowed = AuthorizeManager.authorizeActionBoolean(swordContext.getOnBehalfOfContext(), comms[i], Constants.READ);
				}

				// final check to see if we are allowed to READ
				if (authAllowed && oboAllowed)
				{
					allowed.add(comms[i]);
				}
			}
			return allowed;

		}
		catch (SQLException e)
		{
			log.error("Caught exception: ", e);
			throw new DSpaceSWORDException(e);
		}
	}

	/**
	 * Get an array of all the collections that the current SWORD
	 * context will allow deposit onto in the given DSpace context
	 *
	 * Forwards to:
	 *
	 * getAllowedCollections(swordContext, null)
	 *
	 * See that method for details of the conditions applied
	 *
	 * @param swordContext
	 * @return	the array of allowed collections
	 * @throws DSpaceSWORDException
	 */
	public List<org.dspace.content.Collection> getAllowedCollections(SWORDContext swordContext)
		throws DSpaceSWORDException
	{
		return this.getAllowedCollections(swordContext, null);
	}

	/**
	 * Get an array of all the collections that the current SWORD
	 * context will allow deposit onto in the given DSpace context
	 *
	 * IF: the authenticated user is an administrator
	 *   AND:
	 *      (the on-behalf-of user is an administrator
	 *	 	OR the on-behalf-of user is authorised to ADD
	 *	 	OR the on-behalf-of user is null)
	 * OR IF: the authenticated user is authorised to ADD
	 *   AND:
	 *	     (the on-behalf-of user is an administrator
	 *		  OR the on-behalf-of user is authorised to ADD
	 *		  OR the on-behalf-of user is null)
	 *
	 * @param swordContext
	 * @return	the array of allowed collections
	 * @throws DSpaceSWORDException
	 */
	public List<org.dspace.content.Collection> getAllowedCollections(SWORDContext swordContext, Community community)
		throws DSpaceSWORDException
	{
		// a collection is allowed if the following conditions are met
		//
		// - the authenticated user is an administrator
		// -- the on-behalf-of user is an administrator
		// -- the on-behalf-of user is authorised to ADD
		// -- the on-behalf-of user is null
		// - the authenticated user is authorised to ADD
		// -- the on-behalf-of user is an administrator
		// -- the on-behalf-of user is authorised to ADD
		// -- the on-behalf-of user is null

		try
		{
			// get the context of the authenticated user
			Context authContext = swordContext.getAuthenticatorContext();

			// short cut by obtaining the collections to which the authenticated user can submit
			org.dspace.content.Collection[] cols = org.dspace.content.Collection.findAuthorized(authContext, community, Constants.ADD);
			List<org.dspace.content.Collection> allowed = new ArrayList<org.dspace.content.Collection>();

			// now find out if the obo user is allowed to submit to any of these collections
			for (int i = 0; i < cols.length; i++)
			{
				boolean oboAllowed = false;

				// check for obo null
				if (swordContext.getOnBehalfOf() == null)
				{
					oboAllowed = true;
				}

				// if we have not already determined that the obo user is ok to submit, look up the READ policy on the
				// community.  THis will include determining if the user is an administrator.
				if (!oboAllowed)
				{
					oboAllowed = AuthorizeManager.authorizeActionBoolean(swordContext.getOnBehalfOfContext(), cols[i], Constants.ADD);
				}

				// final check to see if we are allowed to READ
				if (oboAllowed)
				{
					allowed.add(cols[i]);
				}
			}
			return allowed;

		}
		catch (SQLException e)
		{
			log.error("Caught exception: ", e);
			throw new DSpaceSWORDException(e);
		}
	}

	/**
	 * Get a list of all the items that the current SWORD
	 * context will allow deposit onto in the given DSpace context
	 *
	 * IF: the authenticated user is an administrator
	 *   AND:
	 *      (the on-behalf-of user is an administrator
	 *	 	OR the on-behalf-of user is authorised to WRITE on the item and ADD on the ORIGINAL bundle
	 *	 	OR the on-behalf-of user is null)
	 * OR IF: the authenticated user is authorised to WRITE on the item and ADD on the ORIGINAL bundle
	 *   AND:
	 *	     (the on-behalf-of user is an administrator
	 *		  OR the on-behalf-of user is authorised to WRITE on the item and ADD on the ORIGINAL bundle
	 *		  OR the on-behalf-of user is null)
	 *
	 * @param swordContext
	 * @return	the array of allowed collections
	 * @throws DSpaceSWORDException
	 */
	public List<Item> getAllowedItems(SWORDContext swordContext, org.dspace.content.Collection collection)
			throws DSpaceSWORDException
	{
		// an item is allowed if the following conditions are met
		//
		// - the authenticated user is an administrator
		// -- the on-behalf-of user is an administrator
		// -- the on-behalf-of user is authorised to WRITE on the item and ADD on the ORIGINAL bundle
		// -- the on-behalf-of user is null
		// - the authenticated user is authorised to WRITE on the item and ADD on the ORIGINAL bundle
		// -- the on-behalf-of user is an administrator
		// -- the on-behalf-of user is authorised to WRITE on the item and ADD on the ORIGINAL bundle
		// -- the on-behalf-of user is null

		try
		{
			List<Item> allowed = new ArrayList<Item>();
			ItemIterator ii = collection.getItems();

			while (ii.hasNext())
			{
				Item item = ii.next();

				boolean authAllowed = false;
				boolean oboAllowed = false;

				// check for obo null
				if (swordContext.getOnBehalfOf() == null)
				{
					oboAllowed = true;
				}

				// get the "ORIGINAL" bundle(s)
				Bundle[] bundles = item.getBundles("ORIGINAL");

				// look up the READ policy on the community.  This will include determining if the user is an administrator
				// so we do not need to check that separately
				if (!authAllowed)
				{
					boolean write = AuthorizeManager.authorizeActionBoolean(swordContext.getAuthenticatorContext(), item, Constants.WRITE);
					
					boolean add = false;
					for (int i = 0; i < bundles.length; i++)
					{
						add = AuthorizeManager.authorizeActionBoolean(swordContext.getAuthenticatorContext(), bundles[i], Constants.ADD);
						if (!add)
						{
							break;
						}
					}

					authAllowed = write && add;
				}

				// if we have not already determined that the obo user is ok to submit, look up the READ policy on the
				// community.  THis will include determining if the user is an administrator.
				if (!oboAllowed)
				{
					boolean write = AuthorizeManager.authorizeActionBoolean(swordContext.getOnBehalfOfContext(), item, Constants.WRITE);

					boolean add = false;
					for (int i = 0; i < bundles.length; i++)
					{
						add = AuthorizeManager.authorizeActionBoolean(swordContext.getOnBehalfOfContext(), bundles[i], Constants.ADD);
						if (!add)
						{
							break;
						}
					}

					oboAllowed = write && add;
				}

				// final check to see if we are allowed to READ
				if (authAllowed && oboAllowed)
				{
					allowed.add(item);
				}
			}

			return allowed;
		}
		catch (SQLException e)
		{
			throw new DSpaceSWORDException(e);
		}
	}

	/**
	 * Can the current SWORD Context permit deposit into the given
	 * collection in the given DSpace Context?
	 *
	 * IF: the authenticated user is an administrator
	 *   AND:
	 *      (the on-behalf-of user is an administrator
	 *	 	OR the on-behalf-of user is authorised to ADD
	 *	 	OR the on-behalf-of user is null)
	 * OR IF: the authenticated user is authorised to ADD
	 *   AND:
	 *	     (the on-behalf-of user is an administrator
	 *		  OR the on-behalf-of user is authorised to ADD
	 *		  OR the on-behalf-of user is null)
	 *
	 * @param swordContext
	 * @param collection
	 * @throws DSpaceSWORDException
	 */
	public boolean canSubmitTo(SWORDContext swordContext, org.dspace.content.Collection collection)
		throws DSpaceSWORDException
	{
		// a user can submit to a collection in the following conditions:
		//
		// - the authenticated user is an administrator
		// -- the on-behalf-of user is an administrator
		// -- the on-behalf-of user is authorised to ADD/in the submission group
		// -- the on-behalf-of user is null
		// - the authenticated user is authorised to ADD/in the submission group
		// -- the on-behalf-of user is an administrator
		// -- the on-behalf-of user is authorised to ADD/in the submission group
		// -- the on-behalf-of user is null

		try
		{
			boolean authAllowed = false;
			boolean oboAllowed = false;

			// check for obo null
			if (swordContext.getOnBehalfOf() == null)
			{
				oboAllowed = true;
			}

			// look up the READ policy on the collection.  This will include determining if the user is an administrator
			// so we do not need to check that separately
			if (!authAllowed)
			{
				authAllowed = AuthorizeManager.authorizeActionBoolean(swordContext.getAuthenticatorContext(), collection, Constants.ADD);
			}

			// if we have not already determined that the obo user is ok to submit, look up the READ policy on the
			// community.  THis will include determining if the user is an administrator.
			if (!oboAllowed)
			{
				oboAllowed = AuthorizeManager.authorizeActionBoolean(swordContext.getOnBehalfOfContext(), collection, Constants.ADD);
			}

			// final check to see if we are allowed to READ
			return (authAllowed && oboAllowed);

		}
		catch (SQLException e)
		{
			log.error("Caught exception: ", e);
			throw new DSpaceSWORDException(e);
		}
	}

	/**
	 * Does the given context have the authority to submit to the given item.
	 *
	 * The context has permission of the following conditions are met:
	 *
	 * IF: the authenticated user is an administrator
	 *   AND:
	 *      (the on-behalf-of user is an administrator
	 *	 	OR the on-behalf-of user is authorised to WRITE on the item and ADD on the ORIGINAL bundle
	 *	 	OR the on-behalf-of user is null)
	 * OR IF: the authenticated user is authorised to WRITE on the item and ADD on the ORIGINAL bundle
	 *   AND:
	 *	     (the on-behalf-of user is an administrator
	 *		  OR the on-behalf-of user is authorised to WRITE on the item and ADD on the ORIGINAL bundle
	 *		  OR the on-behalf-of user is null)
	 *
	 * @param swordContext
	 * @return	the array of allowed collections
	 * @throws DSpaceSWORDException
	 */
	public boolean canSubmitTo(SWORDContext swordContext, Item item)
			throws DSpaceSWORDException
	{
		// a user can submit to a collection in the following conditions:
		//
		// - the authenticated user is an administrator
		// -- the on-behalf-of user is an administrator
		// -- the on-behalf-of user is authorised to WRITE on the item and ADD on the ORIGINAL bundle
		// -- the on-behalf-of user is null
		// - the authenticated user is authorised to WRITE on the item and ADD on the ORIGINAL bundle
		// -- the on-behalf-of user is an administrator
		// -- the on-behalf-of user is authorised to WRITE on the item and ADD on the ORIGINAL bundle
		// -- the on-behalf-of user is null

		try
		{
			boolean authAllowed = false;
			boolean oboAllowed = false;

			// check for obo null
			if (swordContext.getOnBehalfOf() == null)
			{
				oboAllowed = true;
			}

			// get the "ORIGINAL" bundle(s)
			Bundle[] bundles = item.getBundles("ORIGINAL");

			// look up the READ policy on the community.  This will include determining if the user is an administrator
			// so we do not need to check that separately
			if (!authAllowed)
			{
				boolean write = AuthorizeManager.authorizeActionBoolean(swordContext.getAuthenticatorContext(), item, Constants.WRITE);

				boolean add = false;
				for (int i = 0; i < bundles.length; i++)
				{
					add = AuthorizeManager.authorizeActionBoolean(swordContext.getAuthenticatorContext(), bundles[i], Constants.ADD);
					if (!add)
					{
						break;
					}
				}

				authAllowed = write && add;
			}

			// if we have not already determined that the obo user is ok to submit, look up the READ policy on the
			// community.  THis will include determining if the user is an administrator.
			if (!oboAllowed)
			{
				boolean write = AuthorizeManager.authorizeActionBoolean(swordContext.getOnBehalfOfContext(), item, Constants.WRITE);

				boolean add = false;
				for (int i = 0; i < bundles.length; i++)
				{
					add = AuthorizeManager.authorizeActionBoolean(swordContext.getOnBehalfOfContext(), bundles[i], Constants.ADD);
					if (!add)
					{
						break;
					}
				}

				oboAllowed = write && add;
			}

			// final check to see if we are allowed to READ
			return (authAllowed && oboAllowed);

		}
		catch (SQLException e)
		{
			log.error("Caught exception: ", e);
			throw new DSpaceSWORDException(e);
		}
	}

	/**
	 * Can the given context submit to the specified dspace object.
	 *
	 * This forwards to the individual methods for different object types; see
	 * their documentation for details of the conditions.
	 *
	 * @param context
	 * @param dso
	 * @throws DSpaceSWORDException
	 */
	public boolean canSubmitTo(SWORDContext context, DSpaceObject dso)
			throws DSpaceSWORDException
	{
		if (dso instanceof org.dspace.content.Collection)
		{
			return this.canSubmitTo(context, (org.dspace.content.Collection) dso);
		}
		else if (dso instanceof Item)
		{
			return this.canSubmitTo(context, (Item) dso);
		}
		else
		{
			return false;
		}
	}
}
