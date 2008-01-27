<%--
  - subscription.jsp
  -
  - Version: $Revision$
  -
  - Date: $Date$
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
  - Show a user's subscriptions and allow them to be modified
  -
  - Attributes:
  -   subscriptions  - Collection[] - collections user is subscribed to
  -   updated        - Boolean - if true, subscriptions have just been updated
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>
    

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.content.Collection" %>

<%@ page import="org.dspace.uri.IdentifierFactory" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%
    Collection[] subscriptions =
        (Collection[]) request.getAttribute("subscriptions");
    boolean updated =
        ((Boolean) request.getAttribute("updated")).booleanValue();
%>

<dspace:layout locbar="link"
               parentlink="/mydspace"
               parenttitlekey="jsp.mydspace"
               titlekey="jsp.mydspace.subscriptions.title">

    <table width="100%" border="0">
        <tr>
            <td align="left">
                <%-- <h1>Your Subscriptions</h1> --%>
				<h1><fmt:message key="jsp.mydspace.subscriptions.title"/></h1>
            </td>
            <td align="right" class="standard">
		<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") +\"#subscribe\" %>"><fmt:message key="jsp.help"/></dspace:popup>
            </td>
        </tr>
    </table>
 
<%
    if (updated)
    {
%>
	<p><strong><fmt:message key="jsp.mydspace.subscriptions.info1"/></strong></p>
<%
    }
%>
	<p><fmt:message key="jsp.mydspace.subscriptions.info2"/></p>
<%
    if (subscriptions.length > 0)
    {
%>
	<p><fmt:message key="jsp.mydspace.subscriptions.info3"/></p>
    
    <center>
        <table class="miscTable" summary="Table displaying your subscriptions">
<%
        String row = "odd";

        for (int i = 0; i < subscriptions.length; i++)
        {
%>
            <tr>
                <%--
                  -  HACK: form shouldn't open here, but IE adds a carraige
                  -  return where </form> is placed, breaking our nice layout.
                  --%>

                 <td class="<%= row %>RowOddCol">
                      <a href="<%= IdentifierFactory.getURL(subscriptions[i]).toString() %>"><%= subscriptions[i].getMetadata("name") %></a>
                 </td>
                 <td class="<%= row %>RowEvenCol">
                    <form method="post" action=""> 
                        <input type="hidden" name="collection" value="<%= subscriptions[i].getID() %>" />
			<input type="submit" name="submit_unsubscribe" value="<fmt:message key="jsp.mydspace.subscriptions.unsub.button"/>" />
                    </form>
                 </td>
            </tr>
<%
            row = (row.equals("even") ? "odd" : "even" );
        }
%>
        </table>
    </center>

    <br/>

    <center>
        <form method="post" action="">
    	    <input type="submit" name="submit_clear" value="<fmt:message key="jsp.mydspace.subscriptions.remove.button"/>" />
        </form>
    </center>
<%
    }
    else
    {
%>
	<p><fmt:message key="jsp.mydspace.subscriptions.info4"/></p>
<%
    }
%>
<p align="center"><a href="<%= request.getContextPath() %>/mydspace"><fmt:message key="jsp.mydspace.general.goto-mydspace"/> </a></p>

</dspace:layout>
