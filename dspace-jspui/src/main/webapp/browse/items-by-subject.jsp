<%--
  - items_by_subject.jsp
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
  - Display items by a particular subject
  -
  - Attributes to pass in:
  -
  -   community      - pass in if the scope of the browse is a community, or
  -                    a collection within this community
  -   collection     - pass in if the scope of the browse is a collection
  -   browse.info    - the BrowseInfo containing the items to display
  -   subject        - The name of the subject
  -   sort.by.date   - Boolean.  if true, we're sorting by date, otherwise by
  -                    title.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.net.URLEncoder" %>

<%@ page import="org.dspace.browse.BrowseInfo" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.core.Utils" %>

<%
    Collection collection = (Collection) request.getAttribute("collection");
    Community community = (Community) request.getAttribute("community");
    BrowseInfo browseInfo = (BrowseInfo) request.getAttribute("browse.info" );
    String subject = (String) request.getAttribute("subject");
    boolean orderByTitle = ((Boolean) request.getAttribute("order.by.title")).booleanValue();
%>

<dspace:layout titlekey="jsp.browse.items-by-subject.title">

  <h2><fmt:message key="jsp.browse.items-by-subject.heading1"/> "<%= Utils.addEntities(subject) %>"</h2>
    <%-- Sorting controls --%>
    <table border="0" cellpadding="10" align="center" summary="Browse the repository by subject">
        <tr>
            <td colspan="2" align="center" class="standard">
			    <a href="browse-subject?starts_with=<%= URLEncoder.encode(subject) %>"><fmt:message key="jsp.browse.items-by-subject.return"/></a>
            </td>
        </tr>
        <tr>
            <td class="standard">
<%
    if (orderByTitle)
    {
%>
                <strong><fmt:message key="jsp.browse.items-by-subject.sort1"/></strong>
            </td>
            <td class="standard">
				<a href="items-by-subject?subject=<%= URLEncoder.encode(subject) %>&amp;order=date"><fmt:message key="jsp.browse.items-by-subject.sort2"/></a>
<%
    }
    else
    {
%>
	            <a href="items-by-subject?subject=<%= URLEncoder.encode(subject) %>&amp;order=title"><fmt:message key="jsp.browse.items-by-subject.sort3"/></a>
            </td>
            <td class="standard">
				<strong><fmt:message key="jsp.browse.items-by-subject.sort4"/></strong>
<%
    }
%>
            </td>
        </tr>
    </table>

	<p align="center"><fmt:message key="jsp.browse.items-by-subject.show">

        <fmt:param><%= browseInfo.getResultCount() %></fmt:param>
    </fmt:message></p>


    <%-- The items --%>
<%
    String emphColumn = (orderByTitle ? "title" : "date");
%>
    <dspace:itemlist items="<%= browseInfo.getItemResults() %>" emphcolumn="<%= emphColumn %>" />


</dspace:layout>
