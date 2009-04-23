<%--
  - should be put under $DSPACE_SRC_HOME/dspace-jspui/dspace-jspui-webapp/src/main/webapp/login
  - no-email.jsp
  --%>

<%--
  - Display message indicating that the Shibboleth target is not configured properly to release the "user" information
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<dspace:layout title="No User email is provided">
    <h1>User Email is required</h1>

    <P>Your SSO system is not configured properly to release user email info</P>
</dspace:layout>
