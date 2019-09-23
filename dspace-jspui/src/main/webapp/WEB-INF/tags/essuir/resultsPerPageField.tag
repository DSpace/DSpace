<%@ tag body-content="empty" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%@ attribute name="rpp" rtexprvalue="true" required="true" type="java.lang.Integer"
              description="Sort order" %>

<div class="form-group row">
    <label for="rpp" class="col-sm-6 col-form-label"><fmt:message key="browse.full.rpp"/></label>
    <div class="col-sm-6">
        <select id = "rpp" name="rpp" class="form-control">
            <c:forEach begin="5" end="100" step="5" var="index">
                <option value="${index}"
                        <c:if test="${index == rpp}">
                            selected="selected"
                        </c:if>
                >${index}</option>
            </c:forEach>
        </select>
    </div>
</div>