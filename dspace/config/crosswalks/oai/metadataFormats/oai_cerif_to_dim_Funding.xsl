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

	<xsl:template match="cerif:Funding">
		<dim:dim>
		
			<dim:field mdschema="dc" element="title" >
				<xsl:value-of select="cerif:Name" />
			</dim:field>
			
			<dim:field mdschema="oairecerif" element="acronym">
				<xsl:value-of select="cerif:Acronym" />
			</dim:field>
			
			<xsl:if test="ft:Type">
				<dim:field mdschema="dc" element="type" >
					<xsl:value-of select="concat('cerifToFundingTypes',$converterSeparator,ft:Type)" />
				</dim:field>
			</xsl:if>
			
			<xsl:for-each select="cerif:Identifier">
				<xsl:choose>
					<xsl:when test="@type = 'https://w3id.org/cerif/vocab/IdentifierTypes#FinanceID'">
						<dim:field mdschema="oairecerif" element="internalid" >
							<xsl:value-of select="current()" />
						</dim:field>
					</xsl:when>
					<xsl:when test="@type = 'https://w3id.org/cerif/vocab/IdentifierTypes#AwardNumber'">
						<dim:field mdschema="oairecerif" element="funding" qualifier="identifier" >
							<xsl:value-of select="current()" />
						</dim:field>
					</xsl:when>
				</xsl:choose>
			</xsl:for-each>
			
			<dim:field mdschema="oairecerif" element="amount" >
				<xsl:value-of select="cerif:Amount" />
			</dim:field>
			
			<dim:field mdschema="oairecerif" element="amount" qualifier="currency">
				<xsl:value-of select="cerif:Amount/@currency" />
			</dim:field>
			
			<dim:field mdschema="dc" element="description">
				<xsl:value-of select="cerif:Description" />
			</dim:field>
			
			<dim:field mdschema="oairecerif" element="funder">
				<xsl:if test="cerif:Funder/cerif:OrgUnit/@id">
					<xsl:attribute name="authority"><xsl:value-of select="concat($idPrefix,cerif:Funder/cerif:OrgUnit/@id)"/></xsl:attribute>
				</xsl:if>
				<xsl:value-of select="cerif:Funder/cerif:OrgUnit/cerif:Name"/>
			</dim:field>
			
			<dim:field mdschema="oairecerif" element="funding" qualifier="startDate">
				<xsl:value-of select="cerif:Duration/@startDate" />
			</dim:field>
			
			<dim:field mdschema="oairecerif" element="funding" qualifier="endDate">
				<xsl:value-of select="cerif:Duration/@endDate" />
			</dim:field>
			
			<dim:field mdschema="oairecerif" element="oamandate" >
				<xsl:value-of select="cerif:OAMandate/@mandated" />
			</dim:field>
			
			<dim:field mdschema="oairecerif" element="oamandate" qualifier="url" >
				<xsl:value-of select="cerif:OAMandate/@URL" />
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