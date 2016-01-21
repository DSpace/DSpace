/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeServiceImpl;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.Statement;
import org.swordapp.server.StatementManager;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordConfiguration;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class StatementManagerDSpace extends DSpaceSwordAPI
        implements StatementManager
{
    private static Logger log = Logger.getLogger(StatementManagerDSpace.class);

    protected AuthorizeService authorizeService = AuthorizeServiceFactory
            .getInstance().getAuthorizeService();

    public Statement getStatement(String stateIRI, Map<String, String> accept,
            AuthCredentials authCredentials, SwordConfiguration swordConfig)
            throws SwordServerException, SwordError, SwordAuthException
    {
        SwordContext sc = null;
        try
        {
            SwordConfigurationDSpace config = (SwordConfigurationDSpace) swordConfig;

            SwordAuthenticator auth = new SwordAuthenticator();
            sc = auth.authenticate(authCredentials);
            Context context = sc.getContext();

            if (log.isDebugEnabled())
            {
                log.debug(LogManager
                        .getHeader(context, "sword_get_statement", ""));
            }

            // log the request
            String un = authCredentials.getUsername() != null ?
                    authCredentials.getUsername() :
                    "NONE";
            String obo = authCredentials.getOnBehalfOf() != null ?
                    authCredentials.getOnBehalfOf() :
                    "NONE";
            log.info(LogManager.getHeader(context, "sword_get_statement",
                    "username=" + un + ",on_behalf_of=" + obo));

            // first thing is to figure out what we're being asked to work on
            SwordUrlManager urlManager = config.getUrlManager(context, config);
            Item item = urlManager.getItem(context, stateIRI);
            if (item == null)
            {
                throw new SwordError(404);
            }

            // find out if we are allowed to read the item's statement
            authorizeService.authorizeAction(context, item, Constants.READ);

            // find out, now we know what we're being asked for, whether this is allowed
            WorkflowManagerFactory.getInstance()
                    .retrieveStatement(context, item);

            String suffix = urlManager.getTypeSuffix(context, stateIRI);
            SwordStatementDisseminator disseminator = null;

            if (suffix != null)
            {
                Map<Float, List<String>> analysed = new HashMap<>();
                List<String> list = new ArrayList<>();
                list.add(suffix);
                analysed.put((float) 1.0, list);
                disseminator = SwordDisseminatorFactory
                        .getStatementInstance(analysed);
            }
            else
            {
                // we rely on the content negotiation to do the work
                String acceptContentType = this
                        .getHeader(accept, "Accept", null);

                // we extract from the Accept header the ordered list of content types
                TreeMap<Float, List<String>> analysed = this
                        .analyseAccept(acceptContentType);

                // the meat of this is done by the package disseminator
                disseminator = SwordDisseminatorFactory
                        .getStatementInstance(analysed);
            }

            return disseminator.disseminate(context, item);
        }
        catch (AuthorizeException e)
        {
            throw new SwordAuthException();
        }
        catch (SQLException | DSpaceSwordException e)
        {
            throw new SwordServerException(e);
        }
        finally
        {
            if (sc != null)
            {
                sc.abort();
            }
        }
    }
}
