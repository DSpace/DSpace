<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Confirm withdrawal of a item
  -
  - Attributes:
  -    item   - item we may withdraw
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page import="org.dspace.app.webui.servlet.admin.EditItemServlet" %>
<%@ page import="org.dspace.content.Item" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    String handle = (String) request.getAttribute("handle");
    Item item = (Item) request.getAttribute("item");
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

<dspace:layout titlekey="jsp.tools.confirm-withdraw-item.title"
               navbar="<%= naviAdmin %>"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="<%= link %>"
               nocache="true">

    <%-- <h1>Withdraw Item: <%= (handle == null ? String.valueOf(item.getID()) : handle) %></h1> --%>
    <h1><fmt:message key="jsp.tools.confirm-withdraw-item.title"/>: <%= (handle == null ? String.valueOf(item.getID()) : handle) %></h1>
	
    <%-- <p>Are you sure this item should be withdrawn from the archive?</p> --%>
	<p><fmt:message key="jsp.tools.confirm-withdraw-item.question"/></p>
    
    <dspace:item item="<%= item %>" style="full" />

    <form method="post" action="">
        <input type="hidden" name="item_id" value="<%= item.getID() %>"/>
        <input type="hidden" name="action" value="<%= EditItemServlet.CONFIRM_WITHDRAW %>"/>

        <center>
            <table width="70%">
                <tr>
                    <td align="left">
                        <%-- <input type="submit" name="submit" value="Withdraw" /> --%>
						<input type="submit" name="submit" value="<fmt:message key="jsp.tools.confirm-withdraw-item.withdraw.button"/>" />
                    </td>
                    <td align="right">
                        <%-- <input type="submit" name="submit_cancel" value="Cancel" /> --%>
						<input type="submit" name="submit_cancel" value="<fmt:message key="jsp.tools.general.cancel"/>" />
                    </td>
                </tr>
            </table>
        </center>
    </form>
</dspace:layout>
