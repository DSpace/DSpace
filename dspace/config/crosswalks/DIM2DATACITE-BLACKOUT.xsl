<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:mets="http://www.loc.gov/METS/"
                xmlns:dspace="http://www.dspace.org/xmlns/dspace/dim"
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:dryad="http://purl.org/dryad/terms/"
                version="1.0">

	<xsl:strip-space elements="*"/>
    <xsl:output method="xml" version="1.0" encoding="utf-8" indent="yes"/>
	<xsl:variable name="smallcase" select="'abcdefghijklmnopqrstuvwxyz'" />
	<xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'" />

	<!-- Find the DOI information. -->
	<xsl:template name="get_identifier">
		<xsl:value-of select="dspace:field[@element ='identifier'][@mdschema='dc']"/>
	</xsl:template>

	<!-- Main match for the root node: set up the root element, <resource> -->
    <xsl:template match="/">
			<xsl:apply-templates/>
	</xsl:template>
		
	<!-- Parse datacite tags that are found in DIM -->
	<xsl:template match="dim:dim">
		<xsl:variable name="datatype">
			<xsl:choose>
				<xsl:when test="dspace:field[@element='relation' and @qualifier='ispartof']">
					<xsl:text>DataFile</xsl:text>
				</xsl:when>
				<xsl:when test="dspace:field[@element='relation' and @qualifier='haspart']">
					<xsl:text>DataPackage</xsl:text>
				</xsl:when>
				<xsl:otherwise>
					<xsl:text></xsl:text>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

        <resource xmlns="http://datacite.org/schema/kernel-4" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4/metadata.xsd">

			<xsl:variable name="identifier-doi" select="dspace:field[@element='identifier' and not(@qualifier)]" />
			<!-- ********** Identifiers ********** -->
			<identifier identifierType="DOI">
				<xsl:value-of select="translate(substring-after($identifier-doi,'doi:'), $smallcase, $uppercase)"/>
			</identifier>

			<!-- ********** Creators ************* -->
			<creators>
				<creator>
					<creatorName>
						<xsl:text>(:tba)</xsl:text>
					</creatorName>
				</creator>
			</creators>

			<!-- ********* Title *************** -->
			<titles>
				<title>
					<xsl:text>Dryad Item </xsl:text>
					<xsl:value-of select="translate(substring-after($identifier-doi,'doi:'), $smallcase, $uppercase)"/>
				</title>
			</titles>

		    <!-- *********** Publisher ************ -->
	        <publisher>Dryad Digital Repository</publisher>

			<!-- ************ Publication Year ************** -->
			<publicationYear>
				<xsl:text>0000</xsl:text>
			</publicationYear>

			<!-- ************ Subjects ************** -->
			<subjects>
				<subject>
					<xsl:text>(:tba)</xsl:text>
				</subject>
			</subjects>

			<!-- ************ Funding information ************** -->
			<xsl:if test="dspace:field[@element='fundingEntity' and @confidence='ACCEPTED']">
				<fundingReferences>
					<xsl:for-each select="dspace:field[@element='fundingEntity']">
						<xsl:variable name="awardNumber" select="substring-before(.,'@')"/>
						<xsl:variable name="funderName" select="substring-after(.,'@')"/>
						<xsl:variable name="funderID" select="substring-after(./@authority, 'http://dx.doi.org/')"/>
						<xsl:variable name="confidence" select="./@confidence"/>
						<xsl:if test="$confidence='ACCEPTED'">
							<fundingReference>
								<funderName>
									<xsl:value-of select="$funderName"/>
								</funderName>
								<funderIdentifier funderIdentifierType="Crossref Funder ID">
									<xsl:value-of select="concat('http://doi.org/', $funderID)"/>
								</funderIdentifier>
								<awardNumber><xsl:value-of select="$awardNumber"/></awardNumber>
							</fundingReference>
						</xsl:if>
					</xsl:for-each>
				</fundingReferences>
			</xsl:if>

			<!-- ************ Dates - Only for Data Files ************** -->
			<xsl:if test="$datatype='DataFile'">
				<xsl:variable name="embargoedUntil"
							select="dspace:field[@element='date' and @qualifier='embargoedUntil']"/>
				<xsl:variable name="dateAccepted" select="dspace:field[@element='date' and @qualifier='issued']"/>
				<xsl:if test="($embargoedUntil and not($embargoedUntil='9999-01-01')) or $dateAccepted">
				<dates>
				  <xsl:if test="$embargoedUntil and not($embargoedUntil='9999-01-01')">
					  <date dateType="Available">
						  <xsl:text>(:tba)</xsl:text>
					  </date>
				  </xsl:if>
				  <xsl:if test="$dateAccepted">
					  <date dateType="Accepted">
						  <xsl:text>(:tba)</xsl:text>
					  </date>
				  </xsl:if>
				</dates>
				</xsl:if>
			</xsl:if>

		    <!-- ************ Resource Type ************** -->
			<resourceType resourceTypeGeneral="Dataset">
				<xsl:value-of select="$datatype"/>
			</resourceType>

		    <!-- *********** Related Identifiers ********* -->
			<xsl:if test="dspace:field[@element='relation']">
				<relatedIdentifiers>
				    <xsl:for-each select="dspace:field[@element='relation' and @qualifier='haspart']">
						<relatedIdentifier relatedIdentifierType="DOI" relationType="HasPart">
							<xsl:variable name="id" select="."/>
							<xsl:if test="starts-with($id,'doi')">
								<xsl:value-of select="translate(substring-after($id,'doi:'), $smallcase, $uppercase)"/>
							</xsl:if>
						</relatedIdentifier>
				    </xsl:for-each>
				    <xsl:for-each select="dspace:field[@element='relation' and @qualifier='ispartof']">
				        <relatedIdentifier relatedIdentifierType="DOI" relationType="IsPartOf">
							<xsl:variable name="id" select="."/>
							<xsl:if test="starts-with($id,'doi')">
								<xsl:value-of select="translate(substring-after($id,'doi:'), $smallcase, $uppercase)"/>
							</xsl:if>
				        </relatedIdentifier>
				    </xsl:for-each>
				    <xsl:for-each select="dspace:field[@element='relation' and @qualifier='isreferencedby']">
				      <xsl:variable name="id" select="."/>
				      <xsl:if test="starts-with($id,'doi')">
					<relatedIdentifier relatedIdentifierType="DOI" relationType="IsReferencedBy">
					  <xsl:value-of select="translate(substring-after($id,'doi:'), $smallcase, $uppercase)"/>
					</relatedIdentifier>
				      </xsl:if>
				      <xsl:if test="starts-with($id,'PMID')">
					<relatedIdentifier relatedIdentifierType="PMID" relationType="IsReferencedBy">
					  <xsl:value-of select="translate(substring-after($id,'PMID:'), $smallcase, $uppercase)"/>
					</relatedIdentifier>
				      </xsl:if>
				    </xsl:for-each>
				</relatedIdentifiers>
			</xsl:if>

			<!-- *********** Sizes - Only for data files ********* -->
			<xsl:if test="dspace:field[@element='format' and @qualifier='extent']">
				<sizes>
					<xsl:for-each select="dspace:field[@element='format' and @qualifier='extent']">
						<size xmlns="http://datacite.org/schema/kernel-4">
							<xsl:text>(:tba)</xsl:text>
						</size>
					</xsl:for-each>
				</sizes>
			</xsl:if>
			
			<!-- ************ Rights *************** -->
			<!--  All data package DOIs include a CC0 statement. -->

			<xsl:if test="$datatype='DataPackage'">
				<rightsList>
					<rights>
						<xsl:attribute name="rightsURI">
							<xsl:text>http://creativecommons.org/publicdomain/zero/1.0/</xsl:text>
						</xsl:attribute>
					</rights>
				</rightsList>
			</xsl:if>

			<xsl:if test="$datatype='DataFile'">
				<rightsList>
					<rights>
						<xsl:text>(:tba)</xsl:text>
					</rights>
				</rightsList>
			</xsl:if>
			
			<!-- *********** Description - Only for data files ********* -->
			<xsl:if test="$datatype='DataFile'">
				<descriptions>
					<description descriptionType="Other">
						<xsl:text>(:tba)</xsl:text>
					</description>
				</descriptions>
			</xsl:if>
        </resource>
	</xsl:template>
</xsl:stylesheet>
