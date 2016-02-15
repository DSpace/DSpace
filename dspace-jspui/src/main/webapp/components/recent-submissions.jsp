<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%
if (submissions != null && submissions.count() > 0)
{
%>
        <div class="panel panel-info vertical-carousel" data-itemstoshow="3">        
        <div class="panel-heading">
          <h3 class="panel-title">
          		<fmt:message key="jsp.collection-home.recentsub"/>
          </h3>
       </div>   
	   <div class="panel-body">
	<%
    if(feedEnabled)
    {
    	%>
    	<div class="row">
	   	<div class="col-md-12">
	   	<div class="pull-right small">
    	
    	<%
	    	String[] fmts = feedData.substring(feedData.indexOf(':')+1).split(",");
	    	String icon = null;
	    	int width = 0;
	    	for (int j = 0; j < fmts.length; j++)
	    	{
	%>
		<c:set var="fmtkey">jsp.recentsub.rss.<%= fmts[j] %></c:set>
	    <a href="<%= request.getContextPath() %>/feed/<%= fmts[j] %>/site"><i alt="RSS Feeds" class="fa fa-rss"></i> 
	    <sup class="small"><fmt:message key="${fmtkey}" /></sup></a>
	<%
	    	}
	    	%>
	</div>
	   	</div>
	   </div>
	<%
	    }
%>
	<div class="list-groups">
	<%	
		for (IGlobalSearchResult obj : submissions.getRecentSubmissions()) {
		%>
		
				<dspace:discovery-artifact style="global" artifact="<%= obj %>" view="<%= submissions.getConfiguration() %>"/>
		
		<%
		     }
		%>
		</div>
		  </div>
     </div>
<%
}
%>