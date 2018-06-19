/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.discovery.recentSubmissions;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.dspace.app.util.Util;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.SiteService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.discovery.DiscoverResult;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * Transformer that renders recent submissions with a paging option to traverse them
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class RecentSubmissionTransformer extends AbstractDSpaceTransformer {

    protected static final Message T_dspace_home = message("xmlui.general.dspace_home");
    protected static final Message T_untitled = message("xmlui.general.untitled");
    protected static final Message T_head = message("xmlui.Discovery.RecentSubmissions.RecentSubmissionTransformer.head");
    protected static final Message T_recent_submission_head = message("xmlui.Discovery.RecentSubmissions.RecentSubmissionTransformer.recent.head");
    protected static final Message T_trail = message("xmlui.Discovery.RecentSubmissions.RecentSubmissionTransformer.trail");

    protected boolean isHomePage = false;

    protected SiteService siteService = ContentServiceFactory.getInstance().getSiteService();

    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters) throws ProcessingException, SAXException, IOException {
        super.setup(resolver, objectModel, src, parameters);
        isHomePage = parameters.getParameterAsBoolean("isHomePage", false);
    }

    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        DSpaceObject dso = getDSpaceObject();

        // Set up the major variables
        // Set the page title
        String name = dso.getName();
        Metadata titlePageMeta = pageMeta.addMetadata("title");
        if (name == null || name.length() == 0)
        {
            if(isHomePage){
                titlePageMeta.addContent(T_untitled);
            }else{
                titlePageMeta.addContent(T_recent_submission_head.parameterize(name));
            }
        }
        else
        {
            if(isHomePage){
                titlePageMeta.addContent(name);
            }else{
                titlePageMeta.addContent(T_recent_submission_head.parameterize(name));
            }
        }

        // Add the trail back to the repository root.
        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        HandleUtil.buildHandleTrail(context, dso, pageMeta,contextPath, !isHomePage);
        if(!isHomePage)
        {
            //Add a trail link indicating that we are on a recent submissions page
            pageMeta.addTrail().addContent(T_trail);
        }

        /**
         * If we are on a home page add the feeds (if enabled)
         */
        if(isHomePage)
        {
            // Add RSS links if available
            String[] formats = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("webui.feed.formats");
            if ( formats != null )
            {
                for (String format : formats)
                {
                    // Remove the protocol number, i.e. just list 'rss' or' atom'
                    String[] parts = format.split("_");
                    if (parts.length < 1)
                    {
                        continue;
                    }

                    String feedFormat = parts[0].trim()+"+xml";

                    String feedURL = contextPath+"/feed/"+format.trim()+"/"+dso.getHandle();
                    pageMeta.addMetadata("feed", feedFormat).addContent(feedURL);
                }
            }
        }
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException, ProcessingException {
        Request request = ObjectModelHelper.getRequest(objectModel);
        DSpaceObject dso = getDSpaceObject();

        Division mainDivision = body.addDivision("main-recent-submissions");
        setMainTitle(dso, mainDivision);
        Division recentSubmissionDivision = mainDivision.addDivision("recent-submissions");
        if(isHomePage){
            recentSubmissionDivision.setHead(T_recent_submission_head);
        }

        DiscoverResult recentlySubmittedItems = RecentSubmissionUtils.getRecentlySubmittedItems(context, dso, getOffset(request));
        setPagination(request, dso, recentSubmissionDivision, recentlySubmittedItems);

        ReferenceSet lastSubmitted = recentSubmissionDivision.addReferenceSet(
                "last-submitted", ReferenceSet.TYPE_SUMMARY_LIST,
                null, "recent-submissions");

        for (DSpaceObject resultObject : recentlySubmittedItems.getDspaceObjects()) {
            if(resultObject != null){
                lastSubmitted.addReference(resultObject);
            }
        }
    }

    protected void setPagination(Request request, DSpaceObject dso, Division mainDivision, DiscoverResult recentlySubmittedItems) throws SQLException {
        int offset = getOffset(request);
        int rpp = RecentSubmissionUtils.getRecentSubmissionConfiguration(dso).getMax();
        int firstIndex = offset + 1;
        int lastIndex = offset + recentlySubmittedItems.getDspaceObjects().size();
        mainDivision.setSimplePagination((int) recentlySubmittedItems.getTotalSearchResults(), firstIndex,
                lastIndex, getPreviousPageURL(dso, offset, rpp), getNextPageURL(dso, offset, rpp, (int) recentlySubmittedItems.getTotalSearchResults()));
    }

    protected void setMainTitle(DSpaceObject dso, Division mainDivision) throws WingException {
        String title = dso.getName();
        if(isHomePage)
        {
            mainDivision.setHead(title);
        }else{
            //We are not acting as home page so use a message
            mainDivision.setHead(T_head.parameterize(title));
        }

    }

    protected String getNextPageURL(DSpaceObject dso, int currentOffset, int rpp, int total) throws SQLException {
        if((rpp + currentOffset) < total)
        {
            return getBaseUrl(dso) + "?offset=" + (rpp + currentOffset);
        }else{
            return null;
        }
    }

    protected String getPreviousPageURL(DSpaceObject dso, int currentOffset, int rpp) throws SQLException {
        if((currentOffset - rpp) < 0){
            return null;
        }else{
            return getBaseUrl(dso) + "?offset=" + (currentOffset - rpp);
        }
    }

    protected String getBaseUrl(DSpaceObject dso) throws SQLException {
        String url = contextPath;
        if(dso != null && !dso.equals(siteService.findSite(context)))
        {
            url += "/handle/" + dso.getHandle();
        }
        if(!isHomePage)
        {
            url += "/recent-submissions";
        }
        return url;
    }

    protected int getOffset(Request request) {
        int start = Util.getIntParameter(request, "offset");
        if(start == -1){
            start = 0;
        }
        return start;
    }

    protected DSpaceObject getDSpaceObject() throws SQLException {
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if(dso == null)
        {
            return siteService.findSite(context);
        }else{
            return dso;
        }
    }
}
