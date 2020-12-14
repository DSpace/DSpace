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
	
	<xsl:template match="cerif:Project">
		<dim:dim>
		
			<dim:field mdschema="dc" element="title" >
				<xsl:value-of select="cerif:Title" />
			</dim:field>
			
			<dim:field mdschema="oairecerif" element="acronym">
				<xsl:value-of select="cerif:Acronym" />
			</dim:field>
			
			<xsl:for-each select="cerif:Identifier">
				<xsl:choose>
					<xsl:when test="@type = 'http://namespace.openaire.eu/oaf'">
						<dim:field mdschema="crispj" element="openaireid" >
							<xsl:value-of select="current()" />
						</dim:field>
					</xsl:when>
					<xsl:when test="@type = 'URL'">
						<dim:field mdschema="oairecerif" element="identifier" qualifier="url" >
							<xsl:value-of select="current()" />
						</dim:field>
					</xsl:when>
				</xsl:choose>
			</xsl:for-each>
			
			<dim:field mdschema="oairecerif" element="project" qualifier="startDate" >
				<xsl:value-of select="cerif:StartDate" />
			</dim:field>
			
			<dim:field mdschema="oairecerif" element="project" qualifier="endDate" >
				<xsl:value-of select="cerif:EndDate" />
			</dim:field>
			
			<dim:field mdschema="oairecerif" element="project" qualifier="status" >
				<xsl:value-of select="cerif:Status" />
			</dim:field>
			
			<xsl:for-each select="cerif:Consortium/cerif:Coordinator">
				<dim:field mdschema="crispj" element="coordinator">
					<xsl:if test="cerif:OrgUnit/@id">
						<xsl:attribute name="authority"><xsl:value-of select="concat($idPrefix,cerif:OrgUnit/@id)"/></xsl:attribute>
					</xsl:if>
					<xsl:value-of select="cerif:OrgUnit/cerif:Name"/>
				</dim:field>
			</xsl:for-each>
			
			<xsl:for-each select="cerif:Consortium/cerif:Partner">
				<dim:field mdschema="crispj" element="partnerou">
					<xsl:if test="cerif:OrgUnit/@id">
						<xsl:attribute name="authority"><xsl:value-of select="concat($idPrefix,cerif:OrgUnit/@id)"/></xsl:attribute>
					</xsl:if>
					<xsl:value-of select="cerif:OrgUnit/cerif:Name"/>
				</dim:field>
			</xsl:for-each>
			
			<xsl:for-each select="cerif:Consortium/cerif:Member">
				<dim:field mdschema="crispj" element="organization">
					<xsl:if test="cerif:OrgUnit/@id">
						<xsl:attribute name="authority"><xsl:value-of select="concat($idPrefix,cerif:OrgUnit/@id)"/></xsl:attribute>
					</xsl:if>
					<xsl:value-of select="cerif:OrgUnit/cerif:Name"/>
				</dim:field>
			</xsl:for-each>
			
			<dim:field mdschema="crispj" element="investigator" >
				<xsl:value-of select="cerif:Team/cerif:PrincipalInvestigator/cerif:Person/@displayName" />
			</dim:field>
			
			<xsl:for-each select="cerif:Team/cerif:Member/cerif:Person">
				<dim:field mdschema="crispj" element="coinvestigators" >
					<xsl:value-of select="@displayName" />
				</dim:field>
			</xsl:for-each>
			
			<xsl:for-each select="cerif:Uses/cerif:Equipment">
				<dim:field mdschema="dc" element="relation" qualifier="equipment" >
					<xsl:value-of select="cerif:Name" />
				</dim:field>
			</xsl:for-each>
			
			<dim:field mdschema="dc" element="description" qualifier="abstract" >
				<xsl:value-of select="cerif:Abstract" />
			</dim:field>
			
			<xsl:for-each select="cerif:Keyword">
				<dim:field mdschema="dc" element="subject" >
					<xsl:value-of select="current()" />
				</dim:field>
			</xsl:for-each>
			
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