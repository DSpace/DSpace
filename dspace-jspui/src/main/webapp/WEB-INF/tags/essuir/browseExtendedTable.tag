<%@ tag body-content="empty" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%@ attribute name="items" rtexprvalue="true" required="true" type="java.util.List"
              description="Items list" %>


<table align="center" class="table" summary="This table browses all dspace content">
    <colgroup><col width="130"><col width="60%"><col width="40%"></colgroup>
    <thead>
    <tr>
        <th id="t1" class="oddRowEvenCol"><strong><fmt:message key="itemlist.dc.date.issued"/> </strong></th>
        <th id="t2" class="oddRowOddCol"><fmt:message key="itemlist.dc.title"/></th>
        <th id="t3" class="oddRowEvenCol"><fmt:message key="itemlist.dc.contributor.author"/></th>
        <th id="t4" class="oddRowOddCol"><fmt:message key="itemlist.dc.type"/></th>
        <th id="t5" class="oddRowEvenCol"><fmt:message key="jsp.statistics.heading.views"/></th>
        <th id="t6" class="oddRowOddCol"><fmt:message key="org.dspace.app.webui.jsptag.ItemTag.downloads"/></th>
    </tr>
    </thead>
    <tbody>
    <c:forEach items="${items}" var="item">
        <tr>
            <td align="right" class="oddRowEvenCol"><strong>${item.year}</strong></td>
            <td class="oddRowOddCol"><a href="/handle/${item.handle}">${item.title}</a></td>
            <td class="oddRowEvenCol">${item.authors}</td>
            <td class="oddRowOddCol">${item.type}</td>
            <td class="oddRowEvenCol">${item.views}</td>
            <td class="oddRowOddCol">${item.downloads}</td>
        </tr>
    </c:forEach>
    </tbody>
</table>