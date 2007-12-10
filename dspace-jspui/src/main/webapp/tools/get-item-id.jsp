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
  - Form requesting a URI or internal item ID for item editing
  -
  - Attributes:
  -     invalid.id  - if this attribute is present, display error msg
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.core.ConfigurationManager" %>

<dspace:layout titlekey="jsp.tools.get-item-id.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

	<%-- <h1>Edit or Delete Item</h1> --%>
	<h1><fmt:message key="jsp.tools.get-item-id.heading"/></h1>
    
<%
    if (request.getAttribute("invalid.id") != null) { %>
    <%-- <p><strong>The ID you entered isn't a valid item ID.</strong>  If you're trying to
    edit a community or collection, you need to use the --%>
    <%-- <a href="<%= request.getContextPath() %>/dspace-admin/edit-communities">communities/collections admin page.</a></p> --%>
	<p><fmt:message key="jsp.tools.get-item-id.info1">
        <fmt:param><%= request.getContextPath() %>/dspace-admin/edit-communities</fmt:param>
    </fmt:message></p>
<%  } %>


	<div><fmt:message key="jsp.tools.get-item-id.info2"/>  <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#items\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup></div>
    
    <form method="get" action="">
        <center>
            <table class="miscTable">
                <tr class="oddRowEvenCol">
					<td class="submitFormLabel"><label for="turi"><fmt:message key="jsp.tools.get-item-id.uri"/></label></td>
                      <td>
                            <input type="text" name="uri" id="turi" value="hdl:<%= ConfigurationManager.getProperty("handle.prefix") %>/" size="12"/>
                            <%-- <input type="submit" name="submit" value="Find" /> --%>
							<input type="submit" name="submit" value="<fmt:message key="jsp.tools.get-item-id.find.button"/>" />
                    </td>
                </tr>
                <tr><td></td></tr>
                <tr class="oddRowEvenCol">
                    <%-- <td class="submitFormLabel">Internal ID:</td> --%>
					<td class="submitFormLabel"><label for="titem_id"><fmt:message key="jsp.tools.get-item-id.internal"/></label></td>
                    <td>
                            <input type="text" name="item_id" id="titem_id" size="12"/>
                            <%-- <input type="submit" name="submit" value="Find"> --%>
							<input type="submit" name="submit" value="<fmt:message key="jsp.tools.get-item-id.find.button"/>" />
                    </td>
                </tr>
            </table>
        </center>
    </form>
</dspace:layout>
