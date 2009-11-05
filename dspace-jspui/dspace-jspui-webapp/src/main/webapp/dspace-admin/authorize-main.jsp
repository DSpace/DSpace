<%--
  - authorize_policy_main.jsp
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
  - main page for authorization editing
  -
  - Attributes:
  -   none
  -
  - Returns:
  -   submit_community
  -   submit_collection
  -   submit_item
  -       item_handle
  -       item_id
  -   submit_advanced
  -
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.content.Collection" %>

<% request.setAttribute("LanguageSwitch", "hide"); %>

<%
// this space intentionally left blank
%>

<dspace:layout titlekey="jsp.dspace-admin.authorize-main.title"
               navbar="admin"
               locbar="link"
               parenttitle="general.administer"
               parentlink="/dspace-admin">

    <%-- <h1>Administer Authorization Policies</h1> --%>
    <h1><fmt:message key="jsp.dspace-admin.authorize-main.adm"/></h1>
  <table width="95%">
    <tr>
      <td align="left">
          <%-- <h3>Choose a resource to manage policies for:</h3> --%>
		  <h3><fmt:message key="jsp.dspace-admin.authorize-main.choose"/></h3>
      </td>
      <td align="right" class="standard">
        <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#authorize\"%>"><fmt:message key="jsp.help"/></dspace:popup>
      </td>
    </tr>
  </table>
    
    
    <form method="post" action="">    

    <center>
        <table width="70%">
            <tr>
                <td align="center">
                    <%-- <input type="submit" name="submit_community" value="Manage a Community's Policies"> --%>
                    <input type="submit" name="submit_community" value="<fmt:message key="jsp.dspace-admin.authorize-main.manage1"/>" />
                </td>
            </tr>
            <tr>
                <td align="center">
                    <%-- <input type="submit" name="submit_collection" value="Manage Collection's Policies"> --%>
                    <input type="submit" name="submit_collection" value="<fmt:message key="jsp.dspace-admin.authorize-main.manage2"/>" />
                </td>
            </tr>
            <tr>
                <td align="center">
                    <%-- <input type="submit" name="submit_item" value="Manage An Item's Policies"> --%>
                    <input type="submit" name="submit_item" value="<fmt:message key="jsp.dspace-admin.authorize-main.manage3"/>" />
                </td>
            </tr>
            <tr>
                <td align="center">
                    <%-- <input type="submit" name="submit_advanced" value="Advanced/Item Wildcard Policy Admin Tool"> --%>
                    <input type="submit" name="submit_advanced" value="<fmt:message key="jsp.dspace-admin.authorize-main.advanced"/>" />
                </td>
            </tr>
        </table>
    </center>        

    </form>
</dspace:layout>
