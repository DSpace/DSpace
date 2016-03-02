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
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:doc="http://www.lyncode.com/xoai">
  
  <xsl:output indent="yes" method="xml" omit-xml-declaration="yes"/>

  <!-- apply Dryad filters and transforms -->
  <xsl:include href="./common.xsl"/>

  <!-- 
    Pass-through template
  -->
	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" />
		</xsl:copy>
	</xsl:template>

</xsl:stylesheet>
