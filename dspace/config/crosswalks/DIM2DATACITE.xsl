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


        <resource xmlns="http://datacite.org/schema/kernel-4" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4/metadata.xsd">

			<xsl:variable name="doi">
				<xsl:call-template name="get_identifier"/>
			</xsl:variable>

			<xsl:variable name="identifier" select="translate(substring-after($doi,'doi:'), $smallcase, $uppercase)"/>

			<!-- ********** Identifiers ********** -->
			<identifier identifierType="DOI">
				<xsl:value-of select="$identifier"/>
			</identifier>

			<!-- ********** Version ********** -->
			<xsl:variable name="version">
				<xsl:call-template name="find-version"><xsl:with-param name="working" select="dspace:field[@element='identifier'][@mdschema='dc']"/></xsl:call-template>
			</xsl:variable>
			<version>
				<xsl:value-of select="$version"/>
			</version>

		    <!-- ********** Creators ************* -->
		    <creators>
				<xsl:choose>
		            <xsl:when test="dspace:field[@element ='contributor' and @qualifier='author']">
						<xsl:for-each select="dspace:field[@element ='contributor' and @qualifier='author']">
							<xsl:variable name="orcid" select="substring-after(./@authority, 'orcid:')"/>
							<creator>
								<creatorName>
								  <xsl:value-of select="."/>
								</creatorName>
								<xsl:if test="$orcid">
									<nameIdentifier nameIdentifierScheme="ORCID">
										<xsl:value-of select="$orcid"/>
									</nameIdentifier>
								</xsl:if>
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
					<xsl:if test="$version > 1">
						<xsl:variable name="canonical-doi">
							<xsl:call-template name="canonical-doi"><xsl:with-param name="working" select="$identifier"/></xsl:call-template>
						</xsl:variable>

						<xsl:call-template name="related-identifiers">
							<xsl:with-param name="canonical-doi" select="$canonical-doi"/>
							<xsl:with-param name="current-version" select="$version - 1"/>
						</xsl:call-template>
					</xsl:if>
				</relatedIdentifiers>
			</xsl:if>
			
			
			<!-- *********** Sizes *********** -->
			<xsl:if test="dspace:field[@element='format' and @qualifier='extent']">
				<sizes>
					<xsl:for-each select="dspace:field[@element='format' and @qualifier='extent']">
						<size xmlns="http://datacite.org/schema/kernel-4">
							<xsl:value-of select="."/>
							<xsl:text> bytes</xsl:text>
						</size>
					</xsl:for-each>
				</sizes>
			</xsl:if>
			
			<!-- ************ Rights *************** -->
			<xsl:if test="$datatype='DataPackage'">
				<!--  All data package DOIs include a CC0 statement. -->
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
							<xsl:attribute name="rightsURI">
		                    <xsl:value-of select="dspace:field[@element='rights']"/>
							</xsl:attribute>
		                </xsl:otherwise>
		            </xsl:choose>
				</rights>
				</rightsList>
			</xsl:if>

			<!-- *********** Description - Only for data files ********* -->
			<xsl:if test="$datatype='DataFile'">
				<descriptions>
					<description descriptionType="Other">
					        <!-- Exclude provenance fields, since these often contain private information -->
						<xsl:value-of select="dspace:field[@element='description' and not(@qualifier='provenance')]"/>
					</description>
				</descriptions>
			</xsl:if>
        </resource>
	</xsl:template>
	<xsl:template name="find-version">
		<xsl:param name="working"/>
		<xsl:variable name="suffix">
			<xsl:choose>
				<xsl:when test="contains($working, 'DRYAD')">
					<xsl:value-of select="substring-after($working, 'DRYAD.')"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="substring-after($working, 'dryad.')"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="contains($suffix, '.')">
				<xsl:call-template name="version-number">
					<xsl:with-param name="working" select="$suffix"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>1</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="version-number">
		<xsl:param name="working"/>
		<xsl:variable name="remnant" select="substring-after($working, '.')"/>
		<xsl:choose>
			<xsl:when test="string-length($remnant)=0">
				<!-- if $working is just a number, that's the version. Otherwise, version is 1.-->
				<xsl:choose>
					<xsl:when test="string(number($working))=$working">
						<xsl:value-of select="$working"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:text>1</xsl:text>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="version-number">
					<xsl:with-param name="working" select="$remnant"/>
				</xsl:call-template>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="canonical-doi">
		<xsl:param name="working"/>
		<xsl:variable name="prefix" select="concat(substring-before($working, 'DRYAD.'),'DRYAD.')"/>
		<xsl:variable name="suffix" select="substring-after($working, 'DRYAD.')"/>

		<!-- if it's a file, there will still be a slash in the suffix -->
		<xsl:choose>
			<xsl:when test="contains($suffix, '/')">
				<xsl:variable name="file-suffix" select="substring-after($suffix, '/')"/>
				<xsl:variable name="package-suffix" select="substring-before($suffix, '/')"/>
				<!-- look for a dot in the file suffix -->
				<xsl:choose>
					<xsl:when test="contains($file-suffix, '.')">
						<!-- both the file and package suffixes need to be canonicalized -->
						<xsl:value-of select="concat($prefix, substring-before($package-suffix, '.'), '.1', '/', substring-before($file-suffix, '.'), '.1')"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="$working"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:otherwise>
				<!-- look for a dot in the package suffix -->
				<xsl:choose>
					<xsl:when test="contains($suffix, '.')">
						<!-- the package suffix needs to be canonicalized -->
						<xsl:value-of select="concat($prefix, substring-before($suffix, '.'), '.1')"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="$working"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="versioned-doi">
		<xsl:param name="canonical-doi"/>
		<xsl:param name="version"/>
		<xsl:variable name="prefix" select="concat(substring-before($canonical-doi, 'DRYAD.'),'DRYAD.')"/>
		<xsl:variable name="suffix" select="substring-after($canonical-doi, 'DRYAD.')"/>

		<!-- if it's a file, there will still be a slash in the suffix -->
		<xsl:choose>
			<xsl:when test="contains($suffix, '/')">
				<xsl:variable name="file-suffix" select="substring-after($suffix, '/')"/>
				<xsl:variable name="package-suffix" select="substring-before($suffix, '/')"/>
				<!-- both the file and package suffixes need to be versioned -->
				<xsl:value-of select="concat($prefix, substring-before($package-suffix, '.'), '.', $version, '/', substring-before($file-suffix, '.'), '.', $version)"/>
			</xsl:when>
			<xsl:otherwise>
				<!-- the package suffix needs to be versioned -->
				<xsl:value-of select="concat($prefix, substring-before($suffix, '.'), '.', $version)"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="related-identifiers">
		<xsl:param name="canonical-doi"/>
		<xsl:param name="current-version"/>
		<xsl:if test="$current-version &gt; 0">
			<relatedIdentifier xmlns="http://datacite.org/schema/kernel-4" relatedIdentifierType="DOI" relationType="IsNewVersionOf">
				<xsl:call-template name="versioned-doi">
					<xsl:with-param name="canonical-doi" select="$canonical-doi"/>
					<xsl:with-param name="version" select="$current-version"/>
				</xsl:call-template>
			</relatedIdentifier>
			<xsl:call-template name="related-identifiers">
				<xsl:with-param name="canonical-doi" select="$canonical-doi"/>
				<xsl:with-param name="current-version" select="$current-version - 1"/>
			</xsl:call-template>
		</xsl:if>
	</xsl:template>

</xsl:stylesheet>
