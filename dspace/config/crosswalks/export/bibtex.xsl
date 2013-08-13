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
				<xsl:text>@article</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>@misc</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:text> { </xsl:text>
			<xsl:value-of select="translate(doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text(), '/', '_')"></xsl:value-of>
			<xsl:text>,</xsl:text>
			<xsl:value-of select="$newline"></xsl:value-of>
			<xsl:value-of select="$tab"></xsl:value-of>
			<xsl:text>title = {</xsl:text>
			<xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']/text()"></xsl:value-of>
			<xsl:text>},</xsl:text>
			<xsl:value-of select="$newline"></xsl:value-of>
			<xsl:value-of select="$tab"></xsl:value-of>
			<xsl:text>author = {</xsl:text>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value']">
				<xsl:if test="position() &gt; 1"><xsl:text> AND </xsl:text></xsl:if>
				<xsl:value-of select="."></xsl:value-of>
			</xsl:for-each>
			<xsl:text>},</xsl:text>
			<xsl:if test="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']">
				<xsl:value-of select="$newline"></xsl:value-of>
				<xsl:value-of select="$tab"></xsl:value-of>
				<xsl:text>year = {</xsl:text>
				<xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']/text()"></xsl:value-of>
				<xsl:text>}</xsl:text>
			</xsl:if>
			<xsl:value-of select="$newline"></xsl:value-of>
		<xsl:text>}</xsl:text>
		<xsl:value-of select="$newline"></xsl:value-of>
	</xsl:template>
</xsl:stylesheet>