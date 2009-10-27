<%--
  - should be put under $DSPACE_SRC_HOME/dspace-jspui/dspace-jspui-webapp/src/main/webapp/login
  - no-single-sign-out.jsp
  --%>

<%--
  - Display message indicating that the Shibboleth does not support single sign out yet
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<dspace:layout title="Single Sign Out feature is not implemented">
    <h1>Single Sign Out feature is not implemented</h1>

    <P>The protection provided by Shibboleth does not have single sign out feature implemented yet. Please simply close the browser and re-open it to clear cookie</P>
</dspace:layout>
