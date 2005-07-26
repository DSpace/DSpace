<%--
  - itemmap-info.jsp
  -
  - Version: $ $
  -
  - Date: $ $
  -
  - Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
  - Institute of Technology.  All rights reserved.
  -
  - Redistribution and use in source and binary forms, with or without
  - modification, are permitted provided that the following conditions are
  - met:
  -
  - - Redistributions of source code must retain the above copyright
  - notice, this list of conditions and the following disclaimer.
  -
  - - Redistributions in binary form must reproduce the above copyright
  - notice, this list of conditions and the following disclaimer in the
  - documentation and/or other materials provided with the distribution.
  -
  - - Neither the name of the Hewlett-Packard Company nor the name of the
  - Massachusetts Institute of Technology nor the names of their
  - contributors may be used to endorse or promote products derived from
  - this software without specific prior written permission.
  -
  - THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  - ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  - LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  - A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  - HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  - INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
  - BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
  - OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  - ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  - TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
  - USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
  - DAMAGE.
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
    
    // supported values: "none-selected", "added", "remove"
    String message        = (String)request.getAttribute("message");    
    
    LinkedList processedItems = (LinkedList)request.getAttribute("processedItems");
%>

<dspace:layout titlekey="jsp.tools.itemmap-info.title">

    <h2><fmt:message key="jsp.tools.itemmap-info.heading"/></h2>

    <p>
    <% if (message.equals("none-selected")) { %>
        <fmt:message key="jsp.tools.itemmap-info.msg.none-selected" />
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
    </p>
    
    <form method=POST>
        <input type="hidden" name="cid" value="<%=collection.getID()%>">
        <input type="submit" name="submit" value="<fmt:message key="jsp.tools.itemmap-info.button.continue"/>">
    </form>
</dspace:layout>
