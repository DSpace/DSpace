<xsl:stylesheet
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:doc="http://www.lyncode.com/xoai"
		version="1.0">
	<xsl:output omit-xml-declaration="yes" method="xml" indent="yes" />

	<xsl:template match="/">
		<oai_dc:dc xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
				   xmlns:dc="http://purl.org/dc/elements/1.1/"
				   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				   xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
			<xsl:for-each select="doc:metadata/doc:element[@name='cerif']/doc:element[@name='openaire']/doc:element[@name='none']/doc:field[@name='value']">
				<xsl:value-of select="." disable-output-escaping="yes"/>
			</xsl:for-each>
		</oai_dc:dc>
	</xsl:template>
</xsl:stylesheet>
