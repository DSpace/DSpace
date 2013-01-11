<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

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
  -   start_date    - not set, unused
  -   end_date      - not set, unused
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


<%
    ResourcePolicy policy = (ResourcePolicy) request.getAttribute("policy"    );
    Group   [] groups     = (Group  []     ) request.getAttribute("groups"    );
    EPerson [] epeople    = (EPerson[]     ) request.getAttribute("epeople"   );
    String edit_title     = (String        ) request.getAttribute("edit_title");
    String id_name        = (String        ) request.getAttribute("id_name"   );
    String id             = (String        ) request.getAttribute("id"        );
    String newpolicy      = (String        ) request.getAttribute("newpolicy" );
    
    // calculate the resource type and its relevance ID
    // to check what actions to present
    int resourceType      = policy.getResourceType();
    int resourceRelevance = 1 << resourceType;
    
    request.setAttribute("LanguageSwitch", "hide");  
%>

<dspace:layout titlekey="jsp.dspace-admin.authorize-policy-edit.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin"
               nocache="true">
  <table width="95%">
    <tr>
      <td align="left">
        <%-- <h1>Edit Policy for <%= edit_title %>:</h1> --%>
        <h1><fmt:message key="jsp.dspace-admin.authorize-policy-edit.heading">
            <fmt:param><%= edit_title %></fmt:param>
        </fmt:message></h1>
      </td>
      <td align="right" class="standard">
        <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#authorize\"%>"><fmt:message key="jsp.help"/></dspace:popup>
      </td>
    </tr>
  </table>
      
    <form action="<%= request.getContextPath() %>/tools/authorize" method="post">

    <table class="miscTable" align="center" summary="Edit Policy Form">
        <tr>     
            <%-- <td>Group:</td> --%>
            <th id="t1"><label for="tgroup_id"><fmt:message key="jsp.dspace-admin.general.group-colon"/></label></th>
            <td headers="t1">
                <select size="15" name="group_id" id="tgroup_id">
                    <%  for(int i = 0; i < groups.length; i++ ) { %>
                            <option value="<%= groups[i].getID() %>" <%= (groups[i].getID() == policy.getGroupID() ? "selected=\"selected\"" : "" ) %> >
                            <%= groups[i].getName()%>
                            </option>
                        <%  } %>
                </select>
            </td>
        </tr>
        <%-- <tr><td>Action:</td> --%>
        <tr>
          <th id="t2"><label for="taction_id"><fmt:message key="jsp.dspace-admin.general.action-colon"/></label></th>
            <td headers="t2">
                <input type="hidden" name="<%=id_name%>" value="<%=id%>" />
                <input type="hidden" name="policy_id" value="<%=policy.getID()%>" />
                <select name="action_id" id="taction_id">
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
            </td>
        </tr>
    </table>

    <% if( newpolicy != null ) { %> <input name="newpolicy" type="hidden" value="<%=newpolicy%>"/> <% } %>
    
    <center>
        <table width="70%">
            <tr>
                <td align="left">
                    <%-- <input type="submit" name="submit_save_policy" value="Save Policy"> --%>
                    <input type="submit" name="submit_save_policy" value="<fmt:message key="jsp.dspace-admin.general.save"/>" />
                </td>
                <td align="right">
                    <%-- <input type="submit" name="submit_cancel_policy" value="Cancel"> --%>
                    <input type="submit" name="submit_cancel_policy" value="<fmt:message key="jsp.dspace-admin.general.cancel"/>" />
                </td>
            </tr>
        </table>
    </center>        

    </form>
</dspace:layout>
