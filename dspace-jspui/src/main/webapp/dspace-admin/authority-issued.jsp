<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@ page language="java" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.lang.Long" %>
<%@ page import="org.dspace.app.webui.servlet.admin.AuthorityManagementServlet" %>
<%@page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%
    List<String[]> authoritiesIssued = (List<String[]>) request.getAttribute("authoritiesIssued");
    String message = (String) request.getSession().getAttribute("authority.message");
    Long totAuthoritiesIssued = (Long) request.getAttribute("totAuthoritiesIssued");
    long longTotAuthoritiesIssued = totAuthoritiesIssued != null?totAuthoritiesIssued.longValue():0;
    Integer currPage = (Integer) request.getAttribute("currPage");
    int intCurrPage = currPage  != null?currPage.intValue():0;
    
    String authority = request.getParameter("authority");
    String issued = request.getParameter("issued");
    
    String scopeParam;
	if (authority != null)
	{
	    scopeParam = "authority="+authority;
	}
	else
	{
	    scopeParam = "issued="+issued;
	}
%>


<dspace:layout locbar="link" navbar="admin" titlekey="jsp.dspace-admin.authority">

<table width="95%">
    <tr>      
      <td align="left"><h1><fmt:message key="jsp.dspace-admin.authority-issued">
        <fmt:param value="<%= authority != null?authority:issued %>"/>
    </fmt:message></h1>   
      </td>
      <td align="right" class="standard">
      	<a target="_blank"
				href='<%=LocaleSupport.getLocalizedMessage(pageContext,
                                "help.site-admin.authority-issued")%>'><fmt:message
				key="jsp.help" /></a>        
      </td>
    </tr>
</table>

<% if (message != null){ %>
    <div id="authority-message"><%= message %></div>
<%
    }
    request.getSession().removeAttribute("authority.message");
%>

    <table>
    <tr>
    <th>
    	<fmt:message key="jsp.dspace-admin.authority-key.key" />
    </th>
    <th>
    	<fmt:message key="jsp.dspace-admin.authority-key.label" />
    </th>
    </tr>
<%
    for (String[] authKey : authoritiesIssued)
    {       
%>
        <tr>       
            <td><a href="?<%= scopeParam %>&key=<%= authKey[0] %>"><%= authKey[0] %></a></td>
            <td><%= authKey[1] %></td>
        </tr>
<% } %>
        
    </table>
<% if (longTotAuthoritiesIssued >
                AuthorityManagementServlet.AUTHORITY_KEYS_LIMIT) {
        long lastPage = longTotAuthoritiesIssued/AuthorityManagementServlet.AUTHORITY_KEYS_LIMIT;
%>
    <div class="authority-page-nav-link">
<%
        for (int idx = 0; idx <= lastPage; idx++)
        {
			if(currPage!=idx){%><a href="?<%= scopeParam %>&page=<%= idx %>"><% } %><%= idx+1 %><% if(currPage!=idx){%></a><% }%>&nbsp;<%
		}
%>
    </div>
<%
    }
%>

</dspace:layout>
