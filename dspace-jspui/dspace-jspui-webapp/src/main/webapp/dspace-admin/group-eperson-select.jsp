<%--
  - group_eperson_select.jsp
  -
  - $Id: group-eperson-select.jsp 3705 2009-04-11 17:02:24Z mdiggory $
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
  - Display list of epeople, with continue and cancel buttons
  -
  - Attributes:
  -   collections - a Collection [] containing all collections in the system
  - Returns:
  -   submit set to add_eperson_add, user has selected an eperson
  -   submit set to add_eperson_cancel, user has cancelled operation
  -   group_id - set if user has selected one
  -   eperson_id - set if user has selected one

  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.eperson.Group"   %>

<%
    Group group = (Group) request.getAttribute("group");
    EPerson [] epeople = 
        (EPerson []) request.getAttribute("epeople");
%>

<dspace:layout titlekey="jsp.dspace-admin.group-eperson-select.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin"
               nocache="true">

    <%-- <h1>Select EPerson to Add to Group <%= group.getID() %></h1> --%>
    <h1><fmt:message key="jsp.dspace-admin.group-eperson-select.heading">
        <fmt:param><%= group.getID() %></fmt:param>
    </fmt:message></h1>

    <form method="post" action="">

    <table class="miscTable" align="center">
        <tr>
            <td>
                <input type="hidden" name="group_id" value="<%=group.getID()%>"/>
                
                <select size="15" name="eperson_id" multiple="multiple">
                        <%  for (int i = 0; i < epeople.length; i++) { %>
                            <option value="<%= epeople[i].getID()%>">
                                <%= epeople[i].getEmail()%>
                            </option>
                        <%  } %>
                </select>
            </td>
        </tr>
    </table>

    <center>
        <table width="70%">
            <tr>
                <td align="left">
                    <%-- <input type="submit" name="submit_add_eperson_add" value="Add EPerson"> --%>
                    <input type="submit" name="submit_add_eperson_add" value="<fmt:message key="jsp.dspace-admin.group-eperson-select.add"/>" />
                </td>
                <td align="right">
                    <%-- <input type="submit" name="submit_add_eperson_cancel" value="Cancel"> --%>
                    <input type="submit" name="submit_add_eperson_cancel" value="<fmt:message key="jsp.dspace-admin.general.cancel"/>" />
                </td>
            </tr>
        </table>
    </center>        

    </form>
</dspace:layout>
