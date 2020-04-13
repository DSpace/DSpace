<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri='http://java.sun.com/jsp/jstl/core' prefix='c' %>
<%@taglib prefix="essuir" tagdir="/WEB-INF/tags/essuir" %>

<dspace:layout style="submission" titlekey="jsp.register.edit-profile.title" nocache="true">
    <c:if test="${missingFields}">
        <p class="alert alert-info"><fmt:message key="jsp.register.edit-profile.info1"/></p>
    </c:if>
    <c:if test="${passwordProblem}">
        <p class="alert alert-warning"><fmt:message key="jsp.register.edit-profile.info2"/></p>
    </c:if>

    <form class="form-horizontal" action="<%= request.getContextPath() %>/profile" method="post">
        <essuir:profilePage language="${language}" chair="${chair}" facultyList="${facultyList}" chairListJson="${chairListJson}" isRegisterPage="false"/>
    </form>
</dspace:layout>