<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:doc="http://www.lyncode.com/xoai"
	version="1.0">
	<xsl:output omit-xml-declaration="yes" method="xml" indent="yes" />
	
	<!-- An identity transformation to show the internal XOAI generated XML -->
	<xsl:template match="/ | @* | node()">
		<xsl:copy-of select="@* | node()">
		</xsl:copy-of>
	</xsl:template>
</xsl:stylesheet>
