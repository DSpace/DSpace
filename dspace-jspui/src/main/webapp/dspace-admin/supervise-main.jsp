<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
   - This page provides the options for administering supervisor settings
   --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<dspace:layout style="submission"
			   titlekey="jsp.dspace-admin.supervise-main.title"
               navbar="admin"
               locbar="link"
               parentlink="/dspace-admin"
               parenttitlekey="jsp.administer">

<h1><fmt:message key="jsp.dspace-admin.supervise-main.heading"/>
<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#supervision\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup>
</h1>

<h3><fmt:message key="jsp.dspace-admin.supervise-main.subheading"/></h3>

<br/>

<div align="center" />
<%-- form to navigate to any of the three options available --%>
<form method="post" action="">
    <div class="row">
    	<input class="btn btn-primary col-md-6 col-md-offset-3" type="submit" name="submit_add" value="<fmt:message key="jsp.dspace-admin.supervise-main.add.button"/>"/>
    </div>
    <div class="row">
    	<input class="btn btn-info col-md-6 col-md-offset-3" type="submit" name="submit_view" value="<fmt:message key="jsp.dspace-admin.supervise-main.view.button"/>"/>
    </div>
    <div class="row">    
    	<input class="btn btn-warning col-md-6 col-md-offset-3" type="submit" name="submit_clean" value="<fmt:message key="jsp.dspace-admin.supervise-main.clean.button"/>"/>
    </div>
</form>
<div align="center" />

</dspace:layout>
