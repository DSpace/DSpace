<%--
  - subscription.jsp
  -
  - Version: $Revision$
  -
  - Date: $Date$
  -
  - Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
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
  -   communities    - Community[]  - communities that subscribed collections
  -                    are in - i.e. subscriptions[0] is in communities[0].
  -   updated        - Boolean - if true, subscriptions have just been updated
  --%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.content.Collection" %>

<%
    Collection[] subscriptions =
        (Collection[]) request.getAttribute("subscriptions");
    Community[] communities =
        (Community[]) request.getAttribute("communities");
    boolean updated =
        ((Boolean) request.getAttribute("updated")).booleanValue();
%>

<dspace:layout locbar="link"
               parenttitle="My DSpace"
               parentlink="/mydspace "
               title="Your Subscriptions">

    <H1>Your Subscriptions</H1>
<%
    if (updated)
    {
%>
    <P><strong>Your subscriptions have been updated.</strong></P>
<%
    }
%>
    <P>To subscribe to a collection, visit the collection's home page, and
    click on the "Subscribe" button.</P>
<%
    if (subscriptions.length > 0)
    {
%>
    <P>Below are the collections you are subscribed to.  You will be sent an
    e-mail each day detailing new items that have become available in these
    collections.  On days that no new items have appeared, no e-mail will be
    sent.</P>
    
    <center>
        <table class="miscTable">
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
                <form method=POST>
                    <td class="<%= row %>RowOddCol">
                        <A HREF="<%= request.getContextPath() %>/communities/<%= communities[i].getID() %>/collections/<%= subscriptions[i].getID() %>/"><%= subscriptions[i].getMetadata("name") %></A>
                    </td>
                    <td class="<%= row %>RowEvenCol">
                        <input type="hidden" name="collection" value="<%= subscriptions[i].getID() %>">
                        <input type="submit" name="submit_unsubscribe" value="Unsubscribe">
                    </td>
                </form>
            </tr>
<%
            row = (row.equals("even") ? "odd" : "even" );
        }
%>
        </table>
    </center>

    <br>

    <center>
        <form method=POST>
            <input type="submit" name="submit_clear" value="Remove All Subscriptions">
        </form>
    </center>
<%
    }
    else
    {
%>
    <P>You are not currently subscribed to any collections.</P>
<%
    }
%>

    <P align="center"><A HREF="<%= request.getContextPath() %>/mydspace">Go to
    My DSpace </A></P>

</dspace:layout>
