<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fn="http://www.w3.org/2005/02/xpath-function" 
	xmlns:d1="http://ns.dataone.org/service/types/v1"
	version="1.0">
	
	<xsl:output method="html" encoding="UTF-8" indent="yes" />
		
	<xsl:template name="objectList">
		<p>
			Registered DataONE objects 
			(displaying 
			<xsl:choose>
				<xsl:when test="*[local-name()='objectList']/@count > 0">
					<xsl:value-of select="*[local-name()='objectList']/@start + 1"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="*[local-name()='objectList']/@start"/>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:text>-</xsl:text>
			<xsl:value-of select="*[local-name()='objectList']/@start + *[local-name()='objectList']/@count"/> 	
			of 
			<xsl:value-of select="*[local-name()='objectList']/@total"/>
			total).
		</p>
		<hr/>
		<xsl:for-each select="*[local-name()='objectList']/objectInfo">
			<xsl:call-template name="objectInfo" />
			<hr/>
		</xsl:for-each>
	</xsl:template>
	
	<xsl:template name="objectInfo">
		<xsl:for-each select=".">
			<table>
				<tr>
					<td>Identifier: </td>
					<td>
						<a>
							<!-- TODO: evaluate relative resolve href -->
							<xsl:attribute name="href">/cn/v1/resolve/<xsl:value-of select="identifier"/></xsl:attribute>
							<xsl:attribute name="target">_blank</xsl:attribute>							
							<xsl:value-of select="identifier"/>
						</a>
					</td>
				</tr>
				<tr>
					<td>Format</td>
					<td><xsl:value-of select="formatId"/></td>
				</tr>
				<tr>
					<td>Modified: </td>
					<td><xsl:value-of select="dateSysMetadataModified"/></td>
				</tr>
				<tr>
					<td>Size: </td>
					<td><xsl:value-of select="size"/></td>
				</tr>
				<tr>
					<td>Checksum (<xsl:value-of select="checksum/@algorithm"/>):</td>
					<td><xsl:value-of select="checksum"/></td>
				</tr>
			</table>		
		</xsl:for-each>
	</xsl:template>	
	
</xsl:stylesheet>