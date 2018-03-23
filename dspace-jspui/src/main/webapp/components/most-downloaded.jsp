<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<% if(mostViewedBitstream != null && mostViewedBitstream.getItems().size()!=0){ %>
        <div class="panel panel-primary vertical-carousel" data-itemstoshow="3">        
        <div class="panel-heading">
          <h3 class="panel-title">
          		<fmt:message key="jsp.components.most-downloaded"/>
          </h3>
       </div>   
	   <div class="panel-body">
	   		<div class="list-groups">
<% for(MostViewedItem mvi : mostViewedBitstream.getItems()){
		IGlobalSearchResult item = mvi.getItem();
		if ( mvi.getVisits()==null ) {
			%>
				<fmt:message key="jsp.components.most-downloaded.data-loading"/>
			<%
			break;		
		}
%>
		<dspace:discovery-artifact style="global" artifact="<%= item %>" view="<%= mostViewedBitstream.getConfiguration() %>">
		<span class="badge" data-toggle="tooltip" data-placement="right" title="<fmt:message key="jsp.components.most-downloaded.badge-tooltip"/>"><fmt:formatNumber value="<%= (mvi==null || mvi.getVisits()==null)?0.0:mvi.getVisits() %>" type="NUMBER" maxFractionDigits="0" /></span> ##artifact-item##
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