<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ page import="java.util.stream.Stream" %>
<link rel="stylesheet" type="text/css" media="screen" href="<%= request.getContextPath() %>/static/webix/webix.css"/>
<link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/bootstrap/bootstrap.min.css"
      type="text/css"/>
<script src="<%= request.getContextPath() %>/static/webix/webix.js" type="text/javascript"></script>
<script type='text/javascript' src="<%= request.getContextPath() %>/static/js/jquery/jquery-1.10.2.min.js"></script>
<script type='text/javascript'
        src="<%= request.getContextPath() %>/static/js/jquery/jquery-ui-1.10.3.custom.min.js"></script>
<script type='text/javascript'
        src="<%= request.getContextPath() %>/static/js/jquery/jquery.ui.datepicker-ru.js"></script>
<style>
    .ui-datepicker .ui-datepicker-title select {
        color: #000 !important;
    }
</style>


<dspace:layout locbar="nolink" title="Statistics" feedData="NONE">
    <h2>${depositor}</h2>
    <ul class="list-group">
        <c:forEach items="${data}" var="item">
            <li class="list-group-item">${item}</li>
        </c:forEach>
    </ul>
</dspace:layout>
