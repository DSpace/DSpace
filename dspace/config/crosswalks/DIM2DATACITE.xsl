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
		
		<xsl:variable name="embargoedUntil" select="dspace:field[@element='date' and @qualifier='embargoedUntil']"/>
		<xsl:variable name="dateAccepted" select="dspace:field[@element='date' and @qualifier='issued']"/>


        <resource xmlns="http://datacite.org/schema/kernel-2.2" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://datacite.org/schema/kernel-2.2 http://schema.datacite.org/meta/kernel-2.2/metadata.xsd"
                  lastMetadataUpdate="2006-05-04" metadataVersionNumber="1">

			<!-- ********** Identifiers ********** -->
			<identifier identifierType="DOI">
				<xsl:variable name="doi">
					<xsl:call-template name="get_identifier"/>
				</xsl:variable>
				<xsl:value-of select="translate(substring-after($doi,'doi:'), $smallcase, $uppercase)"/>
			</identifier>
		    <!-- ********** Creators ************* -->
		    <creators>
				<xsl:choose>
		            <xsl:when test="dspace:field[@element ='contributor' and @qualifier='author']">
						<xsl:for-each select="dspace:field[@element ='contributor' and @qualifier='author']">
							<creator>
								<creatorName>
								  <xsl:value-of select="."/>
								</creatorName>
							</creator>
						</xsl:for-each>
		            </xsl:when>
		            <xsl:otherwise>
						<creator><creatorName>(:unav)</creatorName></creator>
					</xsl:otherwise>
				</xsl:choose>
			</creators>
	
		    <!-- ********* Title *************** -->
			<xsl:if test="dspace:field[@element ='title']">
			    <titles>
			        <xsl:for-each select="dspace:field[@element ='title']">
			            <title>
			                <xsl:value-of select="."/>
			            </title>
			        </xsl:for-each>
			    </titles>
			</xsl:if>
	
		    <!-- *********** Publisher ************ -->
	        <publisher>Dryad Digital Repository</publisher>
	
		    <!-- ************ Publication Year ************** -->
	        <xsl:if test="dspace:field[@element='date' and @qualifier='accessioned']">
	            <xsl:for-each select="dspace:field[@qualifier='accessioned'][1]">
	                <publicationYear>
	                    <xsl:variable name="date" select="."/>
	                    <xsl:value-of select="substring($date, 0, 5)"/>
	                </publicationYear>
	            </xsl:for-each>
	        </xsl:if>
	
		    <!-- ************ Subjects ************** -->
	        <xsl:if test="dspace:field[@element ='subject' or @element='coverage']">
	            <subjects>
	                <xsl:for-each select="dspace:field[@element ='subject']">
	                    <subject>
	                      <xsl:value-of select="."/>
	                    </subject>
	                </xsl:for-each>
	                <xsl:for-each select="dspace:field[@element ='coverage']">
	                    <subject>
	                      <xsl:value-of select="."/>
	                    </subject>
	                </xsl:for-each>
	                <xsl:for-each select="dspace:field[@element ='ScientificName']">
	                    <subject>
	                      <xsl:value-of select="."/>
	                    </subject>
	                </xsl:for-each>
	            </subjects>
	        </xsl:if>
			<!-- ************ Dates - Only for Data Files ************** -->
			<xsl:if test="$datatype='DataFile'">
				<dates>
					<xsl:if test="$embargoedUntil and not($embargoedUntil='9999-01-01')">
						<date dateType="Available">
							<xsl:value-of select="$embargoedUntil"/>
						</date>
					</xsl:if>
					<xsl:if test="$dateAccepted">
						<date dateType="Accepted">
							<xsl:value-of select="$dateAccepted"/>
						</date>
					</xsl:if>
				</dates>
			</xsl:if>

		    <!-- ************ Resource Type ************** -->
			<resourceType resourceTypeGeneral="Dataset">
				<xsl:value-of select="$datatype"/>
			</resourceType>
			
			<!-- ************ Alternate Identifiers ************** -->
			<xsl:variable name="alternateIdentifiers">
				<xsl:if test="dspace:field[@element ='identifier']">
					<xsl:for-each select="dspace:field[@element ='identifier' and not(@qualifier='manuscriptNumber') and not(@qualifier='uri')]">
					    <xsl:variable name="id" select="."/>
					    <xsl:choose>
					        <xsl:when test="not(starts-with($id,'doi'))">
					            <xsl:element name="alternateIdentifier">
					                <xsl:attribute name="alternateIdentifierType">
					                    <xsl:value-of select="@qualifier"/>
					                </xsl:attribute>
					                <xsl:value-of select="."/>
					            </xsl:element>
					        </xsl:when>
					    </xsl:choose>
					</xsl:for-each>
				</xsl:if>
			</xsl:variable>
	      
			<xsl:if test="$alternateIdentifiers!=''">
				<alternateIdentifiers>
					<xsl:copy-of select="$alternateIdentifiers"/>
				</alternateIdentifiers>
			</xsl:if>
	
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
						<relatedIdentifier relatedIdentifierType="DOI" relationType="IsReferencedBy">
							<xsl:variable name="id" select="."/>
							<xsl:if test="starts-with($id,'doi')">
								<xsl:value-of select="translate(substring-after($id,'doi:'), $smallcase, $uppercase)"/>
							</xsl:if>
						</relatedIdentifier>
				    </xsl:for-each>
				</relatedIdentifiers>
			</xsl:if>
			
			
			<!-- *********** Sizes *********** -->
			<xsl:if test="dspace:field[@element='format' and @qualifier='extent']">
				<sizes>
					<xsl:for-each select="dspace:field[@element='format' and @qualifier='extent']">
						<size xmlns="http://datacite.org/schema/kernel-2.2">
							<xsl:value-of select="."/>
							<xsl:text> bytes</xsl:text>
						</size>
					</xsl:for-each>
				</sizes>
			</xsl:if>
			
			<!-- ************ Rights *************** -->
			<xsl:if test="$datatype='DataPackage'">
				<!--  All data package DOIs include a CC0 statement. -->
				<rights>
					<xsl:text>http://creativecommons.org/publicdomain/zero/1.0/</xsl:text>
				</rights>
			</xsl:if>

			<xsl:if test="$datatype='DataFile'">
				<rights>
					<xsl:variable name="embargoType" select="dspace:field[@element='type' and @qualifier='embargo']"/>

		            <xsl:choose>
		                <!-- If the embargoedDate is empty, this item is no longer embargoed -->
		                <xsl:when test="$embargoedUntil!=''">
		                    <xsl:choose>
								<xsl:when test="$embargoedUntil='9999-01-01' and $embargoType='oneyear'">
									<!-- The item is under one-year embargo, but the article has not been published yet,
									so we don't have an end date. -->
									<xsl:text>At the request of the author, this item is embargoed until one year after the associated article is published.</xsl:text>
								</xsl:when>
								<xsl:when test="$embargoedUntil='9999-01-01' and ($embargoType='untilArticleAppears' or $embargoType='unknown')">
									<!-- The item is under embargo, but the end date is not yet known -->
									<xsl:text>At the request of the author, this item is embargoed until the associated article is published.</xsl:text>
								</xsl:when>
								<xsl:when test="$embargoedUntil='9999-01-01' and $embargoType='custom'">
									<!-- The item is under embargo, but the end date is not yet known. The editor has approved a custom length. -->
									<xsl:text>At the request of the author, this item is embargoed. The journal editor has set a custom embargo length. Once the associated article is published, the exact release date of the embargo will be shown here.</xsl:text>
								</xsl:when>
								<xsl:otherwise>
									<!-- The item is under embargo, and the end date of the embargo is known. -->
									<xsl:text>At the request of the author, this item is embargoed until </xsl:text>
									<xsl:value-of select="$embargoedUntil"/>
								</xsl:otherwise>
		                    </xsl:choose>
		                </xsl:when>
		                <xsl:otherwise>
		                    <xsl:value-of select="dspace:field[@element='rights']"/>
		                </xsl:otherwise>
		            </xsl:choose>
				</rights>
			</xsl:if>

			<!-- *********** Description - Only for data files ********* -->
			<xsl:if test="$datatype='DataFile'">
				<descriptions>
					<description descriptionType="Other">
						<xsl:value-of select="dspace:field[@element='description']"/>
					</description>
				</descriptions>
			</xsl:if>
        </resource>
	</xsl:template>
</xsl:stylesheet>
