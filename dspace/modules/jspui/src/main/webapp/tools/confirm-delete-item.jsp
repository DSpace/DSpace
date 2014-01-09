<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Confirm deletion of a item
  -
  - Attributes:
  -    item   - item we may delete
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
%>

<dspace:layout titlekey="jsp.tools.confirm-delete-item.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin"
               nocache="true">

    <%-- <h1>Delete Item: <%= (handle == null ? String.valueOf(item.getID()) : handle) %></h1> --%>
    <h1><fmt:message key="jsp.tools.confirm-delete-item.title"/>: <%= (handle == null ? String.valueOf(item.getID()) : handle) %></h1>

    <%-- <p>Are you sure this item should be completely deleted?  Caution:
    At present, no tombstone would be left.</p> --%>
	<p><fmt:message key="jsp.tools.confirm-delete-item.info"/></p>

    <dspace:item item="<%= item %>" style="full" />

    <form method="post" action="">
        <input type="hidden" name="item_id" value="<%= item.getID() %>"/>
        <input type="hidden" name="action" value="<%= EditItemServlet.CONFIRM_DELETE %>"/>

        <center>
            <table width="70%">
                <tr>
                    <td align="left">
                        <%-- <input type="submit" name="submit" value="Delete" /> --%>
						<input type="submit" name="submit" value="<fmt:message key="jsp.tools.general.delete"/>" />
                    </td>
                    <td align="right">
                        <%-- <input type="submit" name="submit_cancel" value="Cancel"> --%>
						<input type="submit" name="submit_cancel" value="<fmt:message key="jsp.tools.general.cancel"/>" />
                    </td>
                </tr>
            </table>
        </center>
    </form>
</dspace:layout>
