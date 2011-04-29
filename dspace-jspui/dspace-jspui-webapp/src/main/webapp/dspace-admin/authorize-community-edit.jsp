<%--
  - authorize_community_edit.jsp
  -
  - $Id: authorize-community-edit.jsp 4309 2009-09-30 19:20:07Z bollini $
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
  - Show policies for a community, allowing you to modify, delete
  -  or add to them
  -
  - Attributes:
  -  community - Community being modified
  -  policies - ResourcePolicy [] of policies for the community
  - Returns:
  -  submit value community_addpolicy    to add a policy
  -  submit value community_editpolicy   to edit policy
  -  submit value community_deletepolicy to delete policy
  -
  -  policy_id - ID of policy to edit, delete
  -
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>


<%@ page import="java.util.List"     %>
<%@ page import="java.util.Iterator" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.authorize.ResourcePolicy" %>
<%@ page import="org.dspace.content.Community"        %>
<%@ page import="org.dspace.core.Constants"           %>
<%@ page import="org.dspace.eperson.EPerson"          %>
<%@ page import="org.dspace.eperson.Group"            %>


<%
    Community community = (Community) request.getAttribute("community");
    List policies =
        (List) request.getAttribute("policies");
%>

<dspace:layout titlekey="jsp.dspace-admin.authorize-community-edit.title"
               navbar="admin"
               locbar="link"
               parenttitle="general.administer"
               parentlink="/dspace-admin"
               nocache="true">
  <table width="95%">
    <tr>
      <td align="left">
	<h1><fmt:message key="jsp.dspace-admin.authorize-community-edit.policies">
        <fmt:param><%= community.getMetadata("name") %></fmt:param>
        <fmt:param>hdl:<%= community.getHandle() %></fmt:param>
        <fmt:param><%=community.getID()%></fmt:param>
    </fmt:message></h1>
      </td>
      <td align="right" class="standard">
        <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#communitypolicies\"%>"><fmt:message key="jsp.help"/></dspace:popup>
      </td>
    </tr>
  </table>

  <form action="<%= request.getContextPath() %>/tools/authorize" method="post">
    <p align="center">
            <input type="hidden" name="community_id" value="<%=community.getID()%>" />
            <input type="submit" name="submit_community_add_policy" value="<fmt:message key="jsp.dspace-admin.general.addpolicy"/>" />
    </p>
  </form>

    <table class="miscTable" align="center" summary="Community Policy Edit Form">
        <tr>

            <th id="t1" class="oddRowOddCol"><strong><fmt:message key="jsp.general.id" /></strong></th>
            <th id="t2" class="oddRowEvenCol"><strong><fmt:message key="jsp.dspace-admin.general.action"/></strong></th>
            <th id="t3" class="oddRowOddCol"><strong><fmt:message key="jsp.dspace-admin.general.group"/></strong></th>
            <th id="t4" class="oddRowEvenCol">&nbsp;</th>
            <th id="t5" class="oddRowOddCol">&nbsp;</th>
        </tr>

<%
    String row = "even";
    Iterator i = policies.iterator();

    while( i.hasNext() )
    {
        ResourcePolicy rp = (ResourcePolicy) i.next();
%>
        <tr>
            <td headers="t1" class="<%= row %>RowOddCol"><%= rp.getID() %></td>
            <td headers="t2" class="<%= row %>RowEvenCol">
                    <%= rp.getActionText() %>
            </td>
            <td headers="t3" class="<%= row %>RowOddCol">
                    <%= (rp.getGroup()   == null ? "..." : rp.getGroup().getName() ) %>  
             </td>
             <td headers="t4" class="<%= row %>RowEvenCol">
                <form action="<%= request.getContextPath() %>/tools/authorize" method="post">
                    <input type="hidden" name="policy_id" value="<%= rp.getID() %>" />
                    <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                    <input type="submit" name="submit_community_edit_policy" value="<fmt:message key="jsp.dspace-admin.general.edit"/>" />
                </form>
             </td>
             <td headers="t5" class="<%= row %>RowOddCol">
                <form action="<%= request.getContextPath() %>/tools/authorize" method="post">
                    <input type="hidden" name="policy_id" value="<%= rp.getID() %>" />
                    <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                    <input type="submit" name="submit_community_delete_policy" value="<fmt:message key="jsp.dspace-admin.general.delete"/>" />
                </form>
             </td>
         </tr>

<%
        row = (row.equals("odd") ? "even" : "odd");
    }
%>
    </table>
</dspace:layout>
