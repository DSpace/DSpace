<?xml version="1.0" encoding="UTF-8"?>
<!-- 

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

	Developed by DSpace @ Lyncode <dspace@lyncode.com> 
	Following OpenAIRE Guidelines 1.1:
		- http://www.openaire.eu/component/content/article/207

 -->
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:doc="http://www.lyncode.com/xoai">
	<xsl:output indent="yes" method="xml" omit-xml-declaration="yes" />
	
	<xsl:include href="driver-commons.xsl"/>
	
	<xsl:template match="/doc:metadata">
	  <doc:metadata>
	  	
	  	<xsl:variable name="subtype" select="doc:element[@name='sedici']/doc:element[@name='subtype']/doc:element/doc:field/text()"/>
		<xsl:variable name="context" select="'openaire'"/>
	  	
	  	<xsl:call-template name="sedici-identifier">
			<xsl:with-param name="handle" select="doc:element[@name='others']/doc:field[@name='handle']/text()"/>
			<xsl:with-param name="context-name" select="$context"/>
		</xsl:call-template>
	  	
	  	<xsl:call-template name="accessRightsAndEmbargo" >
			<xsl:with-param name="liftDate" select="doc:element[@name='sedici']/doc:element[@name='embargo']/doc:element[@name='liftDate']/doc:element/doc:field/text()"/>
			<xsl:with-param name="context-name" select="$context"/>
		</xsl:call-template>
		
		<xsl:call-template name="driver-type">
			<xsl:with-param name="subtype" select="$subtype"/>
		</xsl:call-template>
		
		<xsl:call-template name="driver-version">
			<xsl:with-param name="subtype" select="$subtype"/>
		</xsl:call-template>
	  
	  	<xsl:apply-templates select="@*|node()" />
	  
	  </doc:metadata>
	</xsl:template>
	
	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" />
		</xsl:copy>
	</xsl:template>
	
	<!-- Silenciamos cualquier tipo de dc.format ya que no cumplen con el vocabulario para mimetype de IANA. Es una práctica recomendada por OpenAIRE.-->
	<xsl:template match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='format']/doc:element"/>
	
	<!-- Publication Reference - Se transforma el valor del metadato sedici.relation.isReviewOf para que aparezca como info:eu-repo/semantics/reference/url/<identifier>. Éste metadato es "propio" de la obra, ya que la obra hace referencia <<explícita>> a otra obra.-->
 	<xsl:template match="/doc:metadata/doc:element[@name='sedici']/doc:element[@name='relation']/doc:element[@name='isReviewOf']/doc:element/doc:field/text()">
		<xsl:call-template name="publication-reference">
			<xsl:with-param name="schema" select="'url'"/>
			<xsl:with-param name="identifier" select="."/>
		</xsl:call-template>
	</xsl:template>

	<!-- Prefixing dc.type -->
<!-- 	<xsl:template match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field/text()"> -->
<!-- 		<xsl:call-template name="addPrefix"> -->
<!-- 			<xsl:with-param name="value" select="." /> -->
<!-- 			<xsl:with-param name="prefix" select="'info:eu-repo/semantics/'"></xsl:with-param> -->
<!-- 		</xsl:call-template> -->
<!-- 	</xsl:template> -->
	

	<!-- AUXILIARY TEMPLATES -->
	
	<!-- Publication-reference prefix -->
	<xsl:template name="publication-reference">
		<xsl:param name="schema"/>
		<xsl:param name="identifier"/>
		<xsl:variable name="prefix" select="'info:eu-repo/semantics/reference'"/>
		<xsl:value-of select="concat($prefix,'/',$schema,'/',$identifier)"/>
	</xsl:template>
	
	<!-- dc.type prefixing -->
<!-- 	<xsl:template name="addPrefix"> -->
<!-- 		<xsl:param name="value" /> -->
<!-- 		<xsl:param name="prefix" /> -->
<!-- 		<xsl:choose> -->
<!-- 			<xsl:when test="starts-with($value, $prefix)"> -->
<!-- 				<xsl:value-of select="$value" /> -->
<!-- 			</xsl:when> -->
<!-- 			<xsl:otherwise> -->
<!-- 				<xsl:value-of select="concat($prefix, $value)" /> -->
<!-- 			</xsl:otherwise> -->
<!-- 		</xsl:choose> -->
<!-- 	</xsl:template> -->
</xsl:stylesheet>
