<%--
  - FAQ page JSP
  --%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ page import="java.util.Locale"%>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>

<%
    org.dspace.core.Context context = org.dspace.app.webui.util.UIUtil.obtainContext(request);

    Locale sessionLocale = UIUtil.getSessionLocale(request);

    String locale = sessionLocale.toString();
    String faq = locale.equals("en") ? "faq.html" : "faq_" + locale + ".html";

    faq = ConfigurationManager.readNewsFile(faq);
%>

<dspace:layout locbar="commLink" titlekey="jsp.layout.navbar-default.faq">
     <%= faq %>
</dspace:layout>
