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

	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" />
		</xsl:copy>
	</xsl:template>
	
 	<!-- Formatting dc.date.issued -->
	<xsl:template match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field/text()">
		<xsl:call-template name="formatdate">
			<xsl:with-param name="datestr" select="." />
		</xsl:call-template>
	</xsl:template>
 	
 	<!-- Formatting dcterms.dateAccepted --> 						 
	<xsl:template match="/doc:metadata/doc:element[@name='dcterms']/doc:element[@name='dateAccepted']/doc:element/doc:field/text()">
		<xsl:call-template name="formatdate">
			<xsl:with-param name="datestr" select="." />
		</xsl:call-template>
	</xsl:template>
	
	<!-- Removing other dc.date.* -->
	<xsl:template match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name!='issued']" />
		
	<!-- Prefixing and Modifying dc.rights -->
	<!-- Removing unwanted -->
	<xsl:template match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element/doc:element" />
	
	<!-- AUXILIARY TEMPLATES -->
	
	<!-- Date format -->
	<xsl:template name="formatdate">
		<xsl:param name="datestr" />
		<xsl:variable name="sub">
			<xsl:choose>
			<xsl:when test="translate(substring($datestr,1,10), '123456789', '000000000') = '0000-00-00'">
    			<xsl:value-of select="substring($datestr,1,10)" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="''"/>
			</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:value-of select="$sub" />
	</xsl:template>
</xsl:stylesheet>