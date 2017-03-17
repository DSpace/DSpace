<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@page import="java.util.Map"%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="org.dspace.app.cris.model.ResearcherPage"%>
<%@ page import="org.dspace.core.ConfigurationManager"%>
<%@page import="org.dspace.app.cris.network.NetworkPlugin"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="researchertags" prefix="researcher"%>
<%@ taglib uri="jdynatags" prefix="dyna"%>

<%
    String networkStarted = (String) request
            .getAttribute("networkStarted");
    String authority = (String) request.getAttribute("authority");
    ResearcherPage researcher = (ResearcherPage) request
            .getAttribute("researcher");
    String[] relations = (String[]) request.getAttribute("relations");
    Map<String, Integer> customMaxDepths = (Map<String, Integer>) request
            .getAttribute("customMaxDepths");    
    Map<String, String> colorsNodes = (Map<String, String>) request
            .getAttribute("colorsNodes");
    Map<String, String> colorsEdges = (Map<String, String>) request
            .getAttribute("colorsEdges");
    String configMaxDepth = (String) request
            .getAttribute("configMaxDepth");
    Integer maxDepth = (Integer) request.getAttribute("maxDepth");
    Boolean showexternal = (Boolean) request
            .getAttribute("showexternal");
    Boolean showsamedept = (Boolean) request
            .getAttribute("showsamedept");
    boolean radiographlayout = ConfigurationManager.getBooleanProperty(NetworkPlugin.CFG_MODULE,
            "network.customgraphlayout", true);
    boolean allowexternal = ConfigurationManager.getBooleanProperty(NetworkPlugin.CFG_MODULE,
            "network.allowexternal", true);    
%>
<c:set value="${researcher.dynamicField}" var="anagraficaObject"></c:set>
<c:set value="${authority}" var="authority"></c:set>
<c:set var="messagenodatafound"><fmt:message key="jsp.network.label.nodatafounded"/></c:set>

<html>
<head>
		
	<meta name="author" content="CINECA" />
	<meta http-equiv="X-UA-Compatible" content="IE=edge" />	
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
	
	<meta name="language" content="english"/>
	<meta name="language" content="chinese"/>
	<meta name="description" content="Network of collaboration for ResearcherPage on <c:if test="${!empty researcher.preferredName}">${researcher.preferredName.value}</c:if>, showing contact, bibliometric, and publication list details. Expertise, specialist, collaboration, expert, contract research, media comment."/>

	<meta name="keywords" content="<c:if test="${!empty researcher.preferredName}">${researcher.preferredName.value},</c:if> ${researcher.fullName},<c:if test="${!empty researcher.translatedName}"> ${researcher.translatedName.value}, </c:if>
	<c:forEach items="${researcher.variants}" var="variant">${variant.value}, </c:forEach> bibliometric, bibliometrics, scientometric,
	scientometrics, contact details, publication list, paper list, statistics,
	<c:forEach items="${researcher.anagrafica4view['title']}" var="title">${title.value}, </c:forEach>" />
	
	<meta name="robots" content="index follow"/>
	
	<title>Network of collaboration - ${researcher.fullName}</title>	

	<link href="<%=request.getContextPath()%>/favicon.ico" type="image/x-icon" rel="shortcut icon" />	
	<link href="<%=request.getContextPath()%>/css/researcher.css" type="text/css"  rel="stylesheet" />	
	<link href="<%=request.getContextPath()%>/css/jdyna.css" type="text/css"  rel="stylesheet" />
	
	<link href="<%=request.getContextPath()%>/static/css/bootstrap/bootstrap.min.css" rel="stylesheet">
	<link href="<%=request.getContextPath()%>/static/css/font-awesome/css/font-awesome.min.css" rel="stylesheet">
    <link href="<%=request.getContextPath()%>/css/collaborationNetwork.css" rel="stylesheet" type="text/css" />
	<link type="text/css" href="<%=request.getContextPath()%>/static/css/jquery/ui/all/jquery-ui-latest.custom.css" rel="stylesheet" media="all"/>

	<script type="text/javascript" src="<%= request.getContextPath() %>/javascripts/tmpl.min.js"> </script>
	<script type='text/javascript' src="<%= request.getContextPath() %>/static/js/jquery/jquery-1.10.2.min.js"></script>
	<script type='text/javascript' src='<%= request.getContextPath() %>/static/js/jquery/jquery-ui-1.10.3.custom.min.js'></script>
	<script type='text/javascript' src='<%= request.getContextPath() %>/static/js/bootstrap/bootstrap.min.js'></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/js/jit.js"></script>
    <%--script type="text/javascript" src="<%=request.getContextPath()%>/js/jquery.layout-1.3rc.js"></script--%>
    <script src="<%=request.getContextPath()%>/js/jquery.layout-latest.min.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/js/collaborationNetwork.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/js/jquery.dataTables.min.js"></script>
        
    <!--[if IE]>
    	<script type="text/javascript" src="<%=request.getContextPath()%>/js/excanvas.js"></script>
    <![endif]-->
        
<style type="text/css" media="print">
@page
{
 size: landscape;
 margin: 1cm;
}

#toolbar_print {
	display: none;
}

</style>     

<style type="text/css">
.ui-widget-content a { color: #4EAC2E; }
.ui-layout-pane-west { background: url(<%= request.getContextPath() %>/image/network/background.jpg) left top repeat-x #BDBDBD; border: 0px; overflow-x: hidden;}
.ui-layout-pane-north { background: #ccc; }
.ui-layout-pane-south { background: none; border: 0px; padding: 0px;}
.ui-layout-toggler-south { background: none; border-width: 0px; }
.ui-layout-toggler-south .content { color: #1e6a04; font-size: 16px; -webkit-transform: rotate(90deg); -moz-transform: rotate(90deg); -o-transform: rotate(90deg); transform: rotate(90deg); writing-mode: tb-rl;}
.ui-layout-resizer-west { background: url(<%= request.getContextPath() %>/image/network/background.jpg) left top repeat-x #BDBDBD; border-width: 0px; }
.ui-layout-resizer-south { background: none; border-width: 0px; }
.ui-layout-toggler-west { border-width: 0px; }
.ui-layout-toggler-west .content { background: #fff; color: #1e6a04; font-size: 16px; }

#main-pane { overflow: hidden; }
#network-pane .ui-layout-pane-center { background: none; border: 0px; padding: 0px; }
#network-sliders { margin-top: 5px; border: 1px solid #555; padding: 0px 3px 10px 3px; border-bottom-width: 3px; border-right-width: 2px; }
#network-sliders .sliders-label { padding-bottom: 5px; font-weight: bold; padding-left: 3px; border-bottom: 5px solid #888; margin-bottom: 8px; overflow: hidden; }
#network-sliders .sliders-label > span { float: left; font-size: .8em; font-style: italic; padding-top: 6px; }
#network-sliders .sliders-label > div { float: right; font-size: .6em; }
#network-sliders .slider-label { font-size: .8em; font-style: italic; display: inline-block; vertical-align: top;  }
#network-sliders .slider-label > span { font-weight: bold; }
#network-sliders .slider-option { padding-top: 6px; padding-bottom: 6px; }
#network-sliders .slider-checkbox { display: inline; }
#network-sliders .slider-setup > span { font-size: .8em; font-weight: bold; }
#network-sliders .slider { width: 80%; display: inline-block; }
#network-sliders .sliders-separator { padding: 6px; }
#network-sliders .network-option > div { font-size: .7em; display: inline-block; vertical-align: top; }
#network-sliders .network-option > select { font-size: .8em; width: 100%; }
#network-sliders #sliders-close-button { -webkit-transform: rotate(90deg); -moz-transform: rotate(90deg); -o-transform: rotate(90deg); transform: rotate(90deg); writing-mode: tb-rl; font-size: 16px; cursor: pointer; color: #1e6a04; }
#inner-center { overflow: hidden; }
#inner-center .slider-zoom { position: absolute; }
#slider-zoom-in { position: relative; background: #ccc; padding: 0px 3px 0px 3px; font-weight: bold; cursor: pointer; z-index: 5; }
#slider-zoom { position: relative; z-index: 4; margin-top: -5px; margin-left: 5px; }
#slider-zoom-out { position: relative; background: #ccc; padding: 0px 5px 0px 4px; font-weight: bold; cursor: pointer; z-index: 5; }
.ui-slider-horizontal { height: .4em; }
.ui-slider-horizontal .ui-slider-handle { top: -.5em; width: .3em; }
.ui-slider-vertical { width: .1em; margin-left: 5px; margin-top: -5px; }
.ui-slider-vertical .ui-slider-handle { left: -.6em; height: .3em; background: #000; }
#toolbar_print { float: right; position: relative; z-index: 1; }
#note { width: 200px; }
#note ul li { font-size: .8em; }
#option1 .ui-slider-range { background: #01F33E; }
#option2 .ui-slider-range { background: #5757FF; }
#option3 .ui-slider-range { background: #F70000; }
#option4 .ui-slider-range { background: #BABA21; }
#option5 .ui-slider-range { background: #800080; }
#option6 .ui-slider-range { background: #1F88A7; }
#profiletarget { overflow: hidden; }
#main-rp, #target-rp, #target-common { border: 3px solid #eee; border-bottom: 3px solid #888; border-right: 1px solid #777; padding-bottom: 5px; background-color: #fff; border-top-width: 0; }
.rp-label { background: #465661; color: #fff; font-weight: bold; padding: 1px; }
.rp-header { overflow: hidden; }
.rp-image { display: block; margin-left: auto; margin-right: auto; text-align: center; }
.rp-image img { width: 40px; }
.rp-content { font-size: .8em; }
.rp-name { text-align: center; color: #cf3118; font-weight: bold; }
.rp-title { } 
.rp-dept { overflow: hidden; }
.rp-dept > span { float: left; font-style: italic; font-weight: bold; }
.rp-dept > ul { list-style-type: none; margin: 0px; padding: 0px; }
.rp-dept li { margin-left: 34px; }
.rp-dept-main li a { margin-right: 0px; }

#infovis-label .node { font-family: calibri, serif; font-size: 0.75em; }
#infovis-label .node:hover { font-weight: bold; font-size: 1em; }
#target-rp { border-left-width: 0px; }
.target-separator { color: #cf3118; text-align: center; padding: 10px; font-weight: bold; }

#inner-center { background-color: white; }

@media print {
	#toolbar_print { display: none; }
	.ui-layout-south { display: none; }
	.slider-zoom { display: none; }	
}
</style>

<script><!--




	var j = jQuery;


	var eventTypeDepth = new Array();
	<%for (String relation : relations)
            {%>
		<%=relation%>json = new Object();		
		eventTypeDepth.push('<%=relation%>');		
	<%}%>
	
	
	var colorsNodes = new Array();
	var colorsEdges = new Array();
	<%for (String relation : relations)
            {%>				
		colorsNodes['<%=relation%>'] = '<%=colorsNodes.get(relation)%>';
		colorsEdges['<%=relation%>'] = '<%=colorsEdges.get(relation)%>';
	<%}%>
	
	
	--></script>

	
	

</head>
<body>

	<div id="main-pain" class="ui-layout-center">
	
	
		<div id="inner-center">
			
			<div class="slider-zoom">
				<div id="slider-zoom-in">+</div>
				<div id="sliderleveldistance"></div>
				<div id="slider-zoom-out">-</div>
			</div>			
            <div id="toolbar_print" class="btn-toolbar btn-group" role="toolbar">
				<%--a style='display:none; position: relative;' href='javascript:screenPage()' id='screen'>
					<img id='imgscreen' alt='Return to visualization' title='Return to visualization'  src='../image/window_size.png'/>
				</a--%>
				<button type="button" class="btn btn-default"
                                        href="javascript:void(0)" id="printWithoutWest">
                                        <span class="fa fa-print" 
                                        alt="Print without name card" title="Print without name card">
                                        </span>
                                </button>
                                <button type="button" class="btn btn-default"
                                        href="javascript:void(0)" id="printWithWest">
										<span class="fa fa-print text-success" 
                                        alt="Print with name card" title="Print with name card">
                                        </span>
                                </button>
                                <button type="button" class="btn btn-default" data-toggle="modal" data-target="#networkInformation">
    								<span class="fa fa-question-circle text-info"></span>
  								</button>

			</div>
					<div id="infovis"></div>			
	
		</div>
		
		
		<div id="log">
			<img src="<%=request.getContextPath()%>/image/network/bar-loader.gif" id="loader" />
			<div id="logcontent">
			</div>		
		</div>

		
	</div>

	<div id="network-pane" class="ui-layout-west">
	
	<div class="ui-layout-center" id="ui-no-layout-center">
	<div id="main-rp">


				
					<div class="rp-label"><fmt:message
								key="jsp.network.label.title.focusprofile" /></div>
					<div class="rp-header">
						<div class="rp-image">

										<a target="_blank" href="<%=request.getContextPath()%>/cris/rp/${authority}">
											<img title="A preview ${authority} picture"
												src="researcherimage/${authority}"
												alt="${authority} picture" name="picture" id="picture" onError="this.onerror=null;this.src='<%=request.getContextPath() %>/image/cris/photo_not_available.png'" /> </a>
														

					</div>
					<div class="rp-content">
							<div class="rp-name"><a target="_blank" href="<%= request.getContextPath() %>/cris/rp/${authority}">
							<c:if test="${fn:length(researcher.anagrafica4view['honorific'])>0 && researcher.anagrafica4view['honorific'][0].visibility==1}">
								${researcher.anagrafica4view['honorific'][0].value}
							</c:if>
							${researcher.fullName}</a></div>
							<c:forEach
							items="${researcher.anagrafica4view['title']}" var="title" varStatus="counter">
							<c:if test="${counter.count==1}">
								<div class="rp-title"><strong> ${title.value}</strong></div>
							</c:if>
							</c:forEach>
							<div class="rp-dept rp-dept-main">
							
						<c:set value="false" var="deptvisibility"></c:set>
						<c:forEach items="${researcher.orgUnit}" var="deptvisibilityitem">
						        <c:if test="${deptvisibilityitem.visibility==1}">
						                <c:set value="true" var="deptvisibility"></c:set>
						        </c:if>
						</c:forEach>
						<c:if test="${!empty researcher.orgUnit && deptvisibility==true}">					
												<span><fmt:message
														key="jsp.layout.hku.detail.researcher.department" />
												</span>
												<ul>
						<c:forEach var="dept" items="${researcher.orgUnit}" varStatus="counter">
							<c:if test="${dept.visibility==1}">
													<li>
													
													<c:url var="deptSearch" value="/cris/ou/"/>
													<a target="_blank" href="${deptSearch}${dept.authority}">${dept.value}</a>
						        					<%--span> <input type="checkbox" class="deptvisualizationprofile" id="deptvisualizationprofile" value="${researcher:encode(dept.value,'UTF-8')}"/><label
														for="deptvisualizationprofile">See DGraph</label> </span> --%>					
													<%--a target="_blank" href="#"><span class="icon-network"><img src='../image/wheel-icon2.jpg' alt="<fmt:message key='jsp.network.label.link.network.dept'/>" title="<fmt:message key='jsp.network.label.link.network.dept'/>"/></span></a--%>
													</li>
							</c:if>
																				
												
						</c:forEach>
						</ul>
												
												
						</c:if>
					
							</div>
						
						</div>
					</div>
				


				
		
		</div>
	
		

		<div id="loadingprofile" style="display: none">
			<img src="<%=request.getContextPath()%>/image/network/bar-loader.gif" id="loadertarget" />
		</div>
		
		<div id="profiletarget">
						
				
		</div>
				
		
		<div id="profiletargettwice">
		
				
		</div>	

		
		</div>
				
		
		<div id="separator-ui">
		 &nbsp;
		</div>
		
							
		<div class="ui-layout-south" id="ui-no-layout-south">

			<div id="network-sliders">
				<div class="sliders-label">
					<span><fmt:message
								key="jsp.network.label.title.innerbox" /></span>
					<div id="sliders-close-button">››</div>
				</div>



			<%
			    for (String relation : relations)
			    {
			%>			
			<div class="slider-option">
			   
			   		<input class="slider-checkbox" type="checkbox" value="<%=relation%>" id="<%=relation%>check" />
			   
					<c:set var="rrr">jsp.network.label.showrelationsby.<%=relation%></c:set>					
					<div class="slider-label checkspan" id="<%=relation%>checkonly"><fmt:message key="${rrr}"/>
					<span><input size="3" type="text" id="amount<%=relation%>"
					class="amountother"
					style="border: 0; color: #f6931f; font-weight: bold;  font-style: italic; background: transparent;" /></span>
					</div>
																						
				<div class="slider-setup">
					<span></span>
					<div id="slider<%=relation%>" class="sliderclass"></div>
					<span></span>
				</div>
						
			</div>
			
			<%
				}
			%>



				<div class="sliders-separator"></div>
<%  if (allowexternal) { %>
				<div class="network-option">
					<input type="checkbox" name="radio" class="radio network-checkbox" <%if (showexternal) {%> checked="checked" value="true"<%} else {%> value="false" <% } %> />
					<div><fmt:message key="jsp.network.label.mainconfiguration.showexternalresearcher"/></div>
				</div>
<% }%>
				<div class="network-option" style="display: none;">
					<input type="checkbox" name="radiodept" class="radiodept network-checkbox" <%if (showsamedept) {%> checked="checked" value="true"<%} else {%> value="false" <% } %>/>
					<div><fmt:message key="jsp.network.label.mainconfiguration.showsamedept"/></div>
				</div>
				<div class="network-option">
					<select name="radiographlayout">
						<option value="true" id="radiographlayout1" <%if (radiographlayout) {%> checked="checked" <%}%>><fmt:message key="jsp.network.label.mainconfiguration.radiographlayout.value.custom"/></option>
						<option value="false" id="radiographlayout2" <%if (!radiographlayout) {%> checked="checked" id="radiographlayout2-checked" <%}%>><fmt:message key="jsp.network.label.mainconfiguration.radiographlayout.value.default"/></option>						
					</select>
				</div>
				
				
			</div>


		</div>
		
		
		<div id="open-no-layout-south" style="display: none;">		
		<div id="open-no-layout-south-internal" style="position:absolute; padding: 0px; margin: 0px; font-size: 1px; text-align: left; overflow: hidden; z-index: 2; bottom: 10px; cursor: pointer; display: block; visibility: visible; width: 180px; height: 16px; left: 118px;" class="ui-layout-resizer ui-layout-resizer-south ui-layout-resizer-closed ui-layout-resizer-south-closed" title="Slide Open">		
		<div class="ui-layout-toggler ui-layout-toggler-south ui-layout-toggler-closed ui-layout-toggler-south-closed" style="display: block; padding: 0px; margin: 0px; overflow: hidden; text-align: center; font-size: 1px; cursor: pointer; z-index: 1; visibility: visible; width: 16px; height: 16px; left: 100px; top: 0px;" title="Open">
			<span class="content" id="content-closed" style="display: block; margin-left: 0px;">‹‹</span>
		</div>
		</div>
		</div>
				
		</div>
		
		
	

</body>

	<script type="text/javascript">
	<!-- 
	ajaxurlprofile = "<%=request.getContextPath()%>/networkdataprofile/${authority}"
	var myLayout;
	var networkPane;

	<%if (networkStarted == null)
            {%>

    j("#log").dialog({closeOnEscape: false, modal: true, autoOpen: false, resizable: false, open: function(event, ui) { j(".ui-dialog-titlebar").hide();}});
    j("#log").dialog("open");		
	Log.write("${messagenodatafound}");
 <%}
            else
            {%>
	
	j("#log").dialog({closeOnEscape: true, modal: true, autoOpen: false, resizable: false,
		   open: function(event, ui) { j(".ui-dialog-titlebar").hide();}});

	
	
	function initJSON(rp, network) {

		j("#log").dialog("open");			
		Log.write("Loading... network: " + network);
		var reload = 0;
		var showEXT = j("input:checkbox[name=radio]").val();
		var showSAMEDEPT = j("input:checkbox[name=radiodept]").val();
		var jqxhr = j.getJSON("<%=request.getContextPath()%>/json/collaborationnetwork",
		  {
		    connection: network,		    
		    researcher: rp,
		    showexternal: showEXT,
		    showsamedept: showSAMEDEPT
		    
		  },
		  function(data) {					   
			  var lim = data.length;
<%--if (lim == 1 && showEXT == 'false') {
	j("input:checkbox[name=radio]").click();
	j("#log").dialog("close");
	return;
}--%>
			  for (var i = 0; i < lim; i++){			    
			       data[i].data.color = colorsNodes[network];
			       data[i].data.$color = colorsNodes[network];
			       for(var y = 0; y < data[i].adjacencies.length; y++) {
			    	   data[i].adjacencies[y].data.color = colorsEdges[network];
			    	   data[i].adjacencies[y].data.$color = colorsEdges[network];
			       }
			  }			  
			  init(data, rp, network, "rp");			  
			  <%for (String relation : relations)
                {%> 
				if(network=='<%=relation%>') {	
					addLocalJson({"<%=relation%>" : data});
				}
			<%}%>
			  j("#log").dialog("close");
			  						
		  });
	}
	
	
	
	<%=networkStarted%>json = initJSON('<%=authority%>', '<%=networkStarted%>');
		
	<%}%>

	
		
	j(document).ready(function () {
		// this layout could be created with NO OPTIONS - but showing some here just as a sample...
		// myLayout = $('body').layout(); -- syntax with No Options

		myLayout = j('body').layout({ 
			applyDefaultStyles: false			
			, resizable: false
			, west__size: "250"
			, west__spacing_open: 16
			, west__spacing_closed: 16
			, west__togglerLength_open: "16"
			, west__togglerLength_closed: "16"
			, west__togglerAlign_open: "top"
			, west__togglerAlign_closed: "top"
			, west__togglerContent_open: "&#8249;&#8249;"
			, west__togglerContent_closed: "&#8250;&#8250;" 
		});
		

	});
	
	
	 j('#sliders-close-button').click(function() {
	        j('#ui-no-layout-south').hide();
	        j('#open-no-layout-south').show();	   
	        var a = j('#ui-no-layout-center').innerHeight();			
		    var b = j('#network-pane').height();
		    if((b-a) > 0) {
		    	j('#separator-ui').css("height",b-a-10);		    	
		    }
		    else {
		    	j('#separator-ui').css("height","auto");
		    }
	 });
	 j('#content-closed').click(function() {
		 
		 	j('#ui-no-layout-south').show();        
	        j('#open-no-layout-south').hide();
	    
	    	var a = j('#ui-no-layout-center').innerHeight();
			var c = j('#ui-no-layout-south').innerHeight();
		    var b = j('#network-pane').height();
		    if((b-a-c) > 0) {
		    	j('#separator-ui').css("height",b-a-c);		    	
		    }
		    else {
		    	j('#separator-ui').css("height","auto");
		    }
		    	    
	 });
	 
	function addJSON(rp, network) {

		j("#log").dialog("open");			
		Log.write("Loading... network: " + network);
		var showEXT = j("input:checkbox[name=radio]").val();
		var showSAMEDEPT = j("input:checkbox[name=radiodept]").val();
		
		var jqxhr = j.getJSON("<%=request.getContextPath()%>/json/collaborationnetwork",
		  {
		    connection: network,		    
		    researcher: rp,
		    showexternal: showEXT,
		    showsamedept: showSAMEDEPT
		  },
		  function(data) {	  	  
			  var lim = data.length;
			  for (var i = 0; i < lim; i++){			    
			       data[i].data.color = colorsNodes[network];
			       data[i].data.$color = colorsNodes[network];
			       for(var y = 0; y < data[i].adjacencies.length; y++) {
			    	   data[i].adjacencies[y].data.color = colorsEdges[network];
			    	   data[i].adjacencies[y].data.$color = colorsEdges[network];
			       }
			  }		
				rgraph.op.sum(data, {  
				   type: 'fade:con',  
				   duration: 1500  
				});
				rgraph.refresh();
				
				<%for (String relation : relations)
            {%> 
					if(network=='<%=relation%>') {				
						addLocalJson({"<%=relation%>" : data});
					}
				<%}%>
				
			  j("#log").dialog("close");	
			  
			  return data;
		  });
			
		
	}
	
	
	function checkOnlyWithJSON(rp, network) {

		j("#log").dialog("open");			
		Log.write("Loading... network: " + network);
		var showEXT = j("input:checkbox[name=radio]").val();
		var showSAMEDEPT = j("input:checkbox[name=radiodept]").val();
		
		var jqxhr = j.getJSON("<%=request.getContextPath()%>/json/collaborationnetwork",
		  {
		    connection: network,		    
		    researcher: rp,
		    showexternal: showEXT,
		    showsamedept: showSAMEDEPT
		  },
		  function(data) {		  
			  var lim = data.length;
			  for (var i = 0; i < lim; i++){			    
			       data[i].data.color = colorsNodes[network];
			       data[i].data.$color = colorsNodes[network];
			       for(var y = 0; y < data[i].adjacencies.length; y++) {
			    	   data[i].adjacencies[y].data.color = colorsEdges[network];
			    	   data[i].adjacencies[y].data.$color = colorsEdges[network];
			       }
			  }			  
			  rgraph.loadJSON(data);
			  reloadLocalJson({network:data});		  
			  rgraph.refresh();
			  
			  j("#log").dialog("close");		
			  
			  return data;
		  });
			
		
	}
	
	j(document).ready(function () {
	
		var a = j('#ui-no-layout-center').innerHeight();
		var c = j('#ui-no-layout-south').innerHeight();
	    var b = j('#network-pane').height();
	    if((b-a-c-60) > 0) {
	    	j('#separator-ui').css("height",b-a-c-60);		    	
	    }
	    	    
	    
	customRGraph = <%=radiographlayout%>;
	

	j(".radio").click(function(){		
		
		var showEXT = j("input:checkbox[name=radio]").val();
		if(showEXT=="true") {
			showEXT = false;
		}
		else {
			showEXT = true;
		}
		var showSAMEDEPT = j("input:checkbox[name=radiodept]").val();
		location.href = "<%=request.getContextPath()%>/cris/network/${authority}?showexternal=" + showEXT + "&showsamedept=" + showSAMEDEPT;
		return true;		
		
	});	
		
	j(".radiodept").click(function(){		
		
		var showEXT = j("input:checkbox[name=radio]").val();
		var showSAMEDEPT = j("input:checkbox[name=radiodept]").val();
		if(showSAMEDEPT=="true") {
			showSAMEDEPT = false;
		}
		else {
			showSAMEDEPT = true;
		}
		location.href = "<%=request.getContextPath()%>/cris/network/${authority}?showexternal=" + showEXT + "&showsamedept=" + showSAMEDEPT;
		return true;		
		
	});	

	j("select[name='radiographlayout']").change(function(){
		if (j("#radiographlayout1").is(":selected") && !customRGraph) { customRGraph = true; rgraph.refresh(); }
		if (j("#radiographlayout2").is(":selected") && customRGraph) { customRGraph = false; rgraph.refresh(); }
	});
	
	
	<%for (String relation : relations)
            {%>
			
		
	
	j("#<%=relation%>check").click(function(){
				
		if(this.checked) {
			
			if(j.isEmptyObject(<%=relation%>json)) {	
				
				<%=relation%>json = addJSON('<%=authority%>', '<%=relation%>');			
				
			}
			else {				
				j("#log").dialog("open");		
				Log.write("Loading (from cache)... network: " + '<%=relation%>');
				rgraph.op.sum(<%=relation%>json, {  
				   type: 'fade:con',  
				   duration: 1500  
				});
				addLocalJson({"<%=relation%>" : <%=relation%>json});
				
				j("#log").dialog("close");		
			}
			
			
		}
		else {
			var n = j(".slider-checkbox:checked").length;
			if(n==0) {
				j("#<%=relation%>check").attr('checked','checked');
			}
			else {
				othernetwork = new Array();				
				removeLocalJson("<%=relation%>");	
			}					
			
		}
		j('#slider<%=relation%>').slider("option", "disabled", !j('#<%=relation%>check').is(':checked'));
	});
	
	j("#<%=relation%>checkonly").dblclick(function(){
		
		othernetwork = new Array();
		bezierRatioSettings = new Array();
		if(j.isEmptyObject(<%=relation%>json)) {	
			<%=relation%>json = checkOnlyWithJSON('<%=authority%>', '<%=relation%>');
		}
		else {
			j("#log").dialog("open");
			Log.write("Loading (from cache)... network: " + '<%=relation%>');
			reloadLocalJson({"<%=relation%>":<%=relation%>json});
			rgraph.loadJSON(<%=relation%>json);
			rgraph.refresh();
			j("#log").dialog("close");
		}
		
		j('#<%=relation + "check"%>').attr('checked','checked');
		
		<%for (String r : relations)
                {

                    if (r != relation)
                {%>
						j('#<%=r + "check"%>').attr('checked','');			
				<%}%>
				j('#slider<%=r%>').slider("option", "disabled", !j('#<%=r%>check').is(':checked'));  
         <%}%>
		
		
	});
	<%}%>
	
	
	});	
	
	j(function() {
					
		j( "#sliderleveldistance" ).slider({
			orientation: "vertical",
			min: 20,
			max: 400,
			value: 120,
			slide: function( event, ui ) {
				if (ui.value <= 45) return false;
				rgraph.config.levelDistance = ui.value;				
				rgraph.refresh();
			}
		});
		
		j( "#slider" ).slider({
			orientation: "horizontal",
			range: "min",
			min: 0,
			max: <%=configMaxDepth%>,
			value: <%=maxDepth%>,
			slide: function( event, ui ) {
				j( "#amount" ).val( ui.value );
				
				//j( ".sliderclass" ).slider( "option", "max", ui.value);
				j( ".sliderclass" ).slider( "option", "value", ui.value);								
				j( ".amountother" ).val( ui.value );
				
				rgraph.refresh();
			}
		});
		j( "#amount" ).val( j( "#slider" ).slider( "value" ) );
		
		j('#<%=networkStarted + "check"%>').attr('checked','checked');
				
		<%for (String relation : relations)
            {%>
		j( "#slider<%=relation%>").slider({			
			orientation: "horizontal",
			range: "min",
			min: 0,
			max: <%=customMaxDepths.get(relation)%>,
			value: <%=configMaxDepth%>,
			slide: function( event, ui ) {
				j( "#amount<%=relation%>").val( ui.value );				
				rgraph.refresh();
			}
		});
		j( "#amount<%=relation%>").val( j( "#slider<%=relation%>").slider( "value" ) );		
		j("#slider<%=relation%> .ui-slider-range").css('background', '<%=colorsNodes.get(relation)%>');
		j("#slider<%=relation%> .ui-slider-handle").css('background-color', '<%=colorsNodes.get(relation)%>');
		j("#slider<%=relation%>").slider("option", "disabled", !j('#<%=relation + "check"%>').is(':checked'));
		<%}%>
	});	
	


	

	j(function() {

		  var $slideMe = j("<div/>")
		                    .css({ position : 'absolute' , top : 10, left : 0}).css('background-color', 'white').css('white-space', 'nowrap')
		                    .text("Zoom in/out")
		                    .hide()


		  j("#sliderleveldistance").slider()
		                .find(".ui-slider-handle")
		                .append($slideMe)
		                .hover(function()
		                        { $slideMe.show()}, 
		                       function()
		                        { $slideMe.hide()}
		                )

	});
	
	function screenPage() {		
		
		j('#screen').hide();		
		
		rgraph.config.levelDistance = 200;
		rgraph.canvas.resize(winW, winH);
		rgraph.canvas.translate(-(rgraph.canvas.getSize().width)/6, 0);
		rgraph.refresh();
		
		var width = winW + "px";
		j('#infovis').css("width",width);
		j('#infovis').css("margin","auto");
		myLayout.open("west");		
		
	}	
	
	
	function printPageWithWest() {		
		printIt(true);
	}
	
	function printPageWithoutWest() {	
		printIt(false);
	}
	
	function printIt(withWest) {
		var isWestClosed = myLayout.state.west.isClosed;
		if (withWest == isWestClosed) {
			myLayout.toggle("west");
		}
		window.print();
		if (isWestClosed != myLayout.state.west.isClosed) {
			myLayout.toggle("west");
		}

/*		j('#screen').show();
		
		rgraph.config.levelDistance = 100;
		
		
		rgraph.canvas.resize(900, 640);				
		if(isWestOpened) {
			myLayout.open("west");	
			rgraph.canvas.translate(-(rgraph.canvas.getSize().width)/8, 0);
		}	
		else {
			myLayout.close("west");	
		}
	
				
		rgraph.refresh();
		
		
		j('#infovis').css("width","900px");
		j('#infovis').css("margin","0");
		
		setTimeout(function(){
				window.print();
		},2000);*/
		
	}

	j( "#toolbar_print" ).buttonset();

	j("#printWithoutWest").click(function() {
		printPageWithoutWest()
	});
	j("#printWithWest").click(function() {
		printPageWithWest()
	});


    j('#slider-zoom-in').click(function() {
        var i = j('#sliderleveldistance').slider("value");
        j('#sliderleveldistance').slider("option", "value", i + 10);
        rgraph.config.levelDistance = i + 10;
        rgraph.refresh();
    });

    j('#slider-zoom-out').click(function() {
        var i = j('#sliderleveldistance').slider("value");
	if (i <= 50) return;
        if (i > 10) { 
        	j('#sliderleveldistance').slider("option", "value", i - 10);
        	rgraph.config.levelDistance = i - 10;
        }
        rgraph.refresh();
    });
    

	-->    
	</script>
	<script type="text/x-tmpl" id="tmpl-networkimage">
		<div class='tip-title'><img style='width: 40px' id='picture' name='picture' src='researcherimage/{%= o.id %}' onError="this.onerror=null;this.src='<%=request.getContextPath() %>/image/cris/photo_not_available.png'"/><span>{%= o.name %}</span></div>
	</script>


<div class="modal fade" id="networkInformation">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
        <h4 class="modal-title"><fmt:message key="jsp.network.label.title.info"/></h4>
      </div>
      <div class="modal-body">
        <p>
        <ul style="padding-left: 15px;">
			<li>Best printed with IE/FireFox</li>
			<li>Because of space limitations, approximately 20 names only appear in the first circle for any given facet.</li>
			<% if (allowexternal) { %>
			<li>External name variants have not been merged.</li>
			<% } %>		</ul>        
        </p>
      </div>
    </div><!-- /.modal-content -->
  </div><!-- /.modal-dialog -->
</div><!-- /.modal -->

	
<div class="modal fade dialogfragment" id="relationfragment">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
        <h4 class="modal-title" id="relationfragmentcontenttitle"><fmt:message key="jsp.network.label.title.info"/></h4>
      </div>
      <div class="modal-body">
        <div id="relationfragmentcontent">
				
		</div>
      </div>
    </div><!-- /.modal-content -->
  </div><!-- /.modal-dialog -->
</div><!-- /.modal -->

<div class="modal fade dialogfragment" id="relationfragmenttwice">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
        <h4 class="modal-title" id="relationfragmenttwicecontenttitle"><fmt:message key="jsp.network.label.title.info"/></h4>
      </div>
      <div class="modal-body">
        <div id="relationfragmenttwicecontent">
				
		</div>
      </div>
    </div><!-- /.modal-content -->
  </div><!-- /.modal-dialog -->
</div><!-- /.modal -->

</html>
