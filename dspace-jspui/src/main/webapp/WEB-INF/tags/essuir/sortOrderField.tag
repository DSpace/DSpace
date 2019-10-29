<%@ tag body-content="empty" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%@ attribute name="sortOrder" rtexprvalue="true" required="true" type="java.lang.String"
              description="Sort order" %>

<div class="form-group row">
    <label for="order" class="col-sm-6 col-form-label"><fmt:message key="browse.full.order"/></label>
    <div class="col-sm-6">
        <select id = "order" name="order" class="form-control">
            <option value="ASC"
                    <c:if test="${\"ASC\".equals(sortOrder.toUpperCase())}">
                        selected="selected"
                    </c:if>
            ><fmt:message key="browse.order.asc" /></option>

            <option value="DESC"
                    <c:if test="${\"DESC\".equals(sortOrder.toUpperCase())}">
                        selected="selected"
                    </c:if>
            ><fmt:message key="browse.order.desc" /></option>
        </select>
    </div>
</div>