<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Confirm deletion of a DC schema
  -
  - Attributes:
  -    schema   - schema we may delete
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page import="org.dspace.content.MetadataSchema" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    MetadataSchema schema = (MetadataSchema) request.getAttribute("schema");
%>

<dspace:layout titlekey="jsp.dspace-admin.confirm-delete-dcschema.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

    <%-- <H1>Delete Dublin Core Schema</H1> --%>
    <H1><fmt:message key="jsp.dspace-admin.confirm-delete-mdschema.heading">
        <fmt:param><%= schema.getName() %></fmt:param>
    </fmt:message></H1>
    
    <%-- <P>Are you sure the schema <strong><%= schema.getNamespace() %></strong>
    should be deleted?</P> --%> 
    <P><fmt:message key="jsp.dspace-admin.confirm-delete-mdschema.confirm">
        <fmt:param><%= schema.getName() %></fmt:param>
    </fmt:message></P>
    
    <%-- <P>This will result in an error if any metadata fields exist within this schema.</P>  --%>
    <P><fmt:message key="jsp.dspace-admin.confirm-delete-mdschema.warning"/></P>

    <form method="post">
        <input type="hidden" name="dc_schema_id" value="<%= schema.getSchemaID() %>">

        <center>
            <table width="70%">
                <tr>
                    <td align="left">
                        <%-- <input type="submit" name="submit_confirm_delete" value="Delete"> --%>
                        <input type="submit" name="submit_confirm_delete" value="<fmt:message key="jsp.dspace-admin.general.delete"/>">
                        <%-- <input type="submit" name="submit_cancel" value="Cancel">  --%>
                        <input type="submit" name="submit_cancel" value="<fmt:message key="jsp.dspace-admin.general.cancel"/>">
                    </td>
                </tr>
            </table>
        </center>
    </form>
</dspace:layout>
