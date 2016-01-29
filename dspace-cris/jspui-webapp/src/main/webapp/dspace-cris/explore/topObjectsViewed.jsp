<%
if (viewed != null && viewed.count() > 0)
{
%>
        <div class="panel panel-primary vertical-carousel" data-itemstoshow="1">        
        <div class="panel-heading">
          <h3 class="panel-title">
          		<fmt:message key="jsp.explore-home.topviewed"/>
          </h3>
       </div>   
	   <div class="panel-body">
	<div class="list-groups">
	<%	
		for (IGlobalSearchResult obj : viewed.getRecentSubmissions()) {
		%>
				<dspace:discovery-artifact style="global" artifact="<%= obj %>" view="<%= viewed.getConfiguration() %>">
				<span class="badge" data-toggle="tooltip" data-placement="right" title="<fmt:message key="jsp.components.most-viewed.badge-tooltip"/>">
					<fmt:formatNumber value="<%= (obj==null || ((DSpaceObject) obj).getExtraInfo().get(\"crismetrics_view\")==null)?0.0:((DSpaceObject) obj).getExtraInfo().get(\"crismetrics_view\") %>" type="NUMBER" maxFractionDigits="0" />
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