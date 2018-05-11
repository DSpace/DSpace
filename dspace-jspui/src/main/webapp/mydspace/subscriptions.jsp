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
  -   avail          - List<Collection> - collections available to this user for subscription.
  -   subscriptions  - List<Subscription> - collections user is subscribed to.
  -   updated        - Boolean - if true, subscriptions have just been updated.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
           prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.app.util.CollectionDropDown" %>
<%@ page import="org.dspace.eperson.Subscription" %>
<%@ page import="java.util.List" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.core.Context" %>

<%
    List<Collection> availableSubscriptions =
        (List<Collection>) request.getAttribute("availableSubscriptions");
    List<Subscription> subscriptions =
        (List<Subscription>) request.getAttribute("subscriptions");
    boolean updated =
        ((Boolean) request.getAttribute("updated")).booleanValue();
    Context context = UIUtil.obtainContext(request);
%>

<dspace:layout style="submission" locbar="link"
               parentlink="/mydspace"
               parenttitlekey="jsp.mydspace"
               titlekey="jsp.mydspace.subscriptions.title">

    <%-- <h1>Your Subscriptions</h1> --%>
    <h1>
        <fmt:message key="jsp.mydspace.subscriptions.title"/>
        <dspace:popup page='<%= LocaleSupport.getLocalizedMessage(pageContext, "help.index") +"#subscribe" %>'>
            <fmt:message key="jsp.help"/>
        </dspace:popup>
    </h1>
<%
    if (updated)
    {
%>
	<p><strong><fmt:message key="jsp.mydspace.subscriptions.info1"/></strong></p>
	<p><fmt:message key="jsp.mydspace.subscriptions.info2"/></p>
<%
    }
%>
    <form class="form-group" action="<%= request.getContextPath() %>/subscribe" method="post">
        <div class="col-md-6">
            <select id="available-subscriptions" class="form-control" name="collection">
                <option value="-1">
                    <fmt:message key="jsp.mydspace.subscriptions.select_collection" />
                </option>
<%
for (int i = 0; i < availableSubscriptions.size(); i++)
{
%>
                <option value="<%= availableSubscriptions.get(i).getID() %>">
                    <%= CollectionDropDown.collectionPath(context, availableSubscriptions.get(i), 0) %>
                </option>
<%
}
%>
            </select>
        </div>
        <input class="btn btn-success"
               type="submit"
               name="submit_subscribe"
               value="<fmt:message key="jsp.collection-home.subscribe"/>" />
        <input class="btn btn-danger"
               type="submit"
               name="submit_clear"
               value="<fmt:message key="jsp.mydspace.subscriptions.remove.button"/>" />
	</form>

<%
    if (subscriptions.size() > 0)
    {
%>
	<p><fmt:message key="jsp.mydspace.subscriptions.info3"/></p>
    
        <table class="table" summary="Table displaying your subscriptions">
<%
        String row = "odd";

        for (int i = 0; i < subscriptions.size(); i++)
        {
%>
            <tr>
                <%--
                  -  HACK: form shouldn't open here, but IE adds a carraige
                  -  return where </form> is placed, breaking our nice layout.
                  --%>

                 <td class="<%= row %>RowOddCol">
                      <a href="<%= request.getContextPath() %>/handle/<%= subscriptions.get(i).getCollection().getHandle() %>">
                          <%= CollectionDropDown.collectionPath(context, subscriptions.get(i).getCollection(),0) %>
                      </a>
                 </td>
                 <td class="<%= row %>RowEvenCol">
                    <form method="post" action=""> 
                        <input type="hidden" name="collection" value="<%= subscriptions.get(i).getCollection().getID() %>" />
                        <input class="btn btn-warning"
                               type="submit"
                               name="submit_unsubscribe"
                               value="<fmt:message key="jsp.mydspace.subscriptions.unsub.button"/>" />
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
    <p align="center">
        <a href="<%= request.getContextPath() %>/mydspace">
            <fmt:message key="jsp.mydspace.general.goto-mydspace"/>
        </a>
    </p>

</dspace:layout>
