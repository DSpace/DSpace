<%--
  - eperson-confirm-delete.jsp
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
  - Confirm deletion of an e-person
  -
  - Attributes:
  -    eperson   - eperson we may delete
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page import="org.dspace.eperson.EPerson" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    EPerson eperson = (EPerson) request.getAttribute("eperson");
%>
<dspace:layout titlekey="jsp.dspace-admin.eperson-confirm-delete.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

    <%-- <h1>Delete e-person: <%= eperson.getFullName() %> (<%= eperson.getEmail() %>)</h1> --%>
    <h1><fmt:message key="jsp.dspace-admin.eperson-confirm-delete.heading">
        <fmt:param><%= eperson.getFullName() %></fmt:param>
        <fmt:param><%= eperson.getEmail() %></fmt:param>
    </fmt:message></h1>
    
    <%-- <p>Are you sure this e-person should be deleted?</p> --%>
    <p><fmt:message key="jsp.dspace-admin.eperson-confirm-delete.confirm"/></p>
    
    <form method="post" action="">
        <input type="hidden" name="eperson_id" value="<%= eperson.getID() %>"/>

        <center>
            <table width="70%">
                <tr>
                    <td align="left">
                        <%-- <input type="submit" name="submit_confirm_delete" value="Delete"> --%>
                        <input type="submit" name="submit_confirm_delete" value="<fmt:message key="jsp.dspace-admin.general.delete"/>" />
                    </td>
                    <td align="right">
                        <%-- <input type="submit" name="submit_cancel" value="Cancel"> --%>
                        <input type="submit" name="submit_cancel" value="<fmt:message key="jsp.dspace-admin.general.cancel"/>" />
                    </td>
                </tr>
            </table>
        </center>
    </form>
</dspace:layout>

