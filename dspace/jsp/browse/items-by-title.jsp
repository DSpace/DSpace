<%--
  - items-by-title.jsp
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
  - Display the results of browsing title index
  -
  - Attributes to pass in:
  -   community      - pass in if the scope of the browse is a community, or
  -                    a collection within this community
  -   collection     - pass in if the scope of the browse is a collection
  -   browse.info    - the BrowseInfo containing the items to display
  -   highlight      - Boolean.  If true, the focus point of the browse
  -                    is highlighted
  -   previous.query - The query string to pass to the servlet to get the
  -                    previous page of items
  -   next.query     - The query string to pass to the servlet to get the next
  -                    page of items
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.browse.BrowseInfo" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.DCDate" %>
<%@ page import="org.dspace.content.DCValue" %>
<%@ page import="org.dspace.content.Item" %>


<%
    // Get attributes
    Collection collection = (Collection) request.getAttribute("collection");
    Community community = (Community) request.getAttribute("community");

    BrowseInfo browseInfo = (BrowseInfo) request.getAttribute("browse.info");

    boolean highlight = ((Boolean) request.getAttribute("highlight")).booleanValue();

    String prevQuery = (String) request.getAttribute("previous.query");
    String nextQuery = (String) request.getAttribute("next.query");

%>

<dspace:layout titlekey="jsp.browse.items-by-title.title">

    <h2><fmt:message key="jsp.browse.items-by-title.title"/></h2>
    <%-- Title browse controls table --%>
    <form action="browse-title" method="get">
        <table align="center" border="0" bgcolor="#CCCCCC" cellpadding="0" summary="Browse the respository by title">
            <tr>
                <td>
                    <table border="0" bgcolor="#EEEEEE" cellpadding="2"> <%--allow for greek alphabet also--%>
                        <tr>
                            <td class="browseBar">
                             <%-- <span class="browseBarLabel">Jump&nbsp;to:&nbsp;</span> --%>
    							<span class="browseBarLabel"><fmt:message key="jsp.browse.items-by-title.jump"/></span>
                                 <a href="browse-title?starts_with=0">0-9</a>
<%
    for (char c = 'A'; c <= 'Z'; c++)
    {
%>
                                <a href="browse-title?starts_with=<%= c %>"><%= c %></a>
<%
    }
%>
                            </td>
                        </tr>
                        <tr>
                                <td class="browseBar" align="center">
                                    <%-- <span class="browseBarLabel">or enter first few letters:&nbsp;</span> --%>
    								<span class="browseBarLabel"><fmt:message key="jsp.browse.items-by-title.enter"/>&nbsp;</span>
                                    <%-- <input type="text" name="starts_with"/>&nbsp;<input type="submit" value="Go!"> --%>
    								<input type="text" name="starts_with"/>&nbsp;<input type="submit" value="<fmt:message key="jsp.browse.general.go"/>" />
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>
    </form>

    <br/>

    <p align="center">
        <%-- Showing items <%= browseInfo.getOverallPosition()+1 %>-<%= browseInfo.getOverallPosition()+browseInfo.getResultCount() %>
        of <%= browseInfo.getTotal() %>. --%>
		<fmt:message key="jsp.browse.items-by-title.show">
            <fmt:param><%= browseInfo.getOverallPosition()+1 %></fmt:param>
            <fmt:param><%= browseInfo.getOverallPosition()+browseInfo.getResultCount() %></fmt:param>
            <fmt:param><%= browseInfo.getTotal() %></fmt:param>
        </fmt:message>
    </p>

    <%-- Previous page/Next page --%>
    <table align="center" border="0" width="70%">
        <tr>
            <td class="standard" align="left">
<%
    if (prevQuery != null)
    {
%>
                <%-- <a href="browse-title?<%= prevQuery %>">Previous page</a> --%>
				<a href="browse-title?<%= prevQuery %>"><fmt:message key="jsp.browse.general.previous"/></a>

<%
    }
%>
            </td>
            <td class="standard" align="right">
<%
    if (nextQuery != null)
    {
%>
                <%-- <a href="browse-title?<%= nextQuery %>">Next page</a> --%>
				<a href="browse-title?<%= nextQuery %>"><fmt:message key="jsp.browse.general.next"/></a>

<%
    }
%>
            </td>
        </tr>
    </table>

<%
    String highlightAttribute = "-1";
    if (highlight)
    {
        highlightAttribute = String.valueOf(browseInfo.getOffset());
    }
%>
    <dspace:itemlist items="<%= browseInfo.getItemResults() %>" emphcolumn="title" highlightrow="<%= highlightAttribute %>" />

    <%-- Previous page/Next page --%>
    <table align="center" border="0" width="70%">
        <tr>
            <td class="standard" align="left">
<%
    if (prevQuery != null)
    {
%>
                <%-- <a href="browse-title?<%= prevQuery %>">Previous page</a> --%>
				<a href="browse-title?<%= prevQuery %>"><fmt:message key="jsp.browse.general.previous"/></a>

<%
    }
%>
            </td>
            <td class="standard" align="right">
<%
    if (nextQuery != null)
    {
%>
                <%-- <a href="browse-title?<%= nextQuery %>">Next page</a> --%>
				<a href="browse-title?<%= nextQuery %>"><fmt:message key="jsp.browse.general.next"/></a>

<%
    }
%>
            </td>
        </tr>
    </table>

</dspace:layout>
