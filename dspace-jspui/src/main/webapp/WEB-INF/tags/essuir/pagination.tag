<%@ tag body-content="empty" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%@ attribute name="links" rtexprvalue="true" required="true" type="java.util.List"
              description="Links list" %>

<%@ attribute name="prevPageUrl" rtexprvalue="true" required="true" type="java.lang.String"
              description="Link to previous page" %>

<%@ attribute name="prevPageDisabled" rtexprvalue="true" required="true" type="java.lang.String"
              description="Style, which indicates that link to previous page must be disabled" %>

<%@ attribute name="nextPageUrl" rtexprvalue="true" required="true" type="java.lang.String"
              description="Link to next page" %>

<%@ attribute name="nextPageDisabled" rtexprvalue="true" required="true" type="java.lang.String"
              description="Style, which indicates that link to next page must be disabled" %>


<ul class="cd-pagination no-space move-buttons custom-icons">
    <li class="button">
        <a href="${prevPageUrl}" class = "${prevPageDisabled}"><fmt:message key="pagination.prev"/></a>
    </li>
    <c:forEach items="${links}" var="link">
        ${link}
    </c:forEach>

    <li class="button">
        <a href="${nextPageUrl}" class = "${nextPageDisabled}"><fmt:message key="pagination.next"/></a>
    </li>
</ul>