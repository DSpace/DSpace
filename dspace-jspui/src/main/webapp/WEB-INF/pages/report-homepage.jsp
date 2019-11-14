<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<dspace:layout locbar="nolink" title="Statistics" feedData="NONE">
    <ul class="list-group">
        <li class="list-group-item"><a href="/report/general"><fmt:message key="report.statistics"/></a></li>
        <li class="list-group-item"><a href="/report/recent-person"><fmt:message key="report.newest-persons"/></a></li>
        <li class="list-group-item"><a href="/report/speciality"><fmt:message key="jsp.admin.person-stat.speciality-title"/></a></li>


    </ul>
</dspace:layout>