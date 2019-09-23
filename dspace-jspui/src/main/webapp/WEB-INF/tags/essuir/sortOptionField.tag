<%@ tag body-content="empty" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%@ attribute name="sortOptions" rtexprvalue="true" required="true" type="java.util.Set"
              description="Sort options list" %>

<%@ attribute name="sortedBy" rtexprvalue="true" required="true" type="org.dspace.sort.SortOption"
              description="Sorted by" %>

<div class="form-group row">
    <label for="sort_by" class="col-sm-6 col-form-label"><fmt:message
            key="browse.full.sort-by"/></label>
    <div class="col-sm-6">
        <select class="form-control" id="sort_by" name="sort_by">
            <c:forEach items="${sortOptions}" var="sortOption">
                <c:choose>
                    <c:when test="${sortOption.name.equals(sortedBy.name)}">
                        <option value="${sortOption.number}" selected="selected">
                            <fmt:message key="browse.sort-by.${sortOption.name}"/>
                        </option>
                    </c:when>
                    <c:otherwise>
                        <option value="${sortOption.number}">
                            <fmt:message key="browse.sort-by.${sortOption.name}"/>
                        </option>
                    </c:otherwise>
                </c:choose>
            </c:forEach>
        </select>
    </div>
</div>