<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:doc="http://www.lyncode.com/xoai"
	version="1.0">
	<xsl:output method="text" />

<xsl:variable name='newline'><xsl:text>
</xsl:text></xsl:variable>
<xsl:variable name='tab'><xsl:text>   </xsl:text></xsl:variable>
	<xsl:template match="/">
		<xsl:choose>
			<xsl:when test="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value']/text() = 'article'">
				<xsl:text>TY - RPRT</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>TY - ABST</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:value-of select="$newline"></xsl:value-of>
			
		<xsl:text>TI - </xsl:text>
		<xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']/text()"></xsl:value-of>
		
		<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value']">
			<xsl:value-of select="$newline"></xsl:value-of>
			<xsl:text>AU - </xsl:text>
			<xsl:value-of select="."></xsl:value-of>
		</xsl:for-each>
			
			<xsl:if test="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']">
				<xsl:value-of select="$newline"></xsl:value-of>
				<xsl:text>PY - </xsl:text>
				<xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']/text()"></xsl:value-of>
			</xsl:if>
			
	</xsl:template>
</xsl:stylesheet>