<%--
  - report.jsp
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
  - Renders a page containing a statistical summary of the repository usage
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="java.util.Date" %>
<%@ page import="java.text.SimpleDateFormat" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
    String report = (String) request.getAttribute("report");
    Date[] months = (Date[]) request.getAttribute("months");
    String date = (String) request.getAttribute("date");
    Boolean general = (Boolean) request.getAttribute("general");
    String navbar = (String) request.getAttribute("navbar");
    
    SimpleDateFormat sdfDisplay = new SimpleDateFormat("MM'/'yyyy");
    SimpleDateFormat sdfLink = new SimpleDateFormat("yyyy'-'M");
%>

<dspace:layout navbar="<%= navbar %>" titlekey="jsp.statistics.report.title">

    <p>
<%
    if (general.booleanValue())
    {
%>
    <strong><fmt:message key="jsp.statistics.report.info1"/></strong>
<%
    }
    else
    {
%>
    <strong><a href="<%= request.getContextPath() %>/statistics"><fmt:message key="jsp.statistics.report.info1"/></a></strong>
    </p>
<%
    }
%>
    <p>
    <strong><fmt:message key="jsp.statistics.report.info2"/></strong>
<%
        for (int i = 0; i < months.length; i++)
        {
            if (sdfLink.format(months[i]).equals(date))
            {
%>
                <strong><%= sdfDisplay.format(months[i]) %></strong>
<%
            }
            else
            {
%>
            <a href="<%= request.getContextPath() %>/statistics?date=<%= sdfLink.format(months[i]) %>"><%= sdfDisplay.format(months[i]) %></a>
<%
            }
            
            if (i != months.length - 1)
            {
%>
                &nbsp;|&nbsp;
<%
            }
%>

    <%
        }
    %>
    </p>

    <hr />

    <%= report %>

</dspace:layout>
