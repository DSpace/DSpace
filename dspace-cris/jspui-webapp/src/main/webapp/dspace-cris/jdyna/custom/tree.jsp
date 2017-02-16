<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="jdynatags" prefix="dyna"%>
<c:set var="root"><%=request.getContextPath()%></c:set>
	<link href="<%= request.getContextPath() %>/css/jstree/themes/default/style.min.css" type="text/css" rel="stylesheet" />
	<script type="text/javascript" src="<%=request.getContextPath()%>/js/jstree/jstree.min.js"></script>
	<script type="text/javascript" src="<%=request.getContextPath()%>/js/jstree-advanced/jstree.setup.min.js"></script>
		<script type="text/javascript">
	<!--

					JsTree.load('${root}/api/v1/simple/tree/${entity.crisID}',
							'${root}/api/v1/simple/resource', 1, 'data-tree',
							'data-window');

		-->
	</script>
	
<div class="panel-group ${extraCSS}" id="${holder.shortName}">
	<div class="panel panel-default">
		<div class="panel-heading">
			<h4 class="panel-title">
				<a data-toggle="collapse" data-parent="#${holder.shortName}"
					href="#collapseOne${holder.shortName}"> ${holder.title} </a>
			</h4>
		</div>
		<div id="collapseOne${holder.shortName}"
			class="panel-collapse collapse in">
			<div class="panel-body">


				<div class="row">
					<div class="row">
						<div id="data-tree" class="col-md-6" style="padding-top: 20px;"></div>
						<div id="data-window" class="col-md-6" style="padding-top: 20px;"></div>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>
