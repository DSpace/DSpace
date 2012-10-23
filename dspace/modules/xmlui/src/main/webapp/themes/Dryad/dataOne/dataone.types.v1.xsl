<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fn="http://www.w3.org/2005/02/xpath-function" 
	xmlns:d1="http://ns.dataone.org/service/types/v1"
	version="1.0">
	
	<!-- these are the types we want to render -->
	<xsl:import href="nodeList.v1.xsl"/>
	<xsl:import href="objectList.v1.xsl"/>
	<xsl:import href="objectFormatList.v1.xsl"/>

	<xsl:output method="html" encoding="UTF-8" indent="yes" />
	
	<!-- depending on the deployment, this allows us to locate the resources -->
	<xsl:param name="pathPrefix">/themes/Dryad/</xsl:param>
		
	<xsl:template match="/">
		<html>
		
			<xsl:call-template name="documenthead"/>
			
			<body onload="initTabs()">
		
				<xsl:call-template name="bodyheader"/>
				    
					<div id="content">
						<!-- place holder for tabs-->
						<ul></ul>
					                        
                   		<xsl:if test="*[local-name()='nodeList']">
                   			<div id="nodeList">
                            	<xsl:call-template name="nodeList"/>
                            </div>	
                        </xsl:if>
                        <xsl:if test="*[local-name()='node']">
                   			<div id="node">
                   				<xsl:for-each select="*[local-name()='node']">
									<xsl:call-template name="node" />
								</xsl:for-each>
                            </div>	
                        </xsl:if>
                        <xsl:if test="*[local-name()='objectList']">
                        	<div id="objectList">  	
                            	<xsl:call-template name="objectList"/>
                            </div>	
                        </xsl:if>                        
                        <xsl:if test="*[local-name()='objectFormatList']">
                        	<div id="objectFormatList"> 	
                            	<xsl:call-template name="objectFormatList"/>
                           	</div>
                        </xsl:if>
                        
					</div>
                        
				<xsl:call-template name="bodyfooter"/>
					
			</body>
			
		</html>
		
	</xsl:template>
	
	<xsl:template name="documenthead">
		<head>
			
			<link type="text/css" rel="Stylesheet" >
				<xsl:attribute name="href">
					<xsl:value-of select="$pathPrefix"/>jquery/jqueryui/css/smoothness/jquery-ui-1.8.16.custom.css
				</xsl:attribute>
			</link>
			<link type="text/css" rel="Stylesheet" >
				<xsl:attribute name="href">
					<xsl:value-of select="$pathPrefix"/>dataone.css
				</xsl:attribute>
			</link>	
			<script>
				<xsl:attribute name="src">
					<xsl:value-of select="$pathPrefix"/>jquery/jquery-1.6.4.min.js
				</xsl:attribute>
			</script>
			<script>
				<xsl:attribute name="src">
					<xsl:value-of select="$pathPrefix"/>jquery/jqueryui/jquery-ui-1.8.16.custom.min.js
				</xsl:attribute>
			</script>
			<script type="text/javascript">
			function initTabs() {
				$(function() {
					$("#content").tabs();
					if ($("#nodeList").is("div")) {
						$("#content").tabs("add", "#nodeList", "Node List");
					}
					if ($("#node").is("div")) {
						$("#content").tabs("add", "#node", "Node Details");
					}
					if ($("#objectList").is("div")) {
						$("#content").tabs("add", "#objectList", "Object List");
					}
					if ($("#objectFormatList").is("div")) {
						$("#content").tabs("add", "#objectFormatList", "Object Format List");
					}
				});
			}
			</script>
			
		</head>
	</xsl:template>
	
	<xsl:template name="bodyheader">
		<!-- dataone logo header -->
		<div class="logoheader">
			<h1></h1>
		</div>
	</xsl:template>
	
	<xsl:template name="bodyfooter">
		<!-- footer -->
	</xsl:template>
	
	
</xsl:stylesheet>
