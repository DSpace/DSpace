<%@ attribute name="data" required="true" type="java.lang.Object"%>
<%@ attribute name="statType" required="true"%>
<%@ attribute name="objectName" required="true"%>
<%@ attribute name="pieType" required="true"%>
<%@ attribute name="useLocalMap" required="false" %>
<%@ attribute name="useFmt" required="false" %>
<%@ taglib uri="statstags" prefix="stats" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<div class="table-responsive">
		<table class="table table-bordered datatable-mostviewed">
			<thead>
				<tr>
					<th><fmt:message key="view.${data.jspKey}.data.${statType}.${objectName}.${pieType}.title" />
					</th>
					<th><fmt:message key="view.${data.jspKey}.data.${statType}.${objectName}.${pieType}.value" /></th>
				</tr>
			</thead>
				<tbody>
				<c:set var="total" value="0" />
				<c:forEach items="${data.resultBean.dataBeans[statType][objectName][pieType].limitedDataTable}" var="row" end="${data.maxListMostViewedItem}" varStatus="status">
				<c:set var="id" scope="page">${statType}_${objectName}_${pieType}_${status.count}</c:set>
					<tr>
						<td>
						<%-- <a href="javascript:drillDown('<c:out value="${row.label}"/>',${itemId},'drillDown-${pieType}', 'drillDownDiv','${data.jspKey}');">--%>	
								<c:choose>
								<c:when test="${useLocalMap}">
									<c:choose>
										<c:when test="${objectName eq 'geo' || pieType eq 'sectionid'}">
											<%-- Warning - (ONLY CRIS entities), the label could't showed because properties definition have no label --%>
											ID: ${row.label} <c:if test="${!empty data.label[objectName][row.label].label}">- ${data.label[objectName][row.label].label}</c:if>
										</c:when>
										<c:otherwise>										
											<c:choose>
												<c:when test="${!empty data.label[objectName][row.label].handle}">
												<c:choose>
													<c:when test="${data.label[objectName][row.label].type>=9}">
														<a href="<%= request.getContextPath() %>/cris/uuid/${data.label[objectName][row.label].handle}">ID: ${row.label} - ${data.label[objectName][row.label].name}</a>
													</c:when>												
													<c:otherwise>													
														<a href="<%= request.getContextPath() %>/handle/${data.label[objectName][row.label].handle}">ID: ${row.label} - ${data.label[objectName][row.label].name}</a>
													</c:otherwise>
												</c:choose>
												</c:when>
												<c:otherwise>																								
													ID: ${row.label} - ${data.label[objectName][row.label].name} <c:if test="${objectName eq 'bitstream' && !(pieType eq 'category')}"> - ITEM:( <a href="<%= request.getContextPath() %>/handle/${data.label[row.label]['handle']}">${data.label[row.label]['handle']}</a>) </c:if>
												</c:otherwise>
											</c:choose>
										</c:otherwise>
									</c:choose>
									
								</c:when>
								<c:when test="${useFmt}">
									<c:choose>
										<c:when test="${row.label eq 'Unknown'}">
											<fmt:message key="statistics.table.value.${pieType}.${row.label}" />
										</c:when>
										<c:otherwise>
											${row.label} - <fmt:message key="statistics.table.value.${pieType}.${row.label}" />
										</c:otherwise>
									</c:choose>
								</c:when>
								<c:otherwise>
									<c:choose>
										<c:when test="${row.label eq 'Unknown'}">
											<fmt:message key="statistics.table.value.${pieType}.${row.label}" />
										</c:when>
										<c:otherwise>
											<c:out value="${row.label}"/>
										</c:otherwise>
									</c:choose>
									
								</c:otherwise>
								</c:choose>
						<%-- 	</a>--%>
						</td>
						<td>
							<c:out value="${row.value}"/>
						</td>
					</tr>
					<c:set var="total" value="${total + row.value}" />
				</c:forEach>
				</tbody>
				<tfoot>
					<tr class="evenRowOddCol">
						<th scope="row"><fmt:message key="statistics.table.value.${pieType}.total" /></th>
						<td id="totalItemView"><c:out value="${total}" /></td>
					</tr>
				</tfoot>							
		</table>
</div>