/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.cris.orcid;

import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.orcid.OrcidPreferencesUtils;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.authenticate.StandaloneMethod;
import org.dspace.authority.orcid.OrcidService;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.EPerson;

public class OrcidStandaloneAuthenticationMethod implements StandaloneMethod
{

    /** log4j category */
    private static Logger log = Logger
            .getLogger(OrcidStandaloneAuthenticationMethod.class);

    private ApplicationService applicationService;
    private CrisSearchService searchService;
    
    @Override
    public int connect(Context context, String username, String password,
            String realm, HttpServletRequest request)
            throws SQLException, SearchServiceException
    {
        String email = null;
        EPerson currentUser = context.getCurrentUser();
        if (request == null)
        {
            return AuthenticationMethod.BAD_ARGS;
        }
        String orcid = (String) request.getAttribute("orcid");

        try
        {
            // TEMPORARILY turn off authorisation
            context.turnOffAuthorisationSystem();

            if (orcid == null)
            {
                return AuthenticationMethod.BAD_ARGS;
            }
            String token = (String) request.getAttribute("access_token");
            String scope = (String) request.getAttribute("scope");
            EPerson[] epersons = EPerson.search(context, orcid);

            if (epersons != null)
            {
                if (epersons.length != 0)
                {
                    if (epersons.length > 1)
                    {
                        log.warn("Fail to authorize user with orcid: " + orcid
                                + " email:" + email
                                + " - Multiple Users found");
                        return AuthenticationMethod.MULTIPLE_USERS;
                    }
                    else
                    {
                        EPerson eperson = epersons[0];
                        if (eperson.getID() != currentUser.getID())
                        {
                            log.warn("Fail to authorize user with orcid: "
                                    + orcid + " email:" + email
                                    + " - Another User found with same orcid - EPERSON ID:"
                                    + eperson.getID());
                            request.setAttribute("orcid.standalone.error",
                                    eperson.getID());
                            return AuthenticationMethod.NO_SUCH_USER;
                        }
                    }
                }
            }

            Metadatum[] md = currentUser.getMetadata("eperson", "orcid", null,
                    null);
            boolean found = false;
            if (md != null && md.length > 0)
            {
                for (Metadatum m : md)
                {
                    if (StringUtils.equals(m.value, orcid))
                    {
                        found = true;
                        break;
                    }
                }
            }

            SolrQuery query = new SolrQuery();
            query.setQuery("search.resourcetype:" + CrisConstants.RP_TYPE_ID);
            String filterQuery = "cris-sourceref:" + orcid;
            if(StringUtils.isNotBlank(orcid)){
                filterQuery += (StringUtils.isNotBlank(filterQuery)?" OR ":"")+"crisrp.orcid:\""+orcid+"\" OR crisrp.orcid_private:\""+orcid+"\"";
            }            
            query.addFilterQuery(filterQuery);
            QueryResponse qResp = getSearchService().search(query);
            SolrDocumentList docList = qResp.getResults();
            if (docList.size() >= 2)
            {
                log.warn("Fail to authorize user with orcid: " + orcid
                        + " email:" + email
                        + " - Multiple Researcher Page found");
                return AuthenticationMethod.MULTIPLE_USERS;
            }
            else if (docList.size() == 1)
            {
                SolrDocument doc = docList.get(0);
                String rpKey = (String) doc
                        .getFirstValue("objectpeople_authority");
                ResearcherPage rp = getApplicationService()
                        .getResearcherByAuthorityKey(rpKey);
                if (rp != null)
                {
                    boolean isAlreadyAssociated = false;
                    if (StringUtils.isNotBlank(context.getCrisID()))
                    {
                        if (context.getCrisID().equals(rp.getCrisID()))
                        {
                            isAlreadyAssociated = true;
                        }
                    }
                    if (rp.getEpersonID() != null)
                    {
                        if (rp.getEpersonID() != currentUser.getID())
                        {
                            log.warn("Fail to authorize user with orcid: "
                                    + orcid + " email:" + email
                                    + " - Another User with a Researcher Page found with same orcid - EPERSON ID:"
                                    + rp.getEpersonID());
                            request.setAttribute("orcid.standalone.error",
                                    rp.getEpersonID());
                            return AuthenticationMethod.MULTIPLE_PROFILE;
                        }
                    }
                    else
                    {
                        if (!isAlreadyAssociated)
                        {
                            log.warn("Fail to authorize user with orcid: "
                                    + orcid + " email:" + email
                                    + " - Researcher Page found with same orcid - CRIS ID:"
                                    + rp.getCrisID());
                            request.setAttribute("orcid.standalone.error",
                                    rp.getCrisID());
                            return AuthenticationMethod.NO_SUCH_PROFILE;
                        }
                    }
                }
            }

            if (!found)
            {
                currentUser.addMetadata("eperson", "orcid", null, null, orcid);
            }
            currentUser.addMetadata("eperson", "orcid", "accesstoken", null,
                    token);

            ResearcherPage rp = getApplicationService()
                    .getResearcherByAuthorityKey(context.getCrisID());

            // try to use the access token to read-limited
            boolean orcidPopulated = OrcidPreferencesUtils.populateRP(rp, orcid,
                    token);
            if (!orcidPopulated && token != null)
            {
                // ok, it was not allowed for read-limited, go for public info
                orcidPopulated = OrcidPreferencesUtils.populateRP(rp, orcid);
                ResearcherPageUtils.buildTextValue(rp, token,
                        OrcidService.SYSTEM_ORCID_TOKEN_ACCESS);
            }
            else if (token != null)
            {
                // there are good chances that the access token is valid for the
                // other
                // configured scopes (at least it was valid for read-limited...)
                OrcidPreferencesUtils.setTokens(rp, token);
            }

            if (orcidPopulated)
            {
                rp.setSourceRef("orcid");
                rp.setSourceID(orcid);
            }
            if (rp.getEpersonID() == null)
            {
                rp.setEpersonID(currentUser.getID());
            }

            getApplicationService().saveOrUpdate(ResearcherPage.class, rp);

            currentUser.update();
            context.commit();
        }
        catch (Exception e)
        {
            log.warn("Fail to authorize user with orcid: " + orcid + " email:"
                    + email, e);
            return AuthenticationMethod.GENERIC_ERROR;
        }
        finally
        {
            context.restoreAuthSystemState();
        }
        return AuthenticationMethod.SUCCESS;
    }

    public ApplicationService getApplicationService()
    {
        return applicationService;
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

    public CrisSearchService getSearchService()
    {
        return searchService;
    }

    public void setSearchService(CrisSearchService searchService)
    {
        this.searchService = searchService;
    }

}
