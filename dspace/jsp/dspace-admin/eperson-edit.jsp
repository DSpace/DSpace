<%--
  - eperson-edit.jsp
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
  - eperson editor - for new or existing epeople
  -
  - Attributes:
  -   eperson - eperson to be edited
  -   email_exists - if non-null, user has attempted to enter a duplicate email
  -                  address, so an error should be displayed
  -
  - Returns:
  -   submit_save   - admin wants to save edits
  -   submit_delete - admin wants to delete edits
  -   submit_cancel - admin wants to cancel
  -
  -   eperson_id
  -   email
  -   firstname
  -   lastname
  -   phone
  -   can_log_in          - (boolean)
  -   require_certificate - (boolean)
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.eperson.EPerson, org.dspace.core.ConfigurationManager" %>

<%
    EPerson eperson = (EPerson) request.getAttribute("eperson");

    String email     = eperson.getEmail();
    String firstName = eperson.getFirstName();
    String lastName  = eperson.getLastName();
    String phone     = eperson.getMetadata("phone");
    String netid = eperson.getNetid();
    boolean emailExists = (request.getAttribute("email_exists") != null);

    boolean ldap_enabled = ConfigurationManager.getBooleanProperty("ldap.enable");
%>

<dspace:layout titlekey="jsp.dspace-admin.eperson-edit.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin"
               nocache="true">

  <table width=95%>
    <tr>
      <td align=left>
        <%-- <h1>Edit EPerson <%= eperson.getEmail() %>:</h1> --%>
        <h1><fmt:message key="jsp.dspace-admin.eperson-edit.heading">
            <fmt:param><%= eperson.getEmail() %></fmt:param>
        </fmt:message></h1>
      </td>
      <td align="right" class="standard">
        <dspace:popup page="/help/site-admin.html#epeople"><fmt:message key="jsp.help"/></dspace:popup>
      </td>
    </tr>
  </table>

<% if (emailExists)
	{ %><P><strong>
	     <fmt:message key="jsp.dspace-admin.eperson-edit.emailexists"/>
	   </strong></P>
<%  } %>

    <form method=POST>

    <table class="miscTable" align="center">
        <tr>     
            <%-- <td>Email:</td> --%>
            <td><fmt:message key="jsp.dspace-admin.eperson-edit.email"/></td>
            <td>
                <input type="hidden" name="eperson_id" value="<%=eperson.getID()%>">
                <input name="email" size="24" value="<%=email == null ? "" : email%>">
            </td>
        </tr>

        <tr>
            <%-- <td>Last Name:</td> --%>
            <td><fmt:message key="jsp.dspace-admin.eperson.general.firstname"/></td>
            <td>
                <input name="lastname" size="24" value="<%=lastName == null ? "" : lastName%>">
            </td>
        </tr>

        <tr>
            <%-- <td>First Name:</td> --%>
            <td><fmt:message key="jsp.dspace-admin.eperson.general.lastname"/></td>
            <td>
                <input name="firstname" size="24" value="<%=firstName == null ? "" : firstName%>">
            </td>
        </tr>

        <% if (ldap_enabled) { %>
	<tr>
            <td>LDAP NetID:</td>
            <td>
                <input name="netid" size="24" value="<%=netid == null ? "" : netid%>">
            </td>
        </tr>
        <% } %>

        <tr>
            <%-- <td>Phone:</td> --%>
            <td><fmt:message key="jsp.dspace-admin.eperson-edit.phone"/></td>
            <td>
                <input name="phone" size="24" value="<%=phone == null ? "" : phone%>">
            </td>
        </tr>

        <tr>
            <%-- <td>Can Log In:</td> --%>
            <td><fmt:message key="jsp.dspace-admin.eperson-edit.can"/></td>
            <td>
                <input type="checkbox" name="can_log_in" value="true"<%= eperson.canLogIn() ? " CHECKED" : "" %>>
            </td>
        </tr>

        <tr>
            <%-- <td>Require Certificate:</td> --%>
            <td><fmt:message key="jsp.dspace-admin.eperson-edit.require"/></td>
            <td>
                <input type="checkbox" name="require_certificate" value="true"<%= eperson.getRequireCertificate() ? " CHECKED" : "" %>>
            </td>
        </tr>


    </table>
    
    <center>
        <table width="70%">
            <tr>
                <td align="left">
                    <%-- <input type="submit" name="submit_save" value="Save Edits"> --%>
                    <input type="submit" name="submit_save" value="<fmt:message key="jsp.dspace-admin.general.save"/>">
                </td>
                <td align="right">
                    <%-- <input type="submit" name="submit_delete" value="Delete EPerson..."> --%>
                    <input type="submit" name="submit_delete" value="<fmt:message key="jsp.dspace-admin.general.delete"/>">
                </td>
            </tr>
        </table>
    </center>        

    </form>
</dspace:layout>
