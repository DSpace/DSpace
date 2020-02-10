<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<dspace:layout locbar="nolink" title="Statistics" feedData="NONE">
    <form class="form-inline" action="" method="get">
        <div class="form-group">
            <label class="sr-only" for="limit">Limit: </label>

            <div class="input-group">
                <select class="form-control" id="limit" name="limit">

                    <c:forEach var="index" begin="10" end="100" step="10">
                        <c:set var="selected" value=""/>
                        <c:choose>
                            <c:when test="${index == limit}">
                                <c:set var="selected" value="selected"/>
                            </c:when>

                        </c:choose>
                        <option ${selected}>${index}</option>
                    </c:forEach>
                </select>
            </div>
        </div>
        <button type="submit" class="btn btn-primary"><fmt:message key="jsp.search.yearslider.button"/></button>
    </form>

    <table class="table">
        <thead>
        <tr>
            <th class="evenRowEvenCol"><fmt:message key="jsp.admin.person-stat.name"/></th>
            <th class="evenRowEvenCol"><fmt:message key="jsp.admin.person-stat.email"/></th>
            <th class="evenRowEvenCol"><fmt:message key="jsp.admin.person-stat.faculty"/></th>
            <th class="evenRowEvenCol"><fmt:message key="jsp.admin.person-stat.chair"/></th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${users}" var="user">
            <tr>
                <td class="evenRowOddCol">${user.lastName} ${user.firstName}</td>
                <td class="evenRowOddCol">${user.email}</td>
                <td class="evenRowOddCol">${user.chair.facultyEntity.name}</td>
                <td class="evenRowOddCol">${user.chair.name}</td>
            </tr>

        </c:forEach>


        </tbody>
    </table>
</dspace:layout>
