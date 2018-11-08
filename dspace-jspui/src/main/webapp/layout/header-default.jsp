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
<%@ page import="java.util.Locale"%>
<%@ page import="java.util.Enumeration"%>
<%@ page import="org.dspace.app.webui.util.JSPManager" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.core.I18nUtil" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.app.util.Util" %>
<%@ page import="org.dspace.app.webui.util.LocaleUIHelper" %>
<%@ page import="javax.servlet.jsp.jstl.core.*" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%@ page import="org.apache.commons.lang.StringUtils"%>

<%
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
    String extraHeadDataLast = (String)request.getAttribute("dspace.layout.head.last");
    String dsVersion = Util.getSourceVersion();
    String generator = dsVersion == null ? "DSpace" : "DSpace "+dsVersion;
    String analyticsKey = ConfigurationManager.getProperty("jspui.google.analytics.key");

    boolean cookiesPolicyEnabled = ConfigurationManager.getBooleanProperty("cookies.policy.enabled", false);
    
    // get the locale languages
    Locale[] supportedLocales = I18nUtil.getSupportedLocales();
    Locale sessionLocale = UIUtil.getSessionLocale(request);
    boolean isRtl = StringUtils.isNotBlank(LocaleUIHelper.ifLtr(request, "","rtl"));    
    String resourceSyncBaseURL = ConfigurationManager.getProperty("resourcesync", "base-url");
%>

<!DOCTYPE html>
<html>
    <head>
        <title><%= title %> | <%= siteName %></title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
        <meta name="Generator" content="<%= generator %>" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0">		
        <link rel="resourcesync sitemap" href="<%= resourceSyncBaseURL %>/resourcesync.xml" type="application/xml"/>
        <link rel="shortcut icon" href="<%= request.getContextPath() %>/favicon.ico" type="image/x-icon"/>
	    <link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/jquery-ui-1.10.3.custom/redmond/jquery-ui-1.10.3.custom.css" type="text/css" />
	    <link href="<%= request.getContextPath() %>/css/researcher.css" type="text/css" rel="stylesheet" />
       <link href="<%= request.getContextPath() %>/css/jdyna.css" type="text/css" rel="stylesheet" />
	    <link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/bootstrap/bootstrap.min.css" type="text/css" />
	    <link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/bootstrap/bootstrap-theme.min.css" type="text/css" />
	    <link href="<%= request.getContextPath() %>/static/css/font-awesome/css/font-awesome.min.css" rel="stylesheet">
		<link href="<%= request.getContextPath() %>/static/css/jstree/themes/default/style.min.css" rel="stylesheet"/>
	    <link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/bootstrap/dspace-theme.css" type="text/css" />
	    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/orcid.css" type="text/css" />
	    <link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/static/css/dataTables.bootstrap.min.css"/>
		<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/static/css/buttons.bootstrap.min.css"/>
		<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/static/css/responsive.bootstrap.min.css"/>
		<link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/bootstrap/dspace-theme.css" type="text/css" />
		<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/css/bootstrap-datetimepicker.min.css" />
			<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/static/css/select.dataTables.min.css" />
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
        
   	<script type='text/javascript' src="<%= request.getContextPath() %>/static/js/jquery/jquery-1.11.3.min.js"></script>
	<script type='text/javascript' src='<%= request.getContextPath() %>/static/js/jquery/jquery-ui-1.11.4.min.js'></script>
	<script type="text/javascript" src="<%= request.getContextPath() %>/js/moment.js"></script>
	<script type='text/javascript' src='<%= request.getContextPath() %>/static/js/bootstrap/bootstrap.min.js'></script>
	<script type='text/javascript' src='<%= request.getContextPath() %>/static/js/holder.js'></script>
	<script type="text/javascript" src="<%= request.getContextPath() %>/utils.js"></script>
	<script type='text/javascript' src='<%= request.getContextPath() %>/static/js/custom-functions.js'></script>
    <script type="text/javascript" src="<%= request.getContextPath() %>/static/js/choice-support.js"> </script>
    <script type="text/javascript" src="<%= request.getContextPath() %>/js/jdyna/jdyna.js"></script>    
    <script type="text/javascript" src="<%= request.getContextPath() %>/js/jquery.dataTables.min.js"></script>
	<script type="text/javascript" src="<%= request.getContextPath() %>/js/dataTables.bootstrap.min.js"></script>
	<script type="text/javascript" src="<%= request.getContextPath() %>/js/dataTables.buttons.min.js"></script>
	<script type="text/javascript" src="<%= request.getContextPath() %>/js/buttons.bootstrap.min.js"></script>
	<script type="text/javascript" src="<%= request.getContextPath() %>/js/buttons.html5.min.js"></script>
	<script type="text/javascript" src="<%= request.getContextPath() %>/js/dataTables.responsive.min.js"></script>	
	<script type="text/javascript" src="<%= request.getContextPath() %>/js/bootstrap-datetimepicker.min.js"></script>
	<script type="text/javascript" src="<%= request.getContextPath() %>/static/js/dataTables.select.min.js"></script>
	<script type="text/javascript" src="<%= request.getContextPath() %>/js/jszip.min.js"></script>
	<script type='text/javascript'>
		var j = jQuery.noConflict();
		var $ = jQuery.noConflict();
		var JQ = j;
		dspaceContextPath = "<%=request.getContextPath()%>";
		jQuery(document).ready(function ($) {
			  $('span[data-toggle="tooltip"]').tooltip();
			  $('i[data-toggle="tooltip"]').tooltip();
		});
	</script>
	<% if(StringUtils.isNotBlank(LocaleUIHelper.ifLtr(request, "","rtl"))) { %>
    <script type="text/javascript"> 
    jQuery(document).ready(function() {
    	var sxLayout = jQuery('#sx-layout');
    	var dxLayout = jQuery('#dx-layout');
    	var sxLayoutContent = sxLayout.html();
    	var dxLayoutContent = "";
    	jQuery('.badge').css('float','none');
    	jQuery.each(jQuery('.badge'),function (index, value) {
    		jQuery(value).appendTo(jQuery(value).parent());
    	});
    	if (dxLayout.size() == 0) {
    		sxLayout.insertAfter(jQuery('#central-layout'));
    		sxLayout.addClass('hidden-xs');
        	sxLayout.children('.list-group').css('margin-right','-50px');
    	}
    	else {
    		dxLayoutContent = dxLayout.html();
    		sxLayout.html(dxLayoutContent);
    		dxLayout.html(sxLayoutContent);
    		sxLayout.removeClass('hidden-xs');
        	dxLayout.addClass('hidden-xs');
        	dxLayout.children('.list-group').css('margin-right','-50px');
    	}
    });
    </script>
    <% } %>
    <%--Gooogle Analytics recording.--%>
    <%
    if (analyticsKey != null && analyticsKey.length() > 0)
    {
    %>
        <script type="text/javascript">
            var _gaq = _gaq || [];
            _gaq.push(['_setAccount', '<%= analyticsKey %>']);
            _gaq.push(['_trackPageview']);

            (function() {
                var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
                ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
                var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
            })();
        </script>
    <%
    }
    if (extraHeadDataLast != null)
    { %>
		<%= extraHeadDataLast %>
		<%
		    }
    %>
    

	<!-- HTML5 shiv and Respond.js IE8 support of HTML5 elements and media queries -->
	<!--[if lt IE 9]>  
	  <script src="<%= request.getContextPath() %>/static/js/html5shiv.js"></script>
	  <script src="<%= request.getContextPath() %>/static/js/selectivizr-min.js"></script>
	  <script src="<%= request.getContextPath() %>/static/js/respond.min.js"></script>
	  <link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/bootstrap/dspace-theme-IElte9.css" type="text/css" />
	<![endif]-->
    </head>

    <%-- HACK: leftmargin, topmargin: for non-CSS compliant Microsoft IE browser --%>
    <%-- HACK: marginwidth, marginheight: for non-CSS compliant Netscape browser --%>
    <body class="undernavigation" dir="<%= LocaleUIHelper.ifLtr(request, "ltr","rtl") %>">
<a class="sr-only" href="#content">Skip navigation</a>
<header class="navbar navbar-inverse navbar-square">    
    <%
    if (!navbar.equals("off"))
    {
%>
            <div class="container-fluid">
                <dspace:include page="<%= navbar %>" />
            </div>
<%
    }
    else
    {
    	%>
        <div class="container-fluid">
            <dspace:include page="/layout/navbar-minimal.jsp" />
        </div>
<%    	
    }
%>

<% if(cookiesPolicyEnabled) { %>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/static/css/cookieconsent/cookieconsent.min.css" />
<script src="<%= request.getContextPath() %>/static/js/cookieconsent/cookieconsent.min.js"></script>
<script>
window.addEventListener("load", function(){
window.cookieconsent.initialise({
  "palette": {
    "popup": {
      "background": "#edeff5",
      "text": "#838391"
    },
    "button": {
      "background": "#4b81e8"
    }
  },
  "position": "bottom-right",
  "theme": "classic",
  "content": {
    "message": "<%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.layout.navbar-default.cookies.info.message") %>",
    "dismiss": "<%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.layout.navbar-default.cookies.button") %>",
    "link": "<%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.layout.navbar-default.cookies.info.link") %>",
    "href": "<%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.layout.navbar-default.cookies.href") %>"
  }
})});
</script>
<% } %>
</header>

<main id="content" role="main">
<div class="container banner">
	<div class="row">
		<div class="col-sm-12">
<% if (supportedLocales != null && supportedLocales.length > 1)
     {
 %>
	 <ul class="nav navbar-nav navbar-<%= isRtl ? "left" : "right" %>">
      
 <%
    for (int i = supportedLocales.length-1; i >= 0; i--)
     {
 %>
        <li><a onclick="javascript:document.repost.locale.value='<%=supportedLocales[i].toString()%>';
                  document.repost.submit();" href="?locale=<%=supportedLocales[i].toString()%>">
          <%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.layout.navbar-default.language."+supportedLocales[i].toString()) %>                  
       </a></li>
 <%
     }
 %>
     </ul>
 <%
   }
 %>		
		
		
		
		</div>
		  <div class="col-sm-8 brand pull-<%= isRtl ?"right" :"left" %>">
		<h1><fmt:message key="jsp.layout.header-default.brand.heading" /></h1>
        <fmt:message key="jsp.layout.header-default.brand.description" /> 
        </div>
        <div class="col-sm-4 hidden-xs pull-<%= isRtl ?"left" :"right" %>"><img class="img-responsive" src="<%= request.getContextPath() %>/image/logo.gif" alt="DSpace logo" />
        </div>
	</div>
</div>	
<br/>
                <%-- Location bar --%>
<%
    if (locbar)
    {
%>
<div class="container">
	<div class="row">
		<div class="col-sm-12">
                <dspace:include page="/layout/location-bar.jsp" />
        </div>        
    </div>
</div>                
<%
    }
%>



        <%-- Page contents --%>
<div class="container fullheight">
<% if (request.getAttribute("dspace.layout.sidebar") != null) { %>
	<div class="row">
		<div class="col-md-9 <%= isRtl ? "pull-right":"" %>">
<% } %>		
