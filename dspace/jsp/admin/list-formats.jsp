<%--
  - list-formats.jsp
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
  - Display list of bitstream formats
  -
  - Attributes:
  -
  -   formats - the bitstream formats in the system (BitstreamFormat[])
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.content.BitstreamFormat" %>

<%
    BitstreamFormat[] formats =
        (BitstreamFormat[]) request.getAttribute("formats");
%>

<dspace:layout title="Bitstream Format Registry" navbar="admin" locbar="link" parentlink="/admin" parenttitle="Administer">

    <h1>Bitstream Format Registry</h1>

    <P><strong>Extensions</strong> are comma-separated lists of filename
    extensions used to automatically identify the formats of uploaded files.
    Do not include the dot.</P>

    <P>When you add a bitstream format, it is initially made "internal" so that
    it does not appear in the submission UI before you've finished editing
    the format metadata.  Be sure to uncheck "internal" if the format should
    appear in the submission UI list of formats.</P>

    <table class="miscTable" align="center">
        <tr>
            <th class="oddRowOddCol"><strong>ID</strong></th>
            <th class="oddRowEvenCol"><strong>MIME Type</strong></th>
            <th class="oddRowOddCol"><strong>Name</strong></th>
            <th class="oddRowEvenCol"><strong>Long Description</strong></th>
            <th class="oddRowOddCol"><strong>Support Level</strong></th>
            <th class="oddRowEvenCol"><strong>Internal?</strong></th>
            <th class="oddRowOddCol"><strong>Extensions</strong></th>
            <th class="oddRowEvenCol">&nbsp;</th>
            <th class="oddRowOddCol">&nbsp;</th>
        </tr>

<%
    String row = "even";
    for (int i = 0; i < formats.length; i++)
    {
        String[] extensions = formats[i].getExtensions();
        String extValue = "";

        for (int j = 0 ; j < extensions.length; j++)
        {
            if (j > 0)
            {
                extValue = extValue + ", ";
            }
            extValue = extValue + extensions[j];
        }
%>
        <form method=POST>
            <tr>
                <td class="<%= row %>RowOddCol"><%= formats[i].getID() %></td>
                <td class="<%= row %>RowEvenCol">
                    <input type="text" name="mimetype" value="<%= formats[i].getMIMEType() %>" size=14>
                </td>
                <td class="<%= row %>RowOddCol">
                    <input type="text" name="short_description" value="<%= formats[i].getShortDescription() %>" size=10>
                </td>
                <td class="<%= row %>RowEvenCol">
                    <input type="text" name="description" value="<%= formats[i].getDescription() %>" size=20>
                </td>
                <td class="<%= row %>RowOddCol">
                    <select name="support_level">
                        <option value="0" <%= formats[i].getSupportLevel() == 0 ? "SELECTED" : "" %>>Unknown</option>
                        <option value="1" <%= formats[i].getSupportLevel() == 1 ? "SELECTED" : "" %>>Known</option>
                        <option value="2" <%= formats[i].getSupportLevel() == 2 ? "SELECTED" : "" %>>Supported</option>
                    </select>
                </td>
                <td class="<%= row %>RowEvenCol" align="center">
                    <input type="checkbox" name="internal" value="true"<%= formats[i].isInternal() ? " CHECKED" : "" %>>
                </td>
                <td class="<%= row %>RowOddCol">
                    <input type="text" name="extensions" value="<%= extValue %>" size="10">
                </td>
                <td class="<%= row %>RowEvenCol">
                    <input type="hidden" name="format_id" value="<%= formats[i].getID() %>">
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
        
    <P align="center">
        <form method=POST>
            <input type="submit" name="submit_add" value="Add New">
        </form>
    </p>
</dspace:layout>
