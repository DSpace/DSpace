<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Confirm deletion of a DC type
  -
  - Attributes:
  -    type   - DCType we may delete
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page import="org.dspace.content.MetadataField" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    MetadataField type = (MetadataField) request.getAttribute("type");

    String typeName = type.getElement() +
        (type.getQualifier() == null ? "" : "." + type.getQualifier());

    boolean failed = false;
    if (request.getAttribute("failed") != null)
    {
        failed = ((Boolean)request.getAttribute("failed")).booleanValue();
    }
%>

<dspace:layout style="submission" titlekey="jsp.dspace-admin.confirm-delete-mdfield.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

    <%-- <h1>Delete Metadata Field: <code><%= typeName %></code></h1> --%>
    <h1><fmt:message key="jsp.dspace-admin.confirm-delete-mdfield.heading">
        <fmt:param><%= typeName %></fmt:param>
    </fmt:message></h1>

    <%-- <P>Are you sure the metadata field <strong><%= typeName %></strong>
    should be deleted?</p> --%>
    <p class="alert alert-info"><fmt:message key="jsp.dspace-admin.confirm-delete-mdfield.confirm">
        <fmt:param><%= typeName %></fmt:param>
    </fmt:message></p>
    
    <%
        if (!failed) { %>
            <%-- <P>This will result in an error if any items have values for this metadata field.</P> --%>
            <p class="alert alert-warning"><fmt:message key="jsp.dspace-admin.confirm-delete-mdfield.warning"/></p>
            <form method="post" action="">
                <input type="hidden" name="dc_type_id" value="<%= type.getID() %>">
						<div class="btn-group">
                                <%-- <input type="submit" name="submit_confirm_delete" value="Delete"> --%>
                                <input class="btn btn-danger" type="submit" name="submit_confirm_delete" value="<fmt:message key="jsp.dspace-admin.general.delete"/>" />
                                <%-- <input type="submit" name="submit_cancel" value="Cancel"> --%>
                                <input class="btn btn-default" type="submit" name="submit_cancel" value="<fmt:message key="jsp.dspace-admin.general.cancel"/>" />
						</div>	
            </form><%
        } else {%>
            <%-- <P>Unable to delete this metadata field. This is most likely to be because it is referenced by at least one item.</P> --%>
            <p class="alert alert-danger"><fmt:message key="jsp.dspace-admin.confirm-delete-mdfield.failed"/></p><%
        }
     %>

</dspace:layout>
