<xsl:stylesheet
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:doc="http://www.lyncode.com/xoai"
		version="1.0">
	<xsl:output omit-xml-declaration="yes" method="xml" indent="yes" />

	<xsl:template match="/">
		<xsl:for-each select="doc:metadata/doc:element[@name='cerif']/doc:element[@name='openaire']/doc:element[@name='none']/doc:field[@name='value']">
			<xsl:value-of select="." disable-output-escaping="yes"/>
		</xsl:for-each>
	</xsl:template>
</xsl:stylesheet>
