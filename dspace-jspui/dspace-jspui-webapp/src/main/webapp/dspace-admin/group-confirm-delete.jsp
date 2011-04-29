<%--
  - group-confirm-delete.jsp
  -
  - Version: $Revision: 3705 $
  -
  - Date: $Date: 2009-04-11 13:02:24 -0400 (Sat, 11 Apr 2009) $
  -
  - Copyright (C) 2009, The DSpace Foundation.  All rights reserved.
  -
  - Redistribution and use in source and binary forms, with or without
  - modification, are permitted provided that the following conditions are
  - met:
  -
  -     - Redistributions of source code must retain the above copyright
  -       notice, this list of conditions and the following disclaimer.
  -
  -     - Redistributions in binary form must reproduce the above copyright
  -       notice, this list of conditions and the following disclaimer in the
  -       documentation and/or other materials provided with the distribution.
  -
  -     - Neither the name of the DSpace Foundation nor the names of their
  -       contributors may be used to endorse or promote products derived from
  -       this software without specific prior written permission.
  -
  - THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  -  ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
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

