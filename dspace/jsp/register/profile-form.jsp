<%--
  - profile-form.jsp
  -
  - Version: $Revision$
  -
  - Date: $Date$
  -
  - Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
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
  - User profile editing form.
  -
  - This isn't a full page, just the fields for entering a user's profile.
  -
  - Attributes to pass in:
  -   eperson       - the EPerson to edit the profile for
  --%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.eperson.EPerson" %>

<%
    EPerson epersonForm = (EPerson) request.getAttribute("eperson");

    // Get non-null values
    String lastName = epersonForm.getLastName();
    if (lastName == null) lastName = "";

    String firstName = epersonForm.getFirstName();
    if (firstName == null) firstName = "";

    String phone = epersonForm.getMetadata("phone");
    if (phone == null) phone = "";
%>

<table border=0 align=center cellpadding=5>
    <tr>
        <td align=right class=standard><strong>First name*:</strong></td>
        <td class=standard><input type=text name="first_name" size="40" value="<%= firstName %>"></td>
    </tr>
    <tr>
        <td align=right class=standard><strong>Last name*:</strong></td>
        <td class=standard><input type=text name="last_name" size="40" value="<%= lastName %>"></td>
    </tr>
    <tr>
        <td align=right class=standard><strong>Contact telephone:</strong></td>
        <td class=standard><input type=text name="phone" size="40" value="<%= phone %>"></td>
    </tr>
</table>
