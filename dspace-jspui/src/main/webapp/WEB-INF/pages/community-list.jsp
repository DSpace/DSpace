<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="C" uri="http://java.sun.com/jsp/jstl/core" %>

<%@taglib prefix="essuir" tagdir="/WEB-INF/tags/essuir"%>

<dspace:layout locbar="commLink" titlekey="jsp.community-list.title" feedData="NONE">
    <c:if test="${isAdmin}">
        <dspace:sidebar>
            <div class="panel panel-warning">
            <div class="panel-heading">
                <fmt:message key="jsp.admintools"/>
                <span class="pull-right">
                        <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\")%>"><fmt:message key="jsp.adminhelp"/></dspace:popup>
                    </span>
            </div>
            <div class="panel-body">
                <form method="post" action="<%=request.getContextPath()%>/dspace-admin/edit-communities">
                    <input type="hidden" name="action" value="<%=EditCommunitiesServlet.START_CREATE_COMMUNITY%>" />
                    <input class="btn btn-default" type="submit" name="submit" value="<fmt:message key="jsp.community-list.create.button"/>" />
                </form>
            </div>
            </div>
        </dspace:sidebar>
    </c:if>

    <h1><fmt:message key="jsp.community-list.title"/></h1>
    <p><fmt:message key="jsp.community-list.text1"/></p>
    <div class = "tree well">
        <ul>
        <c:forEach items="${communities}" var="community">

            <essuir:displayCommunity community="${community}" itemCounter="${itemCounter}"/>
            <li>
                <ul>
                    <c:forEach items="${innerCommunities.get(community.ID.toString())}" var="inner">
                        <essuir:displayCommunity community="${inner}" itemCounter="${itemCounter}"/>
                    </c:forEach>
                </ul>
            </li>
        </c:forEach>
        </ul>
    </div>
</dspace:layout>
<%
    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request);
    context.complete();
%>