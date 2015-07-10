<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="jdynatags" prefix="dyna"%>
<c:set var="root"><%=request.getContextPath()%></c:set>
<c:if test="${!empty anagraficaObject.anagrafica4view['orcid']}">
<script type="text/javascript">
j.ajax({
	url: "<%= request.getContextPath() %>/json/orcidqueue",
	data: {
		"id": "${entity.id}"
	},
	success : function(info) {
		if (info == null)
		{
			alert("ERROR 500 - An error has occurred on server-side")
		}
		else
		{
			if(info.status == false) {
				j('#result-list').hide();
				j('#empty-result').show();				
			}
			else {
				if(info.result != null && info.result.length > 0) {
					j('#result-list').show();
					j('#empty-result').hide();
					j('#result-list').html(" ");
					for (var i=0;i<info.result.length;i++)
					{
						var bt = j('<button class="btn btn-info" type="button">').append(j('#orcidmanualsend').text());
						var par = j('<p class="orcid-result">');
	
						par
								.append(j('<span class="orcid-result-name">').text(info.result[i].name))
								.append(bt);
						j('#result-list').append(par);
						bt.button();
						bt.data({uuid: info.result[i].uuid});
						bt.click(function(){
							
						});
					}
				}
			}
		}
	}
});
</script>
<div class="panel-group" id="${holder.shortName}">
	<div class="panel panel-default">
    	<div class="panel-heading">
    		<h4 class="panel-title">
        		<a data-toggle="collapse" data-parent="#${holder.shortName}" href="#collapseOne${holder.shortName}">
          			${holder.title} 
        		</a></h4>
    	</div>
		<div id="collapseOne${holder.shortName}" class="panel-collapse collapse in">
			<div class="panel-body">	
			<div class="dynaClear">&nbsp;</div>
            <div class="dynaClear">&nbsp;</div>
            <div class="dynaClear">&nbsp;</div>
			<div class="dynaField"></div>								
					
					<div class="col-md-12">
    						<div class="container">
							<c:choose>	
								<c:when test="${!empty anagraficaObject.anagrafica4view['orcid-push-manual'] && anagraficaObject.anagrafica4view['orcid-push-manual'][0].value.object==1}">
									<span class="label label-warning"><fmt:message key="jsp.orcid.custom.box.label.preferences.manual"/></span>
								</c:when>
								<c:otherwise>
									<span class="label label-info"><fmt:message key="jsp.orcid.custom.box.label.preferences.batch"/></span>											
								</c:otherwise>
							</c:choose>
							<hr/>
							<div class="clearfix">&nbsp;</div>
							<div id="result-list" style="display: hidden;"></div>
							<div id="result-list" style="display: hidden;"></div>	
					</div></div>
			</div>
		</div>
	</div>
</div>	
</c:if>