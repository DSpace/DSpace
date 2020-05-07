<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Home page JSP
  -
  - Attributes:
  -    communities - Community[] all communities in DSpace
  -    recent.submissions - RecetSubmissions
  --%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<%@ page import="java.util.Locale"%>
<%@ page import="javax.servlet.jsp.jstl.core.*" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.core.Context" %>

<%
    Locale sessionLocale = UIUtil.getSessionLocale(request);
    Config.set(request.getSession(), Config.FMT_LOCALE, sessionLocale);
%>

<dspace:layout locbar="nolink" titlekey="jsp.home.title">

	<table width="100%" style="margin-bottom:20px">
		<tr>
			<td class="jumbotron" width="75%">
				${topNews}
		</td>
		<td width="20px"/><td valign="top" class="jumbotron">
			<p align="center" style="margin-bottom:22px">
				<c:choose>
					<c:when test="${locale.language.equals(\"en\")}">
						<a href="http://sumdu.edu.ua/int/en"><img src="/image/sumdu_logo_en.png" width="158px"></a>
					</c:when>
					<c:when test="${locale.language.equals(\"uk\")}">
						<a href="http://sumdu.edu.ua/int/en"><img src="/image/sumdu_logo_ua.png" width="158px"></a>
					</c:when>
					<c:otherwise>
						<a href="http://sumdu.edu.ua/"><img src="/image/sumdu_logo_en.png" width="158px"></a>
					</c:otherwise>
				</c:choose>
			</p>
				${sideNews}
		</td></tr>
	</table>


	<div class="jumbotron">
		<h3><fmt:message key="jsp.home.type"/></h3>

		<table border="0" cellpadding="2" width="100%">
			<tr>
				<c:forEach items="${submissions}" var="submission" varStatus="listIterator">
				<td class="standard" width="25%">
					<a href="<%= request.getContextPath() %>/simple-search?query=&filtername=type&filtertype=equals&filterquery=${submission.searchQuery}&rpp=20&sort_by=dc.date.issued_dt&order=desc">${submission.title}</a>
					<span class="badge">${submission.count}</span>
				</td>
				<c:if test="${listIterator.index % 4 == 3}">
			</tr>
			<tr>
				</c:if>
				</c:forEach>
				<td></td><td></td><td></td>
			</tr>
		</table>
	</div>
	<div class="jumbotron">
		<table class="miscTable" width="100%" align="center">
			<tr>
				<td class="oddRowEvenCol">
					<h3><fmt:message key="jsp.home.com1"/></h3>

					<table border="0" cellpadding="2">
						<c:forEach items="${communities}" var="community">
							<tr>
								<td class="standard">
									<a href="<%= request.getContextPath() %>/handle/${community.key.handle}">${community.key.name}</a>
									<c:if test="${ConfigurationManager.getBooleanProperty(\"webui.strengths.show\")}">
										<span class="badge">${community.value}</span>
									</c:if>
								</td>
							</tr>
						</c:forEach>
					</table>
				</td>
			</tr>
		</table>
	</div>
</dspace:layout>

<%
	// Obtain DSpace context
	Context context = UIUtil.obtainContext(request);
	context.complete();
%>