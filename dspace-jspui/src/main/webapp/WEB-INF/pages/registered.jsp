<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
           prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<dspace:layout titlekey="jsp.register.registered.title">

    <%-- <h1>Registration Complete</h1> --%>
    <h1><fmt:message key="jsp.register.registered.title"/></h1>

    <%-- <p>Thank you <%= Utils.addEntities(eperson.getFirstName()) %>,</p> --%>
    <p><fmt:message key="jsp.register.registered.thank">
        <fmt:param>${name}</fmt:param>
    </fmt:message></p>

    <%-- <p>You're now registered to use the DSpace system.  You can subscribe to
    collections to receive e-mail updates about new items.</p> --%>
    <p><fmt:message key="jsp.register.registered.info"/></p>

    <%-- <p><a href="<%= request.getContextPath() %>/">Return to DSpace Home</a></p> --%>
    <p><a href="<%= request.getContextPath() %>/"><fmt:message key="jsp.register.general.return-home"/></a></p>
</dspace:layout>
