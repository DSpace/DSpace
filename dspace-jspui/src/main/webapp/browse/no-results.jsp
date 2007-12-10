<%--
  - no-results.jsp
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
  - Message indicating that there are no entries in the browse index.
  -
  - Attributes required:
  -    community      - community the browse was in, or null
  -    collection     - collection the browse was in, or null
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.browse.BrowseInfo" %>
<%@ page  import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.content.Collection" %>

<%
    request.setAttribute("LanguageSwitch", "hide");

	String layoutNavbar = "default";
	if (request.getAttribute("browseWithdrawn") != null)
	{
	    layoutNavbar = "admin";
	}

	// get the BrowseInfo object
	BrowseInfo bi = (BrowseInfo) request.getAttribute("browse.info");

    // Retrieve community and collection if necessary
    Community community = null;
    Collection collection = null;

    if (bi.inCommunity())
    {
    	community = (Community) bi.getBrowseContainer();
    }
    
    if (bi.inCollection())
    {
    	collection = (Collection) bi.getBrowseContainer();
    }
    
    // FIXME: this is not using the i18n
    // Description of what the user is actually browsing, and where link back
    String linkText = LocaleSupport.getLocalizedMessage(pageContext, "jsp.general.home");
    String linkBack = request.getContextPath();

    if (collection != null)
    {
        linkText = collection.getMetadata("name");
        linkBack = collection.getIdentifier().getURL().toString();
    }
    else if (community != null)
    {
        linkText = community.getMetadata("name");
        linkBack = community.getIdentifier().getURL().toString();
    }
%>

<dspace:layout titlekey="browse.no-results.title" navbar="<%= layoutNavbar %>">

    <h1><fmt:message key="browse.no-results.title"/></h1>

<p>
    <%
	    if (collection != null)
	    {
   %>
            	<fmt:message key="browse.no-results.col">
                    <fmt:param><%= collection.getMetadata("name")%></fmt:param>
                </fmt:message>
   <%
	    }
	    else if (community != null)
	    {
   %>
   		<fmt:message key="browse.no-results.com">
            <fmt:param><%= community.getMetadata("name")%></fmt:param>
        </fmt:message>
   <%
 	    }
 	    else
 	    {
   %>
   		<fmt:message key="browse.no-results.genericScope"/>
   <%
   	    }
   %>
 </p>
   
    <p><a href="<%= linkBack %>"><%= linkText %></a></p>

    <%-- dump the results for debug (uncomment to enable) --%>
    <%--
	<!-- <%= bi.toString() %> -->
	--%>

</dspace:layout>
