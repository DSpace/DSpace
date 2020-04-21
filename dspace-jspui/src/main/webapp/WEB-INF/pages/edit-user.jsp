<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.eperson.Group" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
           prefix="fmt" %>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@taglib prefix="essuir" tagdir="/WEB-INF/tags/essuir" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<dspace:layout style="submission" titlekey="jsp.dspace-admin.eperson-main.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">
    <h1><fmt:message key="jsp.dspace-admin.eperson-edit.heading">
        <fmt:param>${email}</fmt:param>
    </fmt:message>
        <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#epeople\"%>"><fmt:message key="jsp.help"/></dspace:popup>
    </h1>
    <c:if test="${emailExists}">
        <p class="alert alert-warning">
            <fmt:message key="jsp.dspace-admin.eperson-edit.emailexists"/>
        </p>
    </c:if>
    <form class="form-horizontal" action="<%= request.getContextPath() %>/dspace-admin/edit-epeople" method="post">
        <div class="form-group">
            <label class="col-md-offset-3 col-md-2 control-label" for="email"><fmt:message
                    key="jsp.dspace-admin.eperson-edit.email"/></label>
            <div class="col-md-3">
                <input class="form-control" type="text" name="email" id="email" size="40" value="${email}"/>
            </div>
        </div>

        <essuir:profilePage language="${language}" chair="${chair}" facultyList="${facultyList}" chairListJson="${chairListJson}" isRegisterPage="false" isEditUserPage="true"/>

        <div class="col-md-4 btn-group">
            <input type="hidden" name="eperson_id" value="${epersonId}"/>
            <input class="btn btn-default" type="submit" name="submit_save" value="<fmt:message key="jsp.dspace-admin.general.save"/>" />
            <c:if test="${not isNewUser}">
                <input class="btn btn-default" type="submit" name="submit_resetpassword" value="<fmt:message key="jsp.dspace-admin.eperson-main.ResetPassword.submit"/>"/>
                <input class="btn btn-danger" type="submit" name="submit_delete" value="<fmt:message key="jsp.dspace-admin.general.delete"/>" />
            </c:if>
        </div>
    </form>

    <c:if test="${not empty groupMemberships}">
        <br/>
        <br/>

        <h3><fmt:message key="jsp.dspace-admin.eperson-edit.groups"/></h3>

        <div class="row">
            <ul>
                <c:forEach items="${groupMemberships}" var="group">
                    <li><a href = "/tools/group-edit?submit_edit&amp;group_id=${group.ID}">${group.name}</a></li>
                </c:forEach>
            </ul>
        </div>
    </c:if>
</dspace:layout>