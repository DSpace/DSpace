<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Form to upload a bitstream
  -
  - Attributes:
  -    item - the item the bitstream will be added to
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page import="org.dspace.content.Item" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    Item item = (Item) request.getAttribute("item");
    request.setAttribute("LanguageSwitch", "hide");
    Boolean noFileSelected = (Boolean) request.getAttribute("noFileSelected");
    boolean isNoFileSelected = (noFileSelected == null ? false : noFileSelected.booleanValue());
%>

<dspace:layout style="submission" titlekey="jsp.tools.upload-bitstream.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin"
               nocache="true">

    <%-- <h1>Upload Bitstream</h1> --%>
	<h1><fmt:message key="jsp.tools.upload-bitstream.title"/></h1>
    
    <%-- <p>Select the bitstream to upload</p> --%>
    <% if(isNoFileSelected){ %>
        <p class="alert alert-warning"><fmt:message key="jsp.tools.upload-bitstream.select.file.msg"/></p>
    <%} else {%>
	<p class="alert alert-info"><fmt:message key="jsp.tools.upload-bitstream.info"/></p>
    <%}%>
    <form method="post" enctype="multipart/form-data" action="">
        <div class="container row">        	
            <input class="form-control" type="file" size="40" name="file"/>
        </div>
        
        <input type="hidden" name="item_id" value="<%= item.getID() %>"/>
		<br/>
        <!-- <p align="center"><input type="submit" name="submit" value="Upload"></p> -->
		<div class="container row col-md-offset-5"><input class="btn btn-success col-md-4" type="submit" name="submit" value="<fmt:message key="jsp.tools.upload-bitstream.upload"/>" /></div>
    </form>
    
</dspace:layout>
