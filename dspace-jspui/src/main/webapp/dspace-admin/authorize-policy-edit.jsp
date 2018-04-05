<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

<%@page import="org.apache.commons.lang.time.DateFormatUtils"%>
<%--
  - policy editor - for new or existing policies
  -
  - Attributes:
  -   policy - a ResourcePolicy to be edited
  -   groups - Group [] of groups to choose from
  -   epeople - EPerson [] of epeople to choose from (unused in first version)
  -   edit_title - title of the page ("Collection 13", etc.
  -   id_name - name of string to put in hidden arg (collection_id, etc.) 
  -   id - ID of the object policy relates to (collection.getID(), etc.)
  -   newpolicy - set to some string value if this is a new policy
  - Returns:
  -   save_policy   - user wants to save a policy
  -   cancel_policy - user wants to cancel, and return to policy list
  -   "id_name"     - name/value passed in from id_name/id above
  -   group_id      - set if user selected a group
  -   eperson_id    - set if user selected an eperson
  -   start_date    - start date of a policy (e.g. for embargo feature)
  -   end_date      - end date of a policy
  -   action_id     - set to whatever user chose
  -   (new policy)  - set to a the string passed in above if policy is a new one
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.authorize.ResourcePolicy" %>
<%@ page import="org.dspace.content.Collection"       %>
<%@ page import="org.dspace.core.Constants"           %>
<%@ page import="org.dspace.eperson.EPerson"          %>
<%@ page import="org.dspace.eperson.Group"            %>
<%@ page import="java.util.List" %>


<%
    ResourcePolicy policy = (ResourcePolicy) request.getAttribute("policy"    );
    List<Group>   groups     = (List<Group>) request.getAttribute("groups"    );
    List<EPerson> epeople    = (List<EPerson>) request.getAttribute("epeople"   );
    String edit_title     = (String        ) request.getAttribute("edit_title");
    String id_name        = (String        ) request.getAttribute("id_name"   );
    String id             = (String        ) request.getAttribute("id"        );
    String newpolicy      = (String        ) request.getAttribute("newpolicy" );
    
    // calculate the resource type and its relevance ID
    // to check what actions to present
    int resourceType      = policy.getdSpaceObject().getType();
    int resourceRelevance = 1 << resourceType;
    
    request.setAttribute("LanguageSwitch", "hide");  
    
   // Is the logged in user an admin or community admin or collection admin
    Boolean admin = (Boolean)request.getAttribute("is.admin");
    boolean isAdmin = (admin == null ? false : admin.booleanValue());
    
    Boolean communityAdmin = (Boolean)request.getAttribute("is.communityAdmin");
    boolean isCommunityAdmin = (communityAdmin == null ? false : communityAdmin.booleanValue());
    
    Boolean collectionAdmin = (Boolean)request.getAttribute("is.collectionAdmin");
    boolean isCollectionAdmin = (collectionAdmin == null ? false : collectionAdmin.booleanValue());
    
    String naviAdmin = "admin";
    String link = "/dspace-admin";
    
    if(!isAdmin && (isCommunityAdmin || isCollectionAdmin))
    {
        naviAdmin = "community-or-collection-admin";
        link = "/tools";
    }
%>

<dspace:layout style="submission" titlekey="jsp.dspace-admin.authorize-policy-edit.title"
               navbar="<%= naviAdmin %>"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="<%= link %>"
               nocache="true">

        <%-- <h1>Edit Policy for <%= edit_title %>:</h1> --%>
        <h1><fmt:message key="jsp.dspace-admin.authorize-policy-edit.heading">
            <fmt:param><%= edit_title %></fmt:param>
        </fmt:message>
        <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#authorize\"%>"><fmt:message key="jsp.help"/></dspace:popup>
        </h1>
            
    <form action="<%= request.getContextPath() %>/tools/authorize" method="post">

            <div class="input-group">
                <span class="col-md-2">
                    <%-- <td>Group:</td> --%>
                    <label for="tgroup_id"><fmt:message key="jsp.dspace-admin.general.group-colon"/></label>
            	</span>
            	<span class="col-md-10">
                <select class="form-control" size="15" name="group_id" id="tgroup_id">
                    <%  for(int i = 0; i < groups.size(); i++ ) { %>
                            <option value="<%= groups.get(i).getID() %>" <%= (groups.get(i).equals((policy.getGroup()))  ? "selected=\"selected\"" : "" ) %> >
                            <%= groups.get(i).getName()%>
                            </option>
                        <%  } %>
                </select>
                </span>
           
        <%-- <tr><td>Action:</td> --%>
        	<span class="col-md-2">
        		<label for="taction_id"><fmt:message key="jsp.dspace-admin.general.action-colon"/></label>
        	</span>
        	<span class="col-md-10">
                    <input type="hidden" name="<%=id_name%>" value="<%=id%>" />
                    <input type="hidden" name="policy_id" value="<%=policy.getID()%>" />
                    <select class="form-control" name="action_id" id="taction_id">
                        <%  for( int i = 0; i < Constants.actionText.length; i++ )
                                {
                                    // only display if action i is relevant
                                    //  to resource type resourceRelevance                             
                                    if( (Constants.actionTypeRelevance[i]&resourceRelevance) > 0)
                                    { %>
                                        <option value="<%= i %>"
                                        <%=(policy.getAction() == i ? "selected=\"selected\"" : "")%>>
                                        <%= Constants.actionText[i]%>
                                        </option>
                        <%          }
                                } %>
                    </select>
                </span>
                    <%
                    // start and end dates are used for Items and Bitstreams only.
                    if (resourceType == Constants.ITEM || resourceType == Constants.BITSTREAM)
                    {
                    %>
                        <!-- policy start date -->
                        <span class="col-md-2">
                            <label for="t_start_date_id"><fmt:message key="jsp.dspace-admin.general.policy-start-date-colon"/></label>
                        </span>
                        <span class="col-md-10">
                            <input class="form-control" name="policy_start_date" maxlength="10" size="10" type="text" 
                                   value="<%= policy.getStartDate() != null ? DateFormatUtils.format(policy.getStartDate(), "yyyy-MM-dd") : "" %>" />
                        </span>
                        <!-- policy end date -->
                        <span class="col-md-2">
                            <label for="t_end_date_id"><fmt:message key="jsp.dspace-admin.general.policy-end-date-colon"/></label>
                        </span>
                        <span class="col-md-10">
                            <input class="form-control" name="policy_end_date" maxlength="10" size="10" type="text" 
                                   value="<%= policy.getEndDate() != null ? DateFormatUtils.format(policy.getEndDate(), "yyyy-MM-dd") : "" %>" />
                        </span>
                    <%} // if Item||Bitstream%>
            </div>
    <% if( newpolicy != null ) { %> <input name="newpolicy" type="hidden" value="<%=newpolicy%>"/> <% } %>
    
				<div class="btn-group pull-right col-md-2">
                    <%-- <input type="submit" name="submit_save_policy" value="Save Policy"> --%>
                    <input class="btn btn-primary" type="submit" name="submit_save_policy" value="<fmt:message key="jsp.dspace-admin.general.save"/>" />

                    <%-- <input type="submit" name="submit_cancel_policy" value="Cancel"> --%>
                    <input class="btn btn-default" type="submit" name="submit_cancel_policy" value="<fmt:message key="jsp.dspace-admin.general.cancel"/>" />
				</div>
    </form>
</dspace:layout>
