<%--
  - Confirm deletion of a etdunit
  -
  - Attributes:
  -    etdunit   - etdunit we may delete
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="org.dspace.content.EtdUnit" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    EtdUnit etdunit = (EtdUnit) request.getAttribute("etdunit");
%>
<dspace:layout titlekey="jsp.dspace-admin.etdunit-confirm-delete.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

    <h1><fmt:message key="jsp.dspace-admin.etdunit-confirm-delete.heading">
        <fmt:param><%= etdunit.getName() %></fmt:param>
    </fmt:message></h1>
    
    <p><fmt:message key="jsp.dspace-admin.etdunit-confirm-delete.confirm"/></p>
    

        <center>
            <table width="70%">
                <tr>
                    <td align="left">
                    <form method="post" action="">
                        <input type="hidden" name="etdunit_id" value="<%= etdunit.getID() %>"/>
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

