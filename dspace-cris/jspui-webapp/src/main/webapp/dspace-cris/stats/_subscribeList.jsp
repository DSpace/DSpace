<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@page import="org.dspace.app.cris.model.CrisConstants" %>
<%@page import="org.dspace.app.cris.model.StatSubscription" %>
<div>

<c:set var="RPTYPE"><%= CrisConstants.RP_TYPE_ID %></c:set>
<c:set var="PJTYPE"><%= CrisConstants.PROJECT_TYPE_ID %></c:set>
<c:set var="OUTYPE"><%= CrisConstants.OU_TYPE_ID %></c:set>
<c:set var="FREQUENCY_DAILY"><%= StatSubscription.FREQUENCY_DAILY %></c:set>
<c:set var="FREQUENCY_WEEKLY"><%= StatSubscription.FREQUENCY_WEEKLY %></c:set>
<c:set var="FREQUENCY_MONTHLY"><%= StatSubscription.FREQUENCY_MONTHLY %></c:set>

<display:table name="${subscriptions}" cellspacing="0" cellpadding="0" 
			requestURI="" id="objectList" htmlId="objectList"  class="displaytaglikemisctable" export="false">
			<display:column titleKey="jsp.statistics.table.type" sortable="false">
				<fmt:message key="jsp.statistics.table.type.${objectList.type}" />
			</display:column>							
			<display:column titleKey="jsp.statistics.table.object" property="objectName" sortable="false"/>			
			<display:column titleKey="jsp.statistics.table.identifier" sortable="false">
			<a href="<%= request.getContextPath() %>/<c:choose>
						<c:when test="${objectList.type >= RPTYPE}">cris/uuid/</c:when><c:otherwise>handle/</c:otherwise></c:choose>${objectList.id}">${objectList.id}</a>
			</display:column>						
			<display:column titleKey="jsp.statistics.table.frequences" sortable="false">
				<form action="<%= request.getContextPath() %>/cris/tools/stats/subscription/subscribe" method="get">
					
					<input type="hidden" name="type" value="${objectList.type}" />						
				   	<input type="hidden" name="uid" value="${objectList.id}" />
						
					
					 <input type="hidden" name="list" value="true" />
					<input type="checkbox" name="freq" value="${FREQUENCY_DAILY}"
				    <c:forEach var="freq" items="${objectList.freqs}">    
				     <c:if test="${freq == FREQUENCY_DAILY}">
				      checked="checked"				       
				     </c:if>
				    </c:forEach>
					 />
					 <input type="checkbox" name="freq" value="${FREQUENCY_WEEKLY}" 
 				    <c:forEach var="freq" items="${objectList.freqs}">    
				     <c:if test="${freq == FREQUENCY_WEEKLY}">
				      checked="checked"				       
				     </c:if>
				    </c:forEach>
					 />
					 <input type="checkbox" name="freq" value="${FREQUENCY_MONTHLY}" 
				    <c:forEach var="freq" items="${objectList.freqs}">    
				     <c:if test="${freq == FREQUENCY_MONTHLY}">
				      checked="checked"				       
				     </c:if>
				    </c:forEach>
					/>
					 <input class="btn btn-primary" type="submit" value="<fmt:message key="jsp.statistics.table.button.update" />" />
				</form>
			</display:column>
			<display:column titleKey="jsp.statistics.table.remove" sortable="false">
				<form action="<%= request.getContextPath() %>/cris/tools/stats/subscription/unsubscribe" method="get">
					
					
					 <input type="hidden" name="type" value="${objectList.type}" />					
					 <input type="hidden" name="uid" value="${objectList.id}" />
					 <input type="hidden" name="list" value="true" />
					 <input class="btn btn-warning" type="submit" value="<fmt:message key="jsp.statistics.table.button.remove" />" />
				</form>	
			</display:column>
		</display:table>

</div>