<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.1" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim">
	
	<xsl:output omit-xml-declaration="no" indent="yes" encoding="UTF-8"/>
	<xsl:strip-space elements="*"/>
	
	<xsl:template match="@* | node()">
		<xsl:copy>
  			<xsl:apply-templates select="@* | node()"/>
  		</xsl:copy>
	</xsl:template>
	
	<xsl:template match="dim:field">
		<xsl:copy>
		
  			<xsl:copy-of select="@*"/>
  			
			<xsl:variable name="smallcase" select="'abcdefghijklmnopqrstuvwxyz'" />
		    <xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'" />
		    
		    <xsl:value-of select="translate(current(), $smallcase, $uppercase)" />
		    
  		</xsl:copy>
	</xsl:template>

</xsl:stylesheet>