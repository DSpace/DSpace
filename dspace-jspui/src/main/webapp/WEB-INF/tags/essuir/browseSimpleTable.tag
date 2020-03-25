<%@ tag body-content="empty" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%@ attribute name="items" rtexprvalue="true" required="true" type="java.util.List"
              description="Items list" %>

<%@ attribute name="type" rtexprvalue="true" required="true" type="java.lang.String"
              description="Type of view" %>

<ul class="list-group">
    <c:forEach items="${items}" var="item">

        <li class="list-group-item"><a href="${requestUri}?type=${type}&value=${item.handle}">${item.title}</a>
            <span class="badge pull-right">${item.views}</span></li>
    </c:forEach>
</ul>