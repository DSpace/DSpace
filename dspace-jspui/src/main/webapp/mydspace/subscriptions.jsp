<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>    

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.app.util.CollectionDropDown" %>
<%@page import="org.dspace.app.cris.model.ACrisObject"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="org.dspace.app.cris.model.VisibilityConstants"%>
<%@page import="org.dspace.app.cris.util.ResearcherPageUtils"%>
<%@page import="java.util.List"%>


<%
    Collection[] availableSubscriptions =
        (Collection[]) request.getAttribute("availableSubscriptions");
    Collection[] subscriptions =
        (Collection[]) request.getAttribute("subscriptions");
    boolean updated =
        ((Boolean) request.getAttribute("updated")).booleanValue();
    Community[] commSubscriptions = (Community[]) request
   		 .getAttribute("comm_subscriptions");

    List<ACrisObject> rpSubscriptions = (List<ACrisObject>) request
   	 	.getAttribute("crisobject_subscriptions");
%>
<dspace:layout style="submission" locbar="link"
               parentlink="/mydspace"
               parenttitlekey="jsp.mydspace"
               titlekey="jsp.mydspace.subscriptions.title">

                <%-- <h1>Your Subscriptions</h1> --%>
<h1><fmt:message key="jsp.mydspace.subscriptions.title"/>
	<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") +\"#subscribe\" %>"><fmt:message key="jsp.help"/></dspace:popup>
</h1>
<%
    if (updated)
    {
%>
	<div class="alert alert-success"><fmt:message key="jsp.mydspace.subscriptions.info1"/></div>
<%
    }
%>



<div id="content">
<ul id="tabs" class="nav nav-tabs" data-tabs="tabs">
<li class="active"><a href="<%= request.getContextPath() %>/subscribe"><fmt:message key="jsp.layout.navbar-hku.item-subscription"/></a></li>
<li><a href="<%= request.getContextPath() %>/cris/tools/stats/subscription/list.htm"><fmt:message key="jsp.layout.navbar-hku.stat-subscription"/></a></li>
</ul>
<div id="my-tab-content" class="tab-content">
<div class="tab-pane active" id="contentsubscription">




<!-- 
        <form class="form-group" action="<%= request.getContextPath() %>/subscribe" method="post">
        	<div class="col-md-6">
            <select id="available-subscriptions" class="form-control" name="collection">
                <option value="-1"><fmt:message key="jsp.mydspace.subscriptions.select_collection" /></option>
<%
    if (availableSubscriptions!=null)
		for (int i = 0; i < availableSubscriptions.length; i++)
	    {
%>
                <option value="<%= availableSubscriptions[i].getID() %>"><%= CollectionDropDown.collectionPath(availableSubscriptions[i], 0) %></option>
<%
   		}
%>
            </select>
            </div>
            <input class="btn btn-success" type="submit" name="submit_subscribe" value="<fmt:message key="jsp.collection-home.subscribe"/>" />
 			<input class="btn btn-danger" type="submit" name="submit_clear" value="<fmt:message key="jsp.mydspace.subscriptions.remove.button"/>" />
	</form>
-->
<h3 class="mydspace-subscriptions"><fmt:message key="jsp.mydspace.subscriptions.community-head"/></h3>
<p><fmt:message key="jsp.mydspace.subscriptions.info2-community"/></p>
<%
if (commSubscriptions!=null)
	if (commSubscriptions.length > 0)
	{
%>
<p><fmt:message key="jsp.mydspace.subscriptions.info3-community"/></p>

<center>
    <table class="table" summary="Table displaying your subscriptions">
	<tr>
		<th>Community</th>
		<th>Identifier</th>
		<th />
	</tr>
<%
String row = "odd";

    for (int i = 0; i < commSubscriptions.length; i++)
    {
%>
        <tr>
            <%--
              -  HACK: form shouldn't open here, but IE adds a carraige
              -  return where </form> is placed, breaking our nice layout.
              --%>
             <td class="<%=row%>RowOddCol"><%=commSubscriptions[i].getMetadata("name")%></td>
             <td class="<%=row%>RowEvenCol">
                  <a href="<%=request.getContextPath()%>/handle/<%=commSubscriptions[i].getHandle()%>"><%=commSubscriptions[i].getHandle()%></a>
             </td>
             <td class="<%=row%>RowOddCol">
                <form method="post" action=""> 
                    <input type="hidden" name="community" value="<%=commSubscriptions[i].getID()%>" />
		<input type="submit" class="btn btn-warning" name="submit_unsubscribe" value="<fmt:message key="jsp.mydspace.subscriptions.unsub.button"/>" />
                </form>
             </td>
        </tr>
<%
row = (row.equals("even") ? "odd" : "even");
    }
%>
   <tr>
	<td /><td />
	<td><form method="post" action=""><input type="submit" class="btn btn-danger" name="submit_clear_comm" value="<fmt:message key="jsp.mydspace.subscriptions.remove.button"/>" /></form></td>
   </tr>
    </table>
</center>

<br/>

<%
}
else
{
%>
<p><fmt:message key="jsp.mydspace.subscriptions.info4-community"/></p>
<%
}
%>
<br/>

<h3 class="mydspace-subscriptions"><fmt:message key="jsp.mydspace.subscriptions.collection-head"/></h3>        
<p><fmt:message key="jsp.mydspace.subscriptions.info2"/></p>
<%
    if (subscriptions.length > 0)
    {
%>
	<p><fmt:message key="jsp.mydspace.subscriptions.info3"/></p>
    
        <table class="table" summary="Table displaying your subscriptions">
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
                      <a href="<%= request.getContextPath() %>/handle/<%= subscriptions[i].getHandle() %>"><%= CollectionDropDown.collectionPath(subscriptions[i],0) %></a>
                 </td>
                 <td class="<%= row %>RowEvenCol">
                    <form method="post" action=""> 
                        <input type="hidden" name="collection" value="<%= subscriptions[i].getID() %>" />
			<input class="btn btn-warning" type="submit" name="submit_unsubscribe" value="<fmt:message key="jsp.mydspace.subscriptions.unsub.button"/>" />
                    </form>
                 </td>
            </tr>
<%
            row = (row.equals("even") ? "odd" : "even" );
        }
%>
        </table>

    <br/>

<%
    }
    else
    {
%>
	<p><fmt:message key="jsp.mydspace.subscriptions.info4"/></p>
<%
    }
%>
<br/>
<h3 class="mydspace-subscriptions"><fmt:message key="jsp.mydspace.subscriptions.rp-head"/></h3>
<p><fmt:message key="jsp.mydspace.subscriptions.info2-rp"/></p>
<%
if (rpSubscriptions!=null && rpSubscriptions.size() > 0)
{
%>
<p><fmt:message key="jsp.mydspace.subscriptions.info3-rp"/></p>

<center>
    <table class="table" summary="Table displaying your subscriptions">
	<tr>
		<th>Type</th>
		<th>Name</th>
		<th>Identifier</th>
		<th/>
	</tr>
<%
String row = "odd";

    for (ACrisObject rp : rpSubscriptions)
    {
%>
        <tr>
            <%--
              -  HACK: form shouldn't open here, but IE adds a carraige
              -  return where </form> is placed, breaking our nice layout.
              --%>
              
              	<td class="<%=row%>RowOddCol"><%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.statistics.table.type."+rp.getType()) %></td>
				<td class="<%=row%>RowOddCol"><%= rp.getName() %></td>
				<td class="<%=row%>RowEvenCol"><a href="<%=request.getContextPath()%>/cris/<%= rp.getPublicPath() %>/<%= ResearcherPageUtils.getPersistentIdentifier(rp) %>"><%= rp.getCrisID() %></a></td>
				<td class="<%=row%>RowOddCol"><form method="post" action="">
                    <input type="hidden" name="crisobject" value="<%=rp.getUuid()%>" />
                    <input type="submit" class="btn btn-warning" name="submit_unsubscribe" value="<fmt:message key="jsp.mydspace.subscriptions.unsub.button"/>" />
     			</td>
     	</tr>
</form>
        
<%
row = (row.equals("even") ? "odd" : "even");
    }
%>
   <tr>
	<td />
	<td />
	<td />	
	<td><form method="post" action=""><input type="submit" class="btn btn-danger" name="submit_clear_rp" value="<fmt:message key="jsp.mydspace.subscriptions.remove.button"/>" /></td>
    </form>
   </tr>
    </table>
</center>

<br/>

<%
}
else
{
%>
<p><fmt:message key="jsp.mydspace.subscriptions.info4-rp"/></p>
<%
}
%>




</div>
<div class="tab-pane" id="statisticssubscription">
<h1><fmt:message key="jsp.layout.navbar-hku.stat-subscription"/></h1>
<p></p>
</div>
</div>
</div>


</dspace:layout>