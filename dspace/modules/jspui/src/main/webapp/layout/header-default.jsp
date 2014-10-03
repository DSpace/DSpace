<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - HTML header for main home page
  --%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="java.util.List"%>
<%@ page import="java.util.Enumeration"%>
<%@ page import="org.dspace.app.webui.util.JSPManager" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.util.Util" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="javax.servlet.jsp.jstl.core.*" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>

<%
    // Get the DSpace Context
    Context context = UIUtil.obtainContext((HttpServletRequest)request);

    String title = (String) request.getAttribute("dspace.layout.title");
    String navbar = (String) request.getAttribute("dspace.layout.navbar");
    boolean locbar = ((Boolean) request.getAttribute("dspace.layout.locbar")).booleanValue();

    String siteName = ConfigurationManager.getProperty("dspace.name");
    String feedRef = (String)request.getAttribute("dspace.layout.feedref");
    boolean osLink = ConfigurationManager.getBooleanProperty("websvc.opensearch.autolink");
    String osCtx = ConfigurationManager.getProperty("websvc.opensearch.svccontext");
    String osName = ConfigurationManager.getProperty("websvc.opensearch.shortname");
    List parts = (List)request.getAttribute("dspace.layout.linkparts");
    String extraHeadData = (String)request.getAttribute("dspace.layout.head");
    String dsVersion = Util.getSourceVersion();
    String generator = dsVersion == null ? "DSpace" : "DSpace "+dsVersion;
%>

<!DOCTYPE html>
<html>
    <head>
        <title><%= siteName %>: <%= title %></title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="Generator" content="<%= generator %>" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link rel="shortcut icon" href="<%= request.getContextPath() %>/favicon.ico" type="image/x-icon"/>
	    <link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/jquery-ui-1.10.3.custom/redmond/jquery-ui-1.10.3.custom.css" type="text/css" />
	    <link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/bootstrap/bootstrap.min.css" type="text/css" />
	    <link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/bootstrap/bootstrap-theme.min.css" type="text/css" />
	    <link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/bootstrap/dspace-theme.css" type="text/css" />


        <link rel="stylesheet" href="<%= request.getContextPath() %>/styles.css" type="text/css" />
        <link rel="stylesheet" href="<%= request.getContextPath() %>/print.css" media="print" type="text/css" />
<%
    if (!"NONE".equals(feedRef))
    {
        for (int i = 0; i < parts.size(); i+= 3)
        {
%>
        <link rel="alternate" type="application/<%= (String)parts.get(i) %>" title="<%= (String)parts.get(i+1) %>" href="<%= request.getContextPath() %>/feed/<%= (String)parts.get(i+2) %>/<%= feedRef %>"/>
<%
        }
    }

    if (osLink)
    {
%>
        <link rel="search" type="application/opensearchdescription+xml" href="<%= request.getContextPath() %>/<%= osCtx %>description.xml" title="<%= osName %>"/>
<%
    }

    if (extraHeadData != null)
        { %>
<%= extraHeadData %>
<%
        }
%>


	<script type='text/javascript' src="<%= request.getContextPath() %>/static/js/jquery/jquery-1.10.2.min.js"></script>
	<script type='text/javascript' src='<%= request.getContextPath() %>/static/js/jquery/jquery-ui-1.10.3.custom.min.js'></script>
	<script type='text/javascript' src='<%= request.getContextPath() %>/static/js/bootstrap/bootstrap.min.js'></script>
	<script type='text/javascript' src='<%= request.getContextPath() %>/static/js/holder.js'></script>

    <script type="text/javascript" src="<%= request.getContextPath() %>/utils.js"></script>
    <script type="text/javascript" src="<%= request.getContextPath() %>/static/js/scriptaculous/prototype.js"> </script>
    <script type="text/javascript" src="<%= request.getContextPath() %>/static/js/scriptaculous/effects.js"> </script>
    <script type="text/javascript" src="<%= request.getContextPath() %>/static/js/scriptaculous/builder.js"> </script>
    <script type="text/javascript" src="<%= request.getContextPath() %>/static/js/scriptaculous/controls.js"> </script>
    <script type="text/javascript" src="<%= request.getContextPath() %>/static/js/choice-support.js"> </script>


<!-- HTML5 shim and Respond.js IE8 support of HTML5 elements and media queries -->
<!--[if lt IE 9]>
  <script src="<%= request.getContextPath() %>/static/js/html5shiv.js"></script>
  <script src="<%= request.getContextPath() %>/static/js/respond.min.js"></script>
<![endif]-->
    </head>

    <%-- HACK: leftmargin, topmargin: for non-CSS compliant Microsoft IE browser --%>
    <%-- HACK: marginwidth, marginheight: for non-CSS compliant Netscape browser --%>
    <body>
     <div class="container-drum">
        <%-- DSpace top-of-page banner --%>
        <div class="row pageBanner">

          <%-- University Libraries logo --%>
	      <div class="col-lg-2 col-md-3 col-sm-4 col-xs-4" id="lib-logo">
            <a href="http://www.lib.umd.edu"><img src="http://www.lib.umd.edu/images/wrapper/liblogo.png" class="img-responsive" alt="<fmt:message key="jsp.layout.header-default.alt"/>"/></a>
     	  </div>

          <%-- DRUM logo --%>
     	  <div class="col-lg-4 col-md-4 col-sm-3 col-xs-6" id="drum-logo">
            <a href="<%= request.getContextPath() %>/"><img src="<%= request.getContextPath() %>/image/drum-logo.png" class="img-responsive" alt="<fmt:message key="jsp.layout.header-default.alt"/>"
            height="55" width="271" border="0"/></a>
          </div>

          <%-- Search box and Login/Logout --%>
          <div id="pageBanner-right">
            <%-- Display this div when not 'xs' --%>
            <div class="pull-right hidden-xs">
              <dspace:include page="/components/page-banner-right.jsp"></dspace:include>
            </div>

            <%-- Display this div when 'xs' --%>
            <div class="visible-xs">
              <dspace:include page="/components/page-banner-right.jsp"></dspace:include>
            </div>
        </div>

        <%-- Localization --%>
<%--  <c:if test="${param.locale != null}">--%>
<%--   <fmt:setLocale value="${param.locale}" scope="session" /> --%>
<%-- </c:if> --%>
<%--        <fmt:setBundle basename="Messages" scope="session"/> --%>

        <%-- Page contents --%>

        <%-- HACK: width, border, cellspacing, cellpadding: for non-CSS compliant Netscape, Mozilla browsers --%>
        <table class="centralPane" width="100%" border="0" cellpadding="3" cellspacing="1">

            <%-- HACK: valign: for non-CSS compliant Netscape browser --%>
            <tr valign="top">

            <%-- Navigation bar --%>
<%
    if (!navbar.equals("off"))
    {
%>
            <td class="navigationBar">
                <dspace:include page="<%= navbar %>" />
            </td>
<%
    }
%>
            <%-- Page Content --%>

            <%-- HACK: width specified here for non-CSS compliant Netscape 4.x --%>
            <%-- HACK: Width shouldn't really be 100%, but omitting this means --%>
            <%--       navigation bar gets far too wide on certain pages --%>
            <td class="pageContents">

                <%-- Location bar --%>
<%
    if (locbar)
    {
%>
                <dspace:include page="/layout/location-bar.jsp" />
<%
    }
%>

</div>
