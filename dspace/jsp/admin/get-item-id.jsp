<%--
  - get-item-id.jsp
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
  - Form requesting a Handle or internal item ID for item editing
  -
  - Attributes:
  -     invalid.id  - if this attribute is present, display error msg
  --%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.core.ConfigurationManager" %>

<dspace:layout title="Edit Item" navbar="admin" locbar="link" parentlink="/admin" parenttitle="Administer">
    <H1>Edit or Delete Item</H1>
    
<%
    if (request.getAttribute("invalid.id") != null) { %>
    <P><strong>The ID you entered was invalid.</strong></P>
<%  } %>

    <P>Enter the Handle or internal item ID of the item you want to edit or
    delete:</P>
    
    <form method=GET>
        <center>
            <table class=miscTable>
                <tr class="oddRowEvenCol">
                    <td class="submitFormLabel">Handle:</td>
                    <td>
                            <input type="text" name="handle" value="<%= ConfigurationManager.getProperty("handle.prefix") %>/" size=12>
                            <input type="submit" name="submit" value="Find">
                    </td>
                </tr>
                <tr></tr>
                <tr class="oddRowEvenCol">
                    <td class="submitFormLabel">Internal ID:</td>
                    <td>
                            <input type="text" name="item_id" size=12>
                            <input type="submit" name="submit" value="Find">
                    </td>
                </tr>
            </table>
        </center>
    </form>
</dspace:layout>
