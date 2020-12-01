<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.1" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:cerif="https://www.openaire.eu/cerif-profile/1.1/">
	
	<xsl:output omit-xml-declaration="no" indent="yes" encoding="UTF-8"/>
	<xsl:strip-space elements="*"/>
	
	<xsl:template match="@* | node()">
		<xsl:copy>
  			<xsl:apply-templates select="@* | node()"/>
  		</xsl:copy>
	</xsl:template>
	
	<xsl:template match="cerif:Equipment">
		<xsl:copy>
  			<xsl:copy-of select="@*"/>
			<xsl:copy-of select="node()"/>
    		<cerif:Identifier type="Institution assigned unique equipment identifier" >test-id</cerif:Identifier>
  		</xsl:copy>
	</xsl:template>

</xsl:stylesheet>