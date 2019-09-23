<%@ tag import="org.dspace.browse.ItemCounter" %>
<%@ tag import="org.dspace.app.webui.util.UIUtil" %>
<%@ tag body-content="empty" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%@ attribute name="community" rtexprvalue="true" required="true" type="org.dspace.content.Community"
              description="Community to display" %>
<%@ attribute name="itemCounter" rtexprvalue="true" required="true" type="org.dspace.browse.ItemCounter"
              description="Item counter" %>

<li>
    <span class="badge"><i class="icon-minus-sign" style = "font-size:22px;"></i></span>
    <a href="handle/${community.handle}">${community.name}</a>
    <span class="badge"> ${itemCounter.getCount(community)}</span>
    <ul>
    <c:forEach items="${community.collections}" var="collection">
        <li>
            <a href="handle/${collection.handle}">${collection.name}</a>
            <span class="badge"> ${itemCounter.getCount(collection)}</span>
        </li>
    </c:forEach>
    </ul>
</li>