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
    Group      [] groups     = (Group      []) request.getAttribute("groups"     );
    Collection [] collections= (Collection []) request.getAttribute("collections");
    request.setAttribute("LanguageSwitch", "hide");
%>

<dspace:layout titlekey="jsp.dspace-admin.authorize-advanced.advanced"
               navbar="admin"
               locbar="link"
               parentlink="/dspace-admin"
               parenttitlekey="jsp.administer">

<h1><fmt:message key="jsp.dspace-admin.authorize-advanced.advanced"/></h1>

	<%-- <p>Allows you to do wildcard additions to and clearing
       of policies for types of content contained in a collection.
       Warning, dangerous - removing READ permissions from
       items will make them not viewable!  <dspace:popup page="/help/site-admin.html#advancedpolicies">More help...</dspace:popup></p> --%>
	<div><fmt:message key="jsp.dspace-admin.authorize-advanced.text"/> <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") +\"#advancedpolicies\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup></div>

    <form method="post" action="">

    <table class="miscTable" align="center" summary="Advanced policy manager">
        <tr>     
            <%-- <td>Collection:</td> --%>
            <th id="t1"><label for ="tcollection"><fmt:message key="jsp.dspace-admin.authorize-advanced.col"/></label></th>
            <td headers="t1">
                <select size="10" name="collection_id" id="tcollection">
                    <%  for(int i = 0; i < collections.length; i++ ) { %>
                            <option value="<%= collections[i].getID() %>"> <%= collections[i].getMetadata("name")%>
                            </option>
                        <%  } %>
                </select>
            </td>
        </tr>

        <tr>
            <%-- <td>Content Type:</td> --%>
            <th id="t2"><label for="tresource_type"><fmt:message key="jsp.dspace-admin.authorize-advanced.type"/></label></th>
            <td headers="t2">
                <select name="resource_type" id="tresource_type">
                    <%-- <option value="<%=Constants.ITEM%>">item</option>
                    <option value="<%=Constants.BITSTREAM%>">bitstream</option> --%>
                    <option value="<%=Constants.ITEM%>"><fmt:message key="jsp.dspace-admin.authorize-advanced.type1"/></option>
                    <option value="<%=Constants.BITSTREAM%>"><fmt:message key="jsp.dspace-admin.authorize-advanced.type2"/></option>
                </select>
            </td>
        </tr>

        <tr>     
            <%-- <td>Group:</td> --%>
            <th id="t3"><fmt:message key="jsp.dspace-admin.general.group-colon"/></th>
            <td headers="t3">
                <select size="10" name="group_id" id="tgroup_id">
                    <%  for(int i = 0; i < groups.length; i++ ) { %>
                            <option value="<%= groups[i].getID() %>"> <%= groups[i].getName()%>
                            </option>
                        <%  } %>
                </select>
            </td>
        </tr>

        <tr>
            <%-- <tr><td>Action:</td> --%>
            <th id="t4"><label for="taction_id"><fmt:message key="jsp.dspace-admin.general.action-colon"/></label></th>
            <td headers="t4">
                <select name="action_id" id="taction_id">
                    <%  for( int i = 0; i < Constants.actionText.length; i++ ) { %>
                        <option value="<%= i %>">
                            <%= Constants.actionText[i]%>
                        </option>
                    <%  } %>
                </select>
            </td>
        </tr>
              
    </table>
    
    <center>
        <table width="70%">
            <tr>
                <td align="left">
                    <%-- <input type="submit" name="submit_advanced_add" value="Add Policy"> --%>
                    <input type="submit" name="submit_advanced_add" value="<fmt:message key="jsp.dspace-admin.authorize-advanced.add"/>" />
                </td>
                <td align="right">
                    <%-- <input type="submit" name="submit_advanced_clear" value="Clear Policies"> (warning: clears all policies for a given set of objects) --%>
                    <input type="submit" name="submit_advanced_clear" value="<fmt:message key="jsp.dspace-admin.authorize-advanced.clear"/>" /></td> <td><fmt:message key="jsp.dspace-admin.authorize-advanced.warning"/>
                </td>
            </tr>
        </table>
    </center>        

    </form>
</dspace:layout>

