<%--
  - home.jsp
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
  - Home page JSP
  -
  - Attributes:
  -    communities - Community[] all communities in DSpace
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.io.File" %>

<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.core.Constants" %>

<%
    Community[] communities = (Community[]) request.getAttribute("communities");

    String topNews = ConfigurationManager.readNewsFile(Constants.NEWS_TOP);
    String sideNews = ConfigurationManager.readNewsFile(Constants.NEWS_SIDE);

%>

<dspace:layout locbar="nolink" titlekey="jsp.home.title">

    <table class="miscTable" width="95%" align="center">
        <tr>
            <td class="oddRowEvenCol"><%= topNews %></td>
        </tr>
    </table>

    <br>

    <form action="<%= request.getContextPath() %>/simple-search" method=GET>
        <table class="miscTable" width="95%" align="center">
            <tr>
                <td class="oddRowEvenCol">
                  <H3><fmt:message key="jsp.home.search1"/></H3>
                      <P><fmt:message key="jsp.home.search2"/></P>
                      <P><input type=text name=query size=20>&nbsp;<input type=submit name=submit value="<fmt:message key="jsp.general.search.button"/>"></P>
                </td>
            </tr>
        </table>
    </form>

    <table class="miscTable" width="95%" align="center">
        <tr>
            <td class="oddRowEvenCol">
               <H3><fmt:message key="jsp.home.com1"/></H3>
                <P><fmt:message key="jsp.home.com2"/></P>
                <table border=0 cellpadding=2>
<%
    for (int i = 0; i < communities.length; i++)
    {
%>                  <tr>
                        <td class="standard">
                            <A HREF="<%= request.getContextPath() %>/handle/<%= communities[i].getHandle() %>"><%= communities[i].getMetadata("name") %></A>
<%
        if (ConfigurationManager.getBooleanProperty("webui.strengths.show"))
        {
%>
            [<%= communities[i].countItems() %>]
<%
        }

%>
                        </td>
                    </tr>
<%
    }
%>
                </table>
            </td>
        </tr>
    </table>

    <dspace:sidebar><%= sideNews %></dspace:sidebar>
</dspace:layout>
