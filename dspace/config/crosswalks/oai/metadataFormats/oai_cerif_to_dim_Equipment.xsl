<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:cerif="https://www.openaire.eu/cerif-profile/1.1/"
	xmlns:pt="https://www.openaire.eu/cerif-profile/vocab/COAR_Publication_Types"
	xmlns:ft="https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Funding_Types">
	
	<xsl:param name="nestedMetadataPlaceholder" />
	<xsl:param name="converterSeparator" />
	<xsl:param name="idPrefix" />
	
	<xsl:template match="cerif:Equipment">
		<dim:dim>
			
			<dim:field mdschema="dc" element="title" >
				<xsl:value-of select="cerif:Name" />
			</dim:field>
			
			<dim:field mdschema="oairecerif" element="acronym">
				<xsl:value-of select="cerif:Acronym" />
			</dim:field>
			
			<dim:field mdschema="oairecerif" element="internalid">
				<xsl:value-of select="cerif:Identifier[@type = 'Institution assigned unique equipment identifier']" />
			</dim:field>
			
			<dim:field mdschema="dc" element="description">
				<xsl:value-of select="cerif:Description" />
			</dim:field>
			
			<dim:field mdschema="crisequipment" element="ownerou">
				<xsl:value-of select="cerif:Owner/cerif:OrgUnit/cerif:Name" />
			</dim:field>
			
			<dim:field mdschema="crisequipment" element="ownerrp">
				<xsl:value-of select="cerif:Owner/cerif:Person/@displayName" />
			</dim:field>
			
		</dim:dim>
	</xsl:template>
	
	
	<xsl:template name="nestedMetadataValue">
		<xsl:param name = "value" />
		<xsl:choose>
			<xsl:when test="$value">
				<xsl:value-of select="$value" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$nestedMetadataPlaceholder"/> 
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
</xsl:stylesheet>