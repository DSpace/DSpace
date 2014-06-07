<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>

<hr />
<div class="box">
<div class="box-label">Subscribe menu</div>
<div class="box-content">


	<div class="subscription-<c:if test="${classcurrent==0}">current</c:if>"><a href="<%= request.getContextPath() %>/subscribe"><fmt:message key="jsp.layout.navbar-hku.item-subscription"/></a></div>
	<div class="subscription-<c:if test="${classcurrent==1}">current</c:if>"><a href="<%= request.getContextPath() %>/cris/tools/stats/subscription/list.htm"><fmt:message key="jsp.layout.navbar-hku.stat-subscription"/></a></div>


</div>
</div>
<hr />

