<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Confirm deletion of a bitstream format
  -
  - Attributes:
  -    format   - bitstream format we may delete
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page import="org.dspace.content.BitstreamFormat" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    BitstreamFormat format = (BitstreamFormat) request.getAttribute("format");
%>

<dspace:layout style="submission" titlekey="jsp.dspace-admin.confirm-delete-format.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

    <%-- <h1>Delete Bitstream Format: <%= format.getID() %></h1> --%>
    <h1><fmt:message key="jsp.dspace-admin.confirm-delete-format.heading">
        <fmt:param><%= format.getID() %></fmt:param>
    </fmt:message></h1>
    
    <%-- <p>Are you sure the format <strong><%= format.getShortDescription() %></strong>
    should be deleted?</p> --%>
    <p class="alert alert-info"><fmt:message key="jsp.dspace-admin.confirm-delete-format.confirm">
        <fmt:param><%= format.getShortDescription() %></fmt:param>
    </fmt:message></p>

    <%-- <p>Any existing bitstreams of this format will be reverted to the
    <em>unknown</em> bitstream format.</p> --%>
    <p class="alert alert-warning"><fmt:message key="jsp.dspace-admin.confirm-delete-format.warning"/></p>

    <form method="post" action="">
        			<input type="hidden" name="format_id" value="<%= format.getID() %>"/>
					<div class="btn-group">
                        <%-- <input type="submit" name="submit_confirm_delete" value="Delete"> --%>
                        <input class="btn btn-danger" type="submit" name="submit_confirm_delete" value="<fmt:message key="jsp.dspace-admin.general.delete"/>" />
                    
                        <%-- <input type="submit" name="submit_cancel" value="Cancel"> --%>
                        <input class="btn btn-default" type="submit" name="submit_cancel" value="<fmt:message key="jsp.dspace-admin.general.cancel"/>" />
                 	</div>
    </form>
</dspace:layout>
