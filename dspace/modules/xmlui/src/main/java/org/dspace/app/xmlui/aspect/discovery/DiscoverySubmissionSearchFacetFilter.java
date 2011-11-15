package org.dspace.app.xmlui.aspect.discovery;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 14-nov-2011
 * Time: 15:02:10
 */
public class DiscoverySubmissionSearchFacetFilter extends SearchFacetFilter{

    public String getView(){
        return "discoverySubmissions";
    }

    public String getSearchFilterUrl(){
        return "discovery-submission-search-filter";
    }

    public String getDiscoverUrl(){
        return "submissions";
    }

    protected String[] getSolrFilterQueries() {
        try {
            java.util.List<String> allFilterQueries = new ArrayList<String>();
            Request request = ObjectModelHelper.getRequest(objectModel);
            java.util.List<String> fqs = new ArrayList<String>();

            if(request.getParameterValues("fq") != null)
            {
                fqs.addAll(Arrays.asList(request.getParameterValues("fq")));
            }

            String type = request.getParameter("filtertype");
            String value = request.getParameter("filter");

            if(value != null && !value.equals("")){
                String exactFq = (type.equals("*") ? "" : type + ":") + value;
                fqs.add(exactFq + " OR " + exactFq + "*");
            }


            for (String fq : fqs) {
                //Do not put a wildcard after a range query
                if (fq.matches(".*\\:\\[.* TO .*\\](?![a-z 0-9]).*")) {
                    allFilterQueries.add(fq);
                }
                else
                {
                    allFilterQueries.add(fq.endsWith("*") ? fq : fq + " OR " + fq + "*");
                }
            }

            //Check if our current user is an admin, if so we need to show only workflow tasks assigned to him
            Context context = ContextUtil.obtainContext(objectModel);
            if(AuthorizeManager.isAdmin(context)){
                StringBuffer adminQuery = new StringBuffer();
                EPerson currentUser = context.getCurrentUser();
                adminQuery.append("WorkflowEpersonId:").append(currentUser.getID());
                //Retrieve all the groups this user is a part of
                Set<Integer> groupIdentifiers = Group.allMemberGroupIDs(this.context, currentUser);
                for(int groupId : groupIdentifiers){
                    adminQuery.append(" OR WorkflowGroupId:").append(groupId);
                }
                adminQuery.append(" OR (SubmitterName_filter:").append(context.getCurrentUser().getName())
                          .append(" AND DSpaceStatus:Submission)");
                allFilterQueries.add(adminQuery.toString());
            }

            return allFilterQueries.toArray(new String[allFilterQueries.size()]);
        }
        catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            return null;
        }
    }
}
