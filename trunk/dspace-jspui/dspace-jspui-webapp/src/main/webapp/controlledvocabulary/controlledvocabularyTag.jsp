<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
  
  
<%--
  -- This custom tag is used to display an HTML version of a 
  -- controlled vocabulary.
  -- This jsp is called by ControlledVocabularyTag.java
  --%>  
  
<%@ page language="java" errorPage="/internal-error" contentType="text/html;charset=UTF-8" %>
<%@ page  import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%
	String vocabulariesHTML = (String)session.getAttribute("controlledvocabulary.vocabularyHTML");
	vocabulariesHTML = (vocabulariesHTML == null || vocabulariesHTML.length() == 0)?
							LocaleSupport.getLocalizedMessage(pageContext,"jsp.controlledvocabulary.controlledvocabularytag.noresults"): 
	                        vocabulariesHTML;
%>

<%= vocabulariesHTML %>



