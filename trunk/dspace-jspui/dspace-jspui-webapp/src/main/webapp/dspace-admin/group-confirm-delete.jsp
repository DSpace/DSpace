<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Confirm deletion of a group
  -
  - Attributes:
  -    group   - group we may delete
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="org.dspace.eperson.Group" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    Group group = (Group) request.getAttribute("group");
%>
<dspace:layout titlekey="jsp.dspace-admin.group-confirm-delete.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

    <h1><fmt:message key="jsp.dspace-admin.group-confirm-delete.heading">
        <fmt:param><%= group.getName() %></fmt:param>
    </fmt:message></h1>
    
    <p><fmt:message key="jsp.dspace-admin.group-confirm-delete.confirm"/></p>
    

        <center>
            <table width="70%">
                <tr>
                    <td align="left">
                    <form method="post" action="">
                        <input type="hidden" name="group_id" value="<%= group.getID() %>"/>
                        <input type="submit" name="submit_confirm_delete" value="<fmt:message key="jsp.dspace-admin.general.delete"/>" />
                    </form>
                    </td>
                    <td align="right">
                    <form method="post" action="">
                        <input type="submit" name="submit_cancel_delete" value="<fmt:message key="jsp.dspace-admin.general.cancel"/>" />
                    </form>
                    </td>
                </tr>
            </table>
        </center>
</dspace:layout>

