<%--
  - Confirm deletion of a unit
  -
  - Attributes:
  -    unit   - unit we may delete
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="org.dspace.eperson.Unit" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    Unit unit = (Unit) request.getAttribute("unit");
%>
<dspace:layout titlekey="jsp.dspace-admin.unit-confirm-delete.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

    <h1><fmt:message key="jsp.dspace-admin.unit-confirm-delete.heading">
        <fmt:param><%= unit.getName() %></fmt:param>
    </fmt:message></h1>
    
    <p><fmt:message key="jsp.dspace-admin.unit-confirm-delete.confirm"/></p>
    

        <center>
            <table width="70%">
                <tr>
                    <td align="left">
                    <form method="post" action="">
                        <input type="hidden" name="unit_id" value="<%= unit.getID() %>"/>
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

