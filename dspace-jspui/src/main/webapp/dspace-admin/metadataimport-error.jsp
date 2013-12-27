<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.app.bulkedit.MetadataImportInvalidHeadingException" %>

<%--
  - Form to show an error from the metadata importer
--%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    // Get the error message
    String error = (String)request.getAttribute("message");

    // Is it a bad metadata element in the header?
    String badheader = (String)request.getAttribute("badheading");
    if (badheader != null)
    {
        if (badheader.equals("" + MetadataImportInvalidHeadingException.SCHEMA))
        {
            error = LocaleSupport.getLocalizedMessage(pageContext, "jsp.dspace-admin.metadataimport.badheadingschema") +
                    ": " + error;
        }
        else
        {
            error = LocaleSupport.getLocalizedMessage(pageContext, "jsp.dspace-admin.metadataimport.badheadingelement") +
                    ": " + error;
        }
    }
    else if (error == null)
    {
        error = LocaleSupport.getLocalizedMessage(pageContext, "jsp.dspace-admin.metadataimport.unknownerror");
    }


%>

<dspace:layout style="submission" titlekey="jsp.dspace-admin.metadataimport.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin" 
               nocache="true">

    <h1><fmt:message key="jsp.dspace-admin.metadataimport.title"/></h1>

    <p>
        <b><%= error %></b>
    </p>
    
</dspace:layout>
