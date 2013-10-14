<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Display information with a 'continue' button. (Maybe a cancel?)
  -
  - Attributes to pass in:
  -
  -   collection - Collection we're managing
  -   message    - String to output
  --%>
  
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ page import="java.net.URLEncoder"            %>
<%@ page import="java.util.Iterator"             %>
<%@ page import="java.util.Map"                  %>
<%@ page import="java.util.LinkedList"           %>
<%@ page import="org.dspace.content.Collection"  %>
<%@ page import="org.dspace.content.Item"        %>
<%@ page import="org.dspace.core.ConfigurationManager" %>

<%
    Collection collection = (Collection)request.getAttribute("collection");
    
    // supported values: "none-selected", "none-removed", "added", "remove"
    String message        = (String)request.getAttribute("message");    
    
    LinkedList processedItems = (LinkedList)request.getAttribute("processedItems");
%>

<dspace:layout style="submission" titlekey="jsp.tools.itemmap-info.title">

    <h2><fmt:message key="jsp.tools.itemmap-info.heading"/></h2>

    <div class="alert">
    <% if (message.equals("none-selected")) { %>
        <fmt:message key="jsp.tools.itemmap-info.msg.none-selected" />
    <% } else if (message.equals("none-removed")) { %>
        <fmt:message key="jsp.tools.itemmap-info.msg.none-removed" />
    <% } else if (message.equals("added")) { %>
            <%-- Iterate through processed items --%>
            <% for (int i=0; i<processedItems.size(); i++) { %>
                    <fmt:message key="jsp.tools.itemmap-info.msg.added">
                            <fmt:param><%= (String)processedItems.get(i) %></fmt:param>
                    </fmt:message><br/>
            <% } %>
    <% } else if (message.equals("remove")) { %>
            <%-- Iterate through processed items --%>
            <% for (int i=0; i<processedItems.size(); i++) { %>
                    <fmt:message key="jsp.tools.itemmap-info.msg.remove">
                            <fmt:param><%= (String)processedItems.get(i) %></fmt:param>
                    </fmt:message><br/>
            <% } %>
    <% } %>
    </div>
    
    <form method="post">
        <input type="hidden" name="cid" value="<%=collection.getID()%>"/>
        <input class="btn btn-default" type="submit" name="submit" value="<fmt:message key="jsp.tools.itemmap-info.button.continue"/>"/>
    </form>
</dspace:layout>
