<%--
  - eperson-browse.jsp
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
  - Display list of E-people, with pagination
  -
  - Attributes:
  -
  -   epeople    - EPerson[] - all epeople to browse
  -   page_size  - size of pages (number of epeople per page)
  -   page_index - current page to display
  -   page_count - number of pages
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.eperson.EPerson" %>

<%
    EPerson[] epeople =
        (EPerson[]) request.getAttribute("epeople");
    int pageSize  = ((Integer)request.getAttribute("page_size" )).intValue();
    int pageIndex = ((Integer)request.getAttribute("page_index")).intValue();
    int pageCount = ((Integer)request.getAttribute("page_count")).intValue();

    int firstEPerson = pageSize*pageIndex;
    int lastEPerson  = firstEPerson + (pageSize - 1);  // index of last person
                                                       // most common case is full page

    if (lastEPerson >= epeople.length)
    {
        // oops, less than a full page left, trim
        lastEPerson = -1 + firstEPerson + ((epeople.length-firstEPerson) % pageSize); 
    }


    String previousButton = "";
    String nextButton     = "";

    if (pageIndex > 0)
    {
        // not at start, so create 'previous' button
        previousButton = "<input type=\"hidden\" name=\"page_index\" value=\"" + pageIndex + "\">" +
                         "<input type=\"hidden\" name=\"page_request\" value=\"previous\"> "       +
                         "<input type=\"submit\" name=\"submit_browse\" value=\"Previous\">";
    }

    if (pageIndex < (pageCount-1) )
    {
        // not showing last either, so create 'next' button
        nextButton = "<input type=\"hidden\" name=\"page_index\" value=\"" + pageIndex + "\">" +
                         "<input type=\"hidden\" name=\"page_request\" value=\"next\"> "       +
                         "<input type=\"submit\" name=\"submit_browse\" value=\"Next\">";
    }

%>

<dspace:layout title="E-People"
               navbar="admin"
               locbar="link"
               parentlink="/admin"
               parenttitle="Administer">

    <h1>Browse EPeople <%=firstEPerson%>-<%=lastEPerson%> of <%=epeople.length%></h1>

    <table class="miscTable" align="center">
        <tr>
            <th class="oddRowOddCol"> <strong><A HREF="<%= request.getContextPath() %>/admin/edit-epeople?submit_browse=1&sortby=id">ID</A></strong></th>
            <th class="oddRowEvenCol"><strong><A HREF="<%= request.getContextPath() %>/admin/edit-epeople?submit_browse=1&sortby=email">E-mail Address</A></strong></th>
            <th class="oddRowOddCol"> <strong><A HREF="<%= request.getContextPath() %>/admin/edit-epeople?submit_browse=1&sortby=lastname">Last Name</A></strong></th>
            <th class="oddRowEvenCol"><strong>First Name</strong></th>
            <th class="oddRowOddCol"> <strong>Can Log In?</strong></th>
            <th class="oddRowEvenCol"><strong>Must Use Cert?</strong></th>
            <th class="oddRowOddCol"> <strong>Self Registered</strong></th>
            <th class="oddRowEvenCol"><strong>Telephone</strong></th>
            <th class="oddRowOddCol">&nbsp;</th>
            <th class="oddRowEvenCol">&nbsp;</th>
        </tr>

<%
    String row = "even";
    for (int i = firstEPerson; i <= lastEPerson; i++)
    {
        EPerson e = epeople[i];
        String commandString = request.getContextPath() + "/admin/edit-epeople?submit_edit&eperson_id=" + e.getID();
%>
        <form method=POST>
            <tr>
                <td class="<%= row %>RowOddCol"><%= e.getID() %></td>
                <td class="<%= row %>RowEvenCol">
                    <%= (e.getEmail() == null ? "" : e.getEmail()) %>
                </td>
                <td class="<%= row %>RowOddCol">
                    <%= (e.getLastName() == null ? "" : e.getLastName()) %>
                </td>
                <td class="<%= row %>RowEvenCol">
                    <%= (e.getFirstName() == null ? "" : e.getFirstName()) %>
                </td>
                <td class="<%= row %>RowOddCol" align="center">
                    <%= e.canLogIn() ? "yes" : "no" %>
                </td>
                <td class="<%= row %>RowEvenCol" align="center">
                    <%= e.getRequireCertificate() ? "yes" : "no" %>
                </td>
                <td class="<%= row %>RowOddCol">
                    <%= e.getSelfRegistered() ? "yes" : "no" %>
                </td>
                <td class="<%= row %>RowEvenCol">
                    <%= (e.getMetadata("phone") == null ? "" : e.getMetadata("phone")) %>
                </td>
                <td class="<%= row %>RowOddCol">
                    <input type="hidden" name="eperson_id" value="<%= e.getID() %>">
<%      if (request.getParameter("sortby") != null) { %>
                    <input type="hidden" name="sortby" value="<%= request.getParameter("sortby") %>">
<%      } %>
                    <input type="submit" name="submit_edit" value="Edit...">
                </td>
                <td class="<%= row %>RowEvenCol">
                    <input type="submit" name="submit_delete" value="Delete...">
                </td>
            </tr>
        </form>
<%
        row = (row.equals("odd") ? "even" : "odd");
    }
%>
    </table>

    <form method=POST>
    <%=previousButton%>
    </form>

    <form method=POST>
    <%=nextButton%>
    </form>



</dspace:layout>
