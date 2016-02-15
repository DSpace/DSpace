<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%
if (submissions != null && submissions.count() > 0)
{
%>
        <div class="panel panel-primary vertical-carousel" data-itemstoshow="1">        
        <div class="panel-heading">
          <h3 class="panel-title">
          		<fmt:message key="jsp.collection-home.recentsub"/>
          </h3>
       </div>   
	   <div class="panel-body">
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