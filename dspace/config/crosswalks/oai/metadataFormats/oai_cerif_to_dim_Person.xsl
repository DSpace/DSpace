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

	<xsl:template match="cerif:Person">
		<dim:dim>
		
			<dim:field mdschema="dc" element="title" >
				<xsl:choose>
					<xsl:when test="cerif:PersonName/cerif:FamilyNames and cerif:PersonName/cerif:FirstNames">
						<xsl:value-of select="concat(cerif:PersonName/cerif:FamilyNames,', ',cerif:PersonName/cerif:FirstNames)" />
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="cerif:PersonName/cerif:FamilyNames"/><xsl:value-of select="cerif:PersonName/cerif:FirstNames"/> 
					</xsl:otherwise>
				</xsl:choose>
			</dim:field>
			
			<dim:field mdschema="person" element="familyName" >
				<xsl:value-of select="cerif:PersonName/cerif:FamilyNames"/>
			</dim:field>
			
			<dim:field mdschema="person" element="givenName" >
				<xsl:value-of select="cerif:PersonName/cerif:FirstNames"/>
			</dim:field>
			
			<xsl:for-each select="cerif:PersonName/cerif:OtherNames">
				<dim:field mdschema="crisrp" element="name" qualifier="variant">
					<xsl:value-of select="current()"/>
				</dim:field>
			</xsl:for-each>
			
			<dim:field mdschema="oairecerif" element="person" qualifier="gender">
				<xsl:value-of select="cerif:Gender"/>
			</dim:field>
			
			<dim:field mdschema="person" element="identifier" qualifier="orcid">
				<xsl:value-of select="cerif:ORCID"/>
			</dim:field>
			
			<xsl:for-each select="cerif:ResearcherID">
				<dim:field mdschema="person" element="identifier" qualifier="rid">
					<xsl:value-of select="current()"/>
				</dim:field>
			</xsl:for-each>
			
			<xsl:for-each select="cerif:ScopusAuthorID">
				<dim:field mdschema="person" element="identifier" qualifier="scopus-author-id">
					<xsl:value-of select="current()"/>
				</dim:field>
			</xsl:for-each>
			
			<xsl:for-each select="cerif:ElectronicAddress">
				<dim:field mdschema="person" element="email">
					<xsl:value-of select="current()"/>
				</dim:field>
			</xsl:for-each>
			
			<xsl:for-each select="cerif:Affiliation">
				<xsl:choose>
					<xsl:when test="cerif:OrgUnit/cerif:Acronym">
						<dim:field mdschema="person" element="affiliation" qualifier="name">
							<xsl:value-of select="cerif:OrgUnit/cerif:Acronym"/>
						</dim:field>
					</xsl:when>
					<xsl:otherwise>
						<dim:field mdschema="oairecerif" element="affiliation" qualifier="startDate" >
							<xsl:call-template name="nestedMetadataValue">
						    	<xsl:with-param name="value" select="@startDate" />
					    	</xsl:call-template>
						</dim:field>
						<dim:field mdschema="oairecerif" element="affiliation" qualifier="endDate" >
							<xsl:call-template name="nestedMetadataValue">
						    	<xsl:with-param name="value" select="@endDate" />
					    	</xsl:call-template>
						</dim:field>
						<dim:field mdschema="oairecerif" element="affiliation" qualifier="role" >
							<xsl:call-template name="nestedMetadataValue">
						    	<xsl:with-param name="value" select="@role" />
					    	</xsl:call-template>
						</dim:field>
						<dim:field mdschema="oairecerif" element="person" qualifier="affiliation" >
							<xsl:call-template name="nestedMetadataValue">
						    	<xsl:with-param name="value" select="cerif:OrgUnit/cerif:Name" />
					    	</xsl:call-template>
						</dim:field>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:for-each>
		
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