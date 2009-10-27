<%--
  - ws-main.jsp
  -
  - Version: $Revision$
  -
  - Date: $Date$
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
  - Workspace Options page, so the user may edit, view, add notes to or remove
  - the workspace item
  -
  - Attributes:
  -    wsItem   - WorkspaceItem containing the current item to be worked on
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ page import="org.dspace.app.webui.servlet.MyDSpaceServlet" %>
<%@ page import="org.dspace.content.DCValue" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.content.WorkspaceItem" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%
    // get the workspace item from the request
    WorkspaceItem workspaceItem =
        (WorkspaceItem) request.getAttribute("wsItem");

    // get the title and submitter of the item
    DCValue[] titleArray =
         workspaceItem.getItem().getDC("title", null, Item.ANY);
//    String title = (titleArray.length > 0 ? titleArray[0].value : "Untitled");
    EPerson submitter = workspaceItem.getItem().getSubmitter();
%>

<dspace:layout locbar="link"
               parentlink="/mydspace"
               parenttitlekey="jsp.mydspace"
               titlekey="jsp.workspace.ws-main.title">

    <table width="100%" border="0">
        <tr>
            <td align="left">
                <h1>
                    <fmt:message key="jsp.workspace.ws-main.wsitem"/>
                </h1>
            </td>
            <td align="right" class="standard">
                <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") + \"#mydspace\"%>"><fmt:message key="jsp.help"/></dspace:popup>
            </td>
        </tr>
    </table>

<%--    <h2><%= title %></h2> --%>
<%
		if (titleArray.length > 0)
		{
%>
			<h2><%= titleArray[0].value %></h2>
<%
		}
		else
		{
%>
			<h2><fmt:message key="jsp.general.untitled"/></h2>
<%
		}
%>

    <p><strong><a href="mailto:<%= submitter.getEmail() %>"><%= submitter.getFullName() %></a></strong></p>

	<p><fmt:message key="jsp.workspace.ws-main.submitmsg"/> 
    <%= workspaceItem.getCollection().getMetadata("name") %></p>

    <table class="miscTable" align="center">
        <tr>
            <th class="oddRowOddCol"><fmt:message key="jsp.workspace.ws-main.optionheading"/></th>
            <th class="oddRowEvenCol"><fmt:message key="jsp.workspace.ws-main.descheading"/></th>
        </tr>
        <tr>
            <td class="evenRowOddCol" align="center">
                <form action="<%= request.getContextPath() %>/mydspace" method="post">
                    <input type="hidden" name="step" value="<%= MyDSpaceServlet.MAIN_PAGE %>"/>
                    <input type="hidden" name="workspace_id" value="<%= workspaceItem.getID() %>"/>
                    <input type="hidden" name="resume" value="<%= workspaceItem.getID() %>"/>
                    <input type="submit" name="submit_resume" value="<fmt:message key="jsp.workspace.ws-main.button.edit"/>"/>
                </form>
            </td>
            <td class="evenRowEvenCol">
                <fmt:message key="jsp.workspace.ws-main.editmsg"/>
            </td>
        </tr>
        
        <tr>
            <td class="oddRowOddCol" align="center">
                <form action="<%= request.getContextPath() %>/view-workspaceitem" method="post">
                   <input type="hidden" name="workspace_id" value="<%= workspaceItem.getID() %>"/>
                   <input type="submit" name="submit_view" value="<fmt:message key="jsp.workspace.ws-main.button.view"/>"/>
                </form>
            </td>
            <td class="oddRowEvenCol">
                <fmt:message key="jsp.workspace.ws-main.viewmsg"/>
            </td>
        </tr>
        
        <tr>
            <td class="evenRowOddCol" align="center">
                <form action="<%= request.getContextPath() %>/mydspace" method="post">
                    <input type="hidden" name="step" value="<%= MyDSpaceServlet.MAIN_PAGE %>"/>
                    <input type="hidden" name="workspace_id" value="<%= workspaceItem.getID() %>"/>
                    <input type="submit" name="submit_delete" value="<fmt:message key="jsp.workspace.ws-main.button.remove"/>"/>
                </form>
            </td>
            <td class="evenRowEvenCol">
                <fmt:message key="jsp.workspace.ws-main.removemsg"/>
            </td>
        </tr>

    </table>

<p><a href="<%= request.getContextPath() %>/mydspace"><fmt:message key="jsp.mydspace.general.returnto-mydspace"/></a></p>

</dspace:layout>
