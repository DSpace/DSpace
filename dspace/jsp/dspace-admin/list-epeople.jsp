<%--
  - list-epeople.jsp
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
  - Display list of E-people
  -
  - Attributes:
  -
  -   epeople - EPerson[] - all epeople in the system
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.eperson.EPerson" %>

<%
    EPerson[] epeople =
        (EPerson[]) request.getAttribute("epeople");
%>

<dspace:layout title="E-People"
               navbar="admin"
               locbar="link"
               parentlink="/dspace-admin"
               parenttitle="Administer">

    <h1>E-People</h1>

    <P>Note that presently you can only update one line at a time - make
    changes to a single e-person and click the relevant "update" button.</P>
    
    <P align="center">
        <form method=POST>
            <input type="submit" name="submit_add" value="Add New">
        </form>
    </p>

    <table class="miscTable" align="center">
        <tr>
            <th class="oddRowOddCol"><strong><A HREF="<%= request.getContextPath() %>/dspace-admin/edit-epeople?sortby=id">ID</A></strong></th>
            <th class="oddRowEvenCol"><strong><A HREF="<%= request.getContextPath() %>/dspace-admin/edit-epeople?sortby=email">E-mail Address</A></strong></th>
            <th class="oddRowOddCol"><strong><A HREF="<%= request.getContextPath() %>/dspace-admin/edit-epeople?sortby=lastname">Last Name</A></strong></th>
            <th class="oddRowEvenCol"><strong>First Name</strong></th>
            <th class="oddRowOddCol"><strong>Can Log In?</strong></th>
            <th class="oddRowEvenCol"><strong>Must Use Cert?</strong></th>
            <th class="oddRowOddCol"><strong>Telephone</strong></th>
            <th class="oddRowEvenCol">&nbsp;</th>
            <th class="oddRowOddCol">&nbsp;</th>
        </tr>

<%
    String row = "even";
    for (int i = 0; i < epeople.length; i++)
    {
%>
        <form method=POST>
            <tr>
                <td class="<%= row %>RowOddCol"><%= epeople[i].getID() %></td>
                <td class="<%= row %>RowEvenCol">
                    <input type="text" name="email" value="<%= (epeople[i].getEmail() == null ? "" : epeople[i].getEmail()) %>" size=12>
                </td>
                <td class="<%= row %>RowOddCol">
                    <input type="text" name="lastname" value="<%= (epeople[i].getLastName() == null ? "" : epeople[i].getLastName()) %>" size=12>
                </td>
                <td class="<%= row %>RowEvenCol">
                    <input type="text" name="firstname" value="<%= (epeople[i].getFirstName() == null ? "" : epeople[i].getFirstName()) %>" size=12>
                </td>
                <td class="<%= row %>RowOddCol" align="center">
                    <input type="checkbox" name="active" value="true"<%= epeople[i].canLogIn() ? " CHECKED" : "" %>>
                </td>
                <td class="<%= row %>RowEvenCol" align="center">
                    <input type="checkbox" name="require_certificate" value="true"<%= epeople[i].getRequireCertificate() ? " CHECKED" : "" %>>
                </td>
                <td class="<%= row %>RowOddCol">
                    <input type="text" name="phone" value="<%= (epeople[i].getMetadata("phone") == null ? "" : epeople[i].getMetadata("phone")) %>" size=12>
                </td>
                <td class="<%= row %>RowEvenCol">
                    <input type="hidden" name="eperson_id" value="<%= epeople[i].getID() %>">
<%      if (request.getParameter("sortby") != null) { %>
                    <input type="hidden" name="sortby" value="<%= request.getParameter("sortby") %>">
<%      } %>
                    <input type="submit" name="submit_update" value="Update">
                </td>
                <td class="<%= row %>RowOddCol">
                    <input type="submit" name="submit_delete" value="Delete...">
                </td>
            </tr>
        </form>
<%
        row = (row.equals("odd") ? "even" : "odd");
    }
%>
    </table>

</dspace:layout>
