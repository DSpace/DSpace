<%--
  - list-dctypes.jsp
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
  - Display list of DC types
  -
  - Attributes:
  -
  -   formats - the DC formats in the system (DCType[])
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.administer.DCType" %>

<%
    DCType[] types =
        (DCType[]) request.getAttribute("types");
%>

<dspace:layout title="Dublin Core Type Registry"
               navbar="admin"
               locbar="link"
               parentlink="/dspace-admin"
               parenttitle="Administer">

  <table width=95%>
    <tr>
      <td align=left>
        <h1>Dublin Core Type Registry</h1>
      </td>
      <td align="right" class="standard">
        <dspace:popup page="/help/site-admin.html#dublincore">Help...</dspace:popup>
      </td>
    </tr>
  </table>

    <p align="center">
        Note: Adding a new element to the DC Registry does not add a corresponding input field to the submit forms!
    </p>

    <table class="miscTable" align="center">
        <tr>
            <th class="oddRowOddCol"><strong>ID</strong></th>
            <th class="oddRowEvenCol"><strong>Element</strong></th>
            <th class="oddRowOddCol"><strong>Qualifier</strong></th>
            <th class="oddRowEvenCol"><strong>Scope Note</strong></th>
            <th class="oddRowOddCol">&nbsp;</th>
            <th class="oddRowEvenCol">&nbsp;</th>
        </tr>

<%
    String row = "even";
    for (int i = 0; i < types.length; i++)
    {
%>
        <form method=POST>
            <tr>
                <td class="<%= row %>RowOddCol"><%= types[i].getID() %></td>
                <td class="<%= row %>RowEvenCol">
                    <input type="text" name="element" value="<%= types[i].getElement() %>" size=12>
                </td>
                <td class="<%= row %>RowOddCol">
                    <input type="text" name="qualifier" value="<%= (types[i].getQualifier() == null ? "" : types[i].getQualifier()) %>" size=12>
                </td>
                <td class="<%= row %>RowEvenCol">
                    <textarea name="scope_note" rows=3 cols=40><%= (types[i].getScopeNote() == null ? "" : types[i].getScopeNote()) %></textarea>
                </td>
                <td class="<%= row %>RowOddCol">
                    <input type="hidden" name="dc_type_id" value="<%= types[i].getID() %>">
                    <input type="submit" name="submit_update" value="Update">
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
        
    <P align="center">
        Note: Adding a new element to the DC Registry does not add a corresponding input field to the submit forms!<br><br>
        <form method=POST>
            <input type="submit" name="submit_add" value="Add New">
        </form>
    </p>
</dspace:layout>
