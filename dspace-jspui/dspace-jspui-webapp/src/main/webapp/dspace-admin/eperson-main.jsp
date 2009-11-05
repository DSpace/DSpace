<%--
  - eperson-main.jsp
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
  - main page for eperson admin
  -
  - Attributes:
  -   no_eperson_selected - if a user tries to edit or delete an EPerson without
  -                         first selecting one
  -
  - Returns:
  -   submit_add    - admin wants to add an eperson
  -   submit_browse - admin wants to browse epeople
  -
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
	
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
   boolean noEPersonSelected = (request.getAttribute("no_eperson_selected") != null);
%>

<dspace:layout titlekey="jsp.dspace-admin.eperson-main.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

    <%-- <h1>Administer EPeople</h1> --%>
    <h1><fmt:message key="jsp.dspace-admin.eperson-main.heading"/></h1>
  <table width="95%">
    <tr>
      <td align="left">
        <%-- <h3>Choose an action:</h3> --%>
        <h3><fmt:message key="jsp.dspace-admin.eperson-main.choose"/></h3>
      </td>
      <td align="right" class="standard">
        <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"epeople\"%>"><fmt:message key="jsp.help"/></dspace:popup>
      </td>
    </tr>
  </table>

<% if (noEPersonSelected)
	{ %><p><strong>
	     <fmt:message key="jsp.dspace-admin.eperson-main.noepersonselected"/>
	   </strong></p>
<%  } %>
    
    <form name="epersongroup" method="post" action="">    

    <center>
        <table width="90%">
            <tr>
                <td colspan="3" align="center">
                    <%-- <input type="submit" name="submit_add" value="Add EPerson..."> --%>
                    <input type="submit" name="submit_add" value="<fmt:message key="jsp.dspace-admin.eperson-main.add"/>" />
                </td>
            </tr>
            <tr>
            	<%-- <td colspan="3"><strong>OR</strong></td> --%>
            	<td colspan="3"><strong><fmt:message key="jsp.dspace-admin.eperson-main.or"/></strong></td>
            </tr>
            <tr>
                <td>
                    <dspace:selecteperson multiple="false" />
                </td>
                <td>
                	<%-- then&nbsp;<input type="submit" name="submit_edit" value="Edit..." onclick="javascript:finishEPerson();"> --%>
                	<fmt:message key="jsp.dspace-admin.eperson-main.then"/>&nbsp;<input type="submit" name="submit_edit" value="<fmt:message key="jsp.dspace-admin.general.edit"/>" onclick="javascript:finishEPerson();"/>
                </td>
                <td>
                	<%-- <input type="submit" name="submit_delete" value="Delete..." onclick="javascript:finishEPerson();"> --%>
                	<input type="submit" name="submit_delete" value="<fmt:message key="jsp.dspace-admin.general.delete-w-confirm"/>" onclick="javascript:finishEPerson();"/>
                </td>
            </tr>
        </table>
    </center>        

    </form>
</dspace:layout>
