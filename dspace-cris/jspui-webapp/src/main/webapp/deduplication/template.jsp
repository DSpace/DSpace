<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<script type="text/x-tmpl" id="tmpl-deduplication-untitled">
	<fmt:message key="jsp.deduplication.untitled"/>
</script>
<script type="text/x-tmpl" id="tmpl-deduplication-label-owner">
	<fmt:message key="jsp.deduplication.render.summary.label.owner"/>
</script>
<script type="text/x-tmpl" id="tmpl-deduplication-label-identifier">
	<fmt:message key="jsp.deduplication.render.summary.label.identifier"/>
</script>
<script type="text/x-tmpl" id="tmpl-deduplication-render-inpress">
	<fmt:message key="jsp.deduplication.render.inpress"/>
</script>
<script type="text/x-tmpl" id="tmpl-deduplication-render-status-draft">
	<fmt:message key="jsp.deduplication.render.status.draft"/>	
</script>

<script type="text/x-tmpl" id="tmpl-deduplication-render-status-workflow">
	<fmt:message key="jsp.deduplication.render.status.workflow"/>
</script>

<script type="text/x-tmpl" id="tmpl-deduplication-render-status-final">
	<fmt:message key="jsp.deduplication.render.status.final"/>
</script>

<script type="text/x-tmpl" id="tmpl-deduplication-render-status-withdrawn">
	<fmt:message key="jsp.deduplication.render.status.withdrawn"/>
</script>

<script type="text/x-tmpl" id="tmpl-deduplication-render-wstatus-pool1">
	<fmt:message key="jsp.deduplication.render.wstatus.pool1"/>
</script>

<script type="text/x-tmpl" id="tmpl-deduplication-render-wstatus-step1">
	<fmt:message key="jsp.deduplication.render.wstatus.step1"/>
</script>

<script type="text/x-tmpl" id="tmpl-deduplication-render-wstatus-pool2">
	<fmt:message key="jsp.deduplication.render.wstatus.pool2"/>
</script>

<script type="text/x-tmpl" id="tmpl-deduplication-render-wstatus-step2">
	<fmt:message key="jsp.deduplication.render.wstatus.step2"/>
</script>

<script type="text/x-tmpl" id="tmpl-deduplication-render-wstatus-pool3">
	<fmt:message key="jsp.deduplication.render.wstatus.pool3"/>
</script>

<script type="text/x-tmpl" id="tmpl-deduplication-render-wstatus-step3">
	<fmt:message key="jsp.deduplication.render.wstatus.step3"/>
</script>

<script type="text/x-tmpl" id="tmpl-deduplication-render-wstatus-unknown">
	<fmt:message key="jsp.deduplication.render.wstatus.unknwon"/>
</script>

<script type="text/x-tmpl" id="tmpl-deduplication-actions">
		<div class="btn-group" id="myDedupActionsGroup">
		    <button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown">
    			<i class="fa fa-cog"></i><span class="caret"></span>
  			 </button>
			<ul class="dropdown-menu dropdown-menu-right" role="menu">			
				{% for (var i=0; i < o.actions.length; i++) { %}
					<li><a href="#" data-contextPath="<%= request.getContextPath() %>" data-action="{%= o.actions[i] %}" 
							data-itemId="{%=o.itemId %}" class="{%= o.actions[i] %}" data-editItemId="<%= item.getID() %>">{%# o.label[o.actions[i]] %}</a></li>
				{% }%}	
			</ul>
		</div>
</script>

<script type="text/x-tmpl" id="tmpl-deduplication-statuslabel">
	<fmt:message key="jsp.deduplication.label.status"/>
</script>

<script type="text/x-tmpl" id="tmpl-deduplication-submitternote">
{% if (o) { %}	
<div class="alert alert-warning">
	<fmt:message key="jsp.deduplication.label.submitter.note"/>{%= o %}		
</div>
 {% } %} 
</script>
