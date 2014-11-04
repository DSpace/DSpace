<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@ page language="java" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.dspace.content.authority.AuthorityInfo" %>
<%@page import="org.dspace.content.authority.Choices"%>
<%@page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%
    List<String> authoritiesMd = (List<String>) request.getAttribute("authorities");
    Map<String, AuthorityInfo> infos = (Map<String, AuthorityInfo>) request.getAttribute("infos");
    String message = (String) request.getSession().getAttribute("authority.message");
    long numItems = (Long) request.getAttribute("numItems");
    
    String detail = request.getParameter("detail");
    
    String scopeParam;
	if (detail != null)
	{
	    scopeParam = "issued=";
	}
	else
	{
	    scopeParam = "authority=";
	}
%>


<dspace:layout locbar="link" navbar="admin" titlekey="jsp.dspace-admin.authority"  parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

<h1><fmt:message key="jsp.dspace-admin.authority"/>      	
<!--  <small><a target="_blank"
				href='<%=LocaleSupport.getLocalizedMessage(pageContext,
                                "help.site-admin")%>'><fmt:message
				key="jsp.help" /></a></small>-->
				 </h1>   
       

<% if (message != null){ %>
    <div id="authority-message"><%= message %></div>
<%
    }
    request.getSession().removeAttribute("authority.message");

    if (detail == null)
    {
%>
		<fmt:message key="jsp.dspace-admin.authority.general-description">
			<fmt:param><%= request.getContextPath() %>/dspace-admin/authority?detail=true</fmt:param>
		</fmt:message>
<%
    }
    else
    {
%>
		<fmt:message key="jsp.dspace-admin.authority.detail-description">
			<fmt:param><%= request.getContextPath() %>/dspace-admin/authority</fmt:param>
		</fmt:message>
<%        
    }
    
    for (String md : authoritiesMd)
    {
        AuthorityInfo info = infos.get(md);
        String type = md;
        if(detail==null) {
        	type = md.substring(0,(md.length()-9));
        }
%>
<h3><fmt:message key="jsp.dspace-admin.authority-heading">
        <fmt:param value="<%= md %>"/>
    </fmt:message></h3>    
    <table class="authority-statistics table table-striped">
        <thead>
            <tr class="info">
                <th><fmt:message key="jsp.dspace-admin.authority-statistics.label" /></th>
                <th><fmt:message key="jsp.dspace-admin.authority-statistics.value" /></th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td scope="row"><fmt:message key="jsp.dspace-admin.authority-statistics.numTotMetadata-level"><fmt:param><%= type %></fmt:param></fmt:message></td>
                <td>
                    <table>
                        <thead>
                            <tr>
                                
                                <% int numAuthMet = 0;
                                	String icon = "";
                                    for (int i = 0; i < 7; i++){ numAuthMet += info.getNumMetadataWithKey()[i]; 
                                    	if(Choices.getConfidenceText(i*100).equalsIgnoreCase("novalue") ){
                                    		icon="bug";
                                    	}else if(Choices.getConfidenceText(i*100).equalsIgnoreCase("rejected")){
                                    		icon="ban";
                                    	}else if(Choices.getConfidenceText(i*100).equalsIgnoreCase("failed")){
                                    		icon="warning";
                                    	}else if(Choices.getConfidenceText(i*100).equalsIgnoreCase("notfound")){
                                    		icon="times-circle";
                                    	}else if(Choices.getConfidenceText(i*100).equalsIgnoreCase("ambiguous")){
                                    		icon="question-circle";
                                    	}else if(Choices.getConfidenceText(i*100).equalsIgnoreCase("uncertain")){
                                    		icon="gear";
                                    	}else if(Choices.getConfidenceText(i*100).equalsIgnoreCase("accepted")){
                                    		icon="check-square";
                                    	}
                                    %>
                                    
                                <th><span title="<fmt:message key="<%= \"jsp.common.authority-level\" + i %>" />" class="fa fa-<%= icon%>">&nbsp;&nbsp;&nbsp;</span></th>
                                <% }%>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>                                
                                <% for (int i = 0; i < 7; i++){ %>
                                <td><%= info.getNumMetadataWithKey()[i] %></td>
                                <% }%>
                            </tr>
                        </tbody>
                    </table>
                    <% if (info.getNumAuthorityIssued() > 0) { %>
                        <a class="fakebutton" href="?<%= scopeParam + info.getScope() %>" ><fmt:message key="jsp.dspace-admin.authority-statistics.numAuthorityIssued-fixit"/></a>
                    <% } %>
                </td>
            </tr>
            <tr>
                <td scope="row"><fmt:message key="jsp.dspace-admin.authority-statistics.numTotMetadata"><fmt:param><%= type %></fmt:param></fmt:message>
                <td><%= info.getNumTotMetadata() %></td>
            </tr>
            <tr>
                <td scope="row"><fmt:message key="jsp.dspace-admin.authority-statistics.numAuthorityKey"><fmt:param><%= type %></fmt:param></fmt:message></td>
                <td><%= info.getNumAuthorityKey() %></td>
            </tr>
            <tr>
                <td scope="row"><fmt:message key="jsp.dspace-admin.authority-statistics.numAuthorityIssued"><fmt:param><%= type %></fmt:param></fmt:message></td>
                <td><%= info.getNumAuthorityIssued() %></td>
            </tr>
            <tr>
                <td scope="row"><fmt:message key="jsp.dspace-admin.authority-statistics.numItems"><fmt:param><%= type %></fmt:param></fmt:message></td>
                <td><%= info.getNumItems() %></td>
            </tr>
            <tr>
                <td scope="row"><fmt:message key="jsp.dspace-admin.authority-statistics.numIssuedItems"><fmt:param><%= type %></fmt:param></fmt:message></td>
                <td><%= info.getNumIssuedItems() %></td>
            </tr>

        </tbody>
    </table>
<% } %>
	<div>
	<h3><fmt:message key="jsp.dspace-admin.authority-heading.info"/></h3>  
	<table>
 		<tr>
                <td scope="row"><fmt:message key="jsp.dspace-admin.authority-statistics.numTotalItems" /></td>
                <td><%= numItems %></td>
           </tr>
        
    </table>
    </div>
</dspace:layout>
