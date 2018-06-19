<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

<%--
  - Advanced policy tool - a bit dangerous, but powerful
  -
  - Attributes:
  -  collections - Collection [] all DSpace collections
  -  groups      - Group      [] all DSpace groups for select box
  - Returns:
  -  submit_advanced_clear - erase all policies for set of objects
  -  submit_advanced_add   - add a policy to a set of objects
  -  collection_id         - ID of collection containing objects
  -  resource_type         - type, "bitstream" or "item"
  -  group_id              - group that policy relates to
  -  action_id             - action that policy allows
  -
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>


<%@ page import="java.util.List"     %>
<%@ page import="java.util.Iterator" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.content.Collection"       %>
<%@ page import="org.dspace.core.Constants"           %>
<%@ page import="org.dspace.eperson.Group"            %>

<%
    List<Group>      groups     = (List<Group>)      request.getAttribute("groups"     );
    List<Collection> collections= (List<Collection>) request.getAttribute("collections");
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

<dspace:layout style="submission" titlekey="jsp.dspace-admin.authorize-advanced.advanced"
               navbar="<%= naviAdmin %>"
               locbar="link"
               parentlink="<%= link %>"
               parenttitlekey="jsp.administer">

<h1><fmt:message key="jsp.dspace-admin.authorize-advanced.advanced"/>
<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") +\"#advancedpolicies\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup>
</h1>

	<%-- <p>Allows you to do wildcard additions to and clearing
       of policies for types of content contained in a collection.
       Warning, dangerous - removing READ permissions from
       items will make them not viewable!  <dspace:popup page="/help/site-admin.html#advancedpolicies">More help...</dspace:popup></p> --%>
	<div class="alert alert-info"><fmt:message key="jsp.dspace-admin.authorize-advanced.text"/></div>

    <form method="post" action="">
         
            <%-- <td>Collection:</td> --%>
		<div class="input-group">
            <span class="col-md-2"><label for="tcollection"><fmt:message key="jsp.dspace-admin.authorize-advanced.col"/></label></span>
            <span class="col-md-10">
                <select class="form-control" size="10" name="collection_id" id="tcollection">
                    <%  for(int i = 0; i < collections.size(); i++ ) { %>
                            <option value="<%= collections.get(i).getID() %>"> <%= collections.get(i).getName()%>
                            </option>
                        <%  } %>
                </select>
            </span>    
            
            <%-- <td>Content Type:</td> --%>
            <span class="col-md-2"><label for="tresource_type"><fmt:message key="jsp.dspace-admin.authorize-advanced.type"/></label></span>
			<span class="col-md-10">
                <select class="form-control" name="resource_type" id="tresource_type">
                    <%-- <option value="<%=Constants.ITEM%>">item</option>
                    <option value="<%=Constants.BITSTREAM%>">bitstream</option> --%>
                    <option value="<%=Constants.ITEM%>"><fmt:message key="jsp.dspace-admin.authorize-advanced.type1"/></option>
                    <option value="<%=Constants.BITSTREAM%>"><fmt:message key="jsp.dspace-admin.authorize-advanced.type2"/></option>
                </select>
     		</span>
            
            <%-- <td>Group:</td> --%>
			<span class="col-md-2">
				<label for="tgroup_id"><fmt:message key="jsp.dspace-admin.general.group-colon"/></label>
			</span>
            <span class="col-md-10">
            	<select class="form-control" size="10" name="group_id" id="tgroup_id">
                    <%  for(int i = 0; i < groups.size(); i++ ) { %>
                            <option value="<%= groups.get(i).getID() %>"> <%= groups.get(i).getName()%>
                            </option>
                        <%  } %>
                </select>
            </span>
            
            <span class="col-md-2">            
            	<%-- <tr><td>Action:</td> --%>
				<label for="taction_id"><fmt:message key="jsp.dspace-admin.general.action-colon"/></label>
			</span>
			<span class="col-md-10">
                <select class="form-control" name="action_id" id="taction_id">
                    <%  for( int i = 0; i < Constants.actionText.length; i++ ) { %>
                        <option value="<%= i %>">
                            <%= Constants.actionText[i]%>
                        </option>
                    <%  } %>
                </select>
            </span>
        </div>
	    
	    <br/>        
        <div class="btn-group">
           	<%-- <input type="submit" name="submit_advanced_add" value="Add Policy"> --%>
            <input class="btn btn-primary" type="submit" name="submit_advanced_add" value="<fmt:message key="jsp.dspace-admin.authorize-advanced.add"/>" />
            <%-- <input type="submit" name="submit_advanced_clear" value="Clear Policies"> (warning: clears all policies for a given set of objects) --%>
        	<input class="btn btn-danger" type="submit" name="submit_advanced_clear" value="<fmt:message key="jsp.dspace-admin.authorize-advanced.clear"/>" /><span class="alert alert-warning"><fmt:message key="jsp.dspace-admin.authorize-advanced.warning"/></span>
        </div>    

    </form>
</dspace:layout>

