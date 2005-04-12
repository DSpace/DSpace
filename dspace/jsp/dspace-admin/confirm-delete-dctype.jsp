<%--
  - confirm-delete-dctype.jsp
  -
  - Version: $Revision$
  -
  - Date: $Date$
  -
  - Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
  - Institute of Technology.  All rights reserved.
  -
  - Redistribution and use in source and binary forms, with or without
  - modification, are permitted provided that the following conditions are
  - met:
  -
  - - Redistributions of source code must retain the above copyright
  - notice, this list of conditions and the following disclaimer.
  -
  - - Redistributions in binary form must reproduce the above copyright
  - notice, this list of conditions and the following disclaimer in the
  - documentation and/or other materials provided with the distribution.
  -
  - - Neither the name of the Hewlett-Packard Company nor the name of the
  - Massachusetts Institute of Technology nor the names of their
  - contributors may be used to endorse or promote products derived from
  - this software without specific prior written permission.
  -
  - THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  - ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  - LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  - A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  - HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  - INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
  - BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
  - OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  - ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  - TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
  - USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
  - DAMAGE.
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

<%@ page import="org.dspace.administer.DCType" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    DCType type = (DCType) request.getAttribute("type");

    String typeName = type.getElement() +
        (type.getQualifier() == null ? "" : "." + type.getQualifier());
%>

<dspace:layout titlekey="jsp.dspace-admin.confirm-delete-dctype.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

    <%-- <H1>Delete Dublin Core Format: <code><%= typeName %></code></H1> --%>
    <H1><fmt:message key="jsp.dspace-admin.confirm-delete-dctype.heading"/> <code><%= typeName %></code></H1>
    
    <%-- <P>Are you sure the format <strong><%= typeName %></strong>
    should be deleted?</P> --%>
    <P><fmt:message key="jsp.dspace-admin.confirm-delete-dctype.confirm1"/> <strong><%= typeName %></strong>
    <fmt:message key="jsp.dspace-admin.confirm-delete-dctype.confirm2"/></P>
    
    <%-- <P>This will result in an error if any DC values have this type.</P> --%>
    <P><fmt:message key="jsp.dspace-admin.confirm-delete-dctype.warning"/></P>

    <form method=POST>
        <input type="hidden" name="dc_type_id" value="<%= type.getID() %>">

        <center>
            <table width="70%">
                <tr>
                    <td align="left">
                        <%-- <input type="submit" name="submit_confirm_delete" value="Delete"> --%>
                        <input type="submit" name="submit_confirm_delete" value="<fmt:message key="jsp.dspace-admin.confirm-delete-dctype.delete"/>">
                    </td>
                    <td align="right">
                        <%-- <input type="submit" name="submit_cancel" value="Cancel"> --%>
                        <input type="submit" name="submit_cancel" value="<fmt:message key="jsp.dspace-admin.confirm-delete-dctype.cancel"/>">
                    </td>
                </tr>
            </table>
        </center>
    </form>
</dspace:layout>

