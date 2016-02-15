<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%
if (download != null && download.count() > 0)
{
%>
        <div class="panel panel-primary vertical-carousel" data-itemstoshow="1">        
        <div class="panel-heading">
          <h3 class="panel-title">
          		<fmt:message key="jsp.explore-home.topdownload"/>
          </h3>
       </div>   
	   <div class="panel-body">
	<div class="list-groups">
	<%	
		for (IGlobalSearchResult obj : download.getRecentSubmissions()) {
		%>
		
				<dspace:discovery-artifact style="global" artifact="<%= obj %>" view="<%= download.getConfiguration() %>">
				<span class="badge" data-toggle="tooltip" data-placement="right" title="<fmt:message key="jsp.components.most-download.badge-tooltip"/>">
					<fmt:formatNumber value="<%= (obj==null || ((DSpaceObject) obj).getExtraInfo().get(\"crismetrics_download\")==null)?0.0:((DSpaceObject) obj).getExtraInfo().get(\"crismetrics_download\") %>" type="NUMBER" maxFractionDigits="0" />
					</span> ##artifact-item##
				</dspace:discovery-artifact>
		
		<%
		     }
		%>
		</div>
		  </div>
     </div>
<%
}
%>