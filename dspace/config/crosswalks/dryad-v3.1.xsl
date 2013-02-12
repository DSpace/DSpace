<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim" xmlns:mets="http://www.loc.gov/METS/"
	xmlns:bibo="http://purl.org/ontology/bibo/" xmlns:dcterms="http://purl.org/dc/terms/"
	xmlns:dwc="http://rs.tdwg.org/dwc/terms/" xmlns="http://purl.org/dryad/terms/"
	version="1.0" exclude-result-prefixes="xsl mets dim">

	<!-- Lame workaround for the fact that XSD doesn't really support unordered... 
		it's either xs:sequence (with cardinality) or xs:all (without cardinality) 
		We can remove this if we ever write a RELAX NG schema of the Dryad Profile -->
	<xsl:import href="dryad-v3.1-order.xsl" />

	<xsl:output indent="yes" method="xml" />

   <!--  In case what's passed in is just the dims from the dspace item record -->
	<xsl:template match="/dim:dim">
		<xsl:call-template name="rootTemplate"/>
	</xsl:template>
	
	<!-- In case what's passed in is the whole dspace mets record -->
	<xsl:template match="//mets:xmlData/dim:dim">
		<xsl:call-template name="rootTemplate"/>
	</xsl:template>

	<xsl:template name="rootTemplate">
		<DryadMetadata>
		<xsl:choose>
			<xsl:when 	test="count(dim:field[@element='relation'][@qualifier='haspart']) &gt; 0">
				<xsl:call-template name="package_template" >
					<xsl:with-param name="node" select="."/>
				</xsl:call-template>
			</xsl:when>
			<xsl:when test="count(dim:field[@element='relation'][@qualifier='ispartof']) &gt; 0">
				<xsl:call-template name="file_template" >
					<xsl:with-param name="node" select="."/>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<output>The XML output is not what was expected</output>
			</xsl:otherwise>
		</xsl:choose>
		</DryadMetadata>
	</xsl:template>

	<!-- An old Dryad data file has a dc.relation.ispartof value -->
	<xsl:template name="file_template">
		<xsl:param name="node"/>
		<xsl:variable name="fileNode">
			<DryadDataFile>
				<dcterms:type>file</dcterms:type>
				<status>deposited</status>
				<xsl:apply-templates select="$node/dim:field" mode="inner" />
			
				<!-- special handling for isPartOf, while the metadata still contains handles in this field -->
				<xsl:variable name="fileDOI">
				  <xsl:value-of select="$node/dim:field[@element='identifier'][not(@qualifier)]" />
				</xsl:variable>
				<xsl:variable name="calculatedPackageDOI">
				  <xsl:call-template name="substring-before-last">
				    <xsl:with-param name="string" select="$fileDOI" />
				    <xsl:with-param name="match" select="'/'" />
				  </xsl:call-template>
				</xsl:variable>
				<dcterms:isPartOf>
				  <xsl:call-template name="normalize_doi">
				    <xsl:with-param name="node" select="$calculatedPackageDOI" />
				  </xsl:call-template>
				</dcterms:isPartOf>
			</DryadDataFile>
		</xsl:variable>
		<!-- our workaround -->
		<xsl:apply-templates xmlns:xalan="http://xml.apache.org/xalan"
			select="xalan:nodeset($fileNode)/*" />
	</xsl:template>

	<!-- An old Dryad data package has a dc.relation.haspart value -->
	<xsl:template name="package_template">
		<xsl:param name="node"/>
		<xsl:variable name="pubPkgNode">
			<DryadDataPackage>
				<dcterms:type>package</dcterms:type>
				<!-- TODO: assign value below dynamically -->
				<status>deposited</status>
				<xsl:apply-templates select="$node/dim:field" mode="inner" />
			</DryadDataPackage>
		</xsl:variable>
		<!-- our workaround -->
		<xsl:apply-templates xmlns:xalan="http://xml.apache.org/xalan"
			select="xalan:nodeset($pubPkgNode)/*" />
		</xsl:template>

	<!-- package and file -->
	<xsl:template mode="inner"
		match="dim:field[@element='identifier'][starts-with(., 'doi:') or starts-with(., 'http://dx.doi')]">
		<dcterms:identifier>
		  <xsl:call-template name="normalize_doi" >
		    <xsl:with-param name="node" select="."/>
		  </xsl:call-template>
		</dcterms:identifier>
	</xsl:template>

	<!-- package and file -->
	<xsl:template mode="inner"
		match="dim:field[@element='contributor'][not(@qualifier='correspondingAuthor')]">
		<dcterms:creator>
			<xsl:value-of select="." />
		</dcterms:creator>
	</xsl:template>

	<!-- publication -->
	<xsl:template mode="inner-pub"
		match="dim:field[@element='contributor'][not(@qualifier='correspondingAuthor')]">
		<dcterms:creator>
			<xsl:value-of select="." />
		</dcterms:creator>
	</xsl:template>

	<!-- publication -->
	<xsl:template mode="inner-pub"
		match="dim:field[@element='date'][@qualifier='issued']">
		<dcterms:issued>
			<xsl:value-of select="." />
		</dcterms:issued>
	</xsl:template>

	<!-- package and file -->
	<xsl:template mode="inner" match="dim:field[@element='title']">
		<dcterms:title>
			<xsl:value-of select="." />
		</dcterms:title>
	</xsl:template>

	<!-- publication -->
	<xsl:template mode="inner-pub" match="dim:field[@element='title']">
		<dcterms:title>
			<!-- Remove 'Data From: ' which == 10 chars + 1 -->
			<xsl:value-of select="substring(., 11)" />
		</dcterms:title>
	</xsl:template>

	<!-- publication -->
	<xsl:template mode="inner-pub"
		match="dim:field[@element='publicationName'][@mdschema='prism']">
		<bibo:Journal>
			<xsl:value-of select="." />
		</bibo:Journal>
	</xsl:template>

	<!-- publication -->
	<xsl:template mode="inner-pub"
		match="dim:field[@element='relation'][@qualifier='isreferencedby'][starts-with(., 'doi') or starts-with(., 'http://dx.doi')]">
		<bibo:doi>
		  <xsl:call-template name="normalize_doi" >
		    <xsl:with-param name="node" select="."/>
		  </xsl:call-template>
		</bibo:doi>
	</xsl:template>

	<!-- package link to the publication DOI -->
	<xsl:template mode="inner"
 		match="dim:field[@element='relation'][@qualifier='isreferencedby'][starts-with(., 'doi') or starts-with(., 'http://dx.doi')]">
		<dcterms:references>
		  <xsl:call-template name="normalize_doi" >
		    <xsl:with-param name="node" select="."/>
		  </xsl:call-template>
		</dcterms:references>
	</xsl:template>


	<!-- publication -->
	<xsl:template mode="inner-pub"
		match="dim:field[@element='relation'][@qualifier='isreferencedby'][starts-with(., 'pmid')]">
		<bibo:pmid>
			<xsl:value-of select="." />
		</bibo:pmid>
	</xsl:template>

	<!-- publication -->
	<xsl:template mode="inner-pub"
		match="dim:field[@element='description'][@qualifier='abstract']">
		<dcterms:abstract>
			<xsl:value-of select="." />
		</dcterms:abstract>
	</xsl:template>

	<!-- publication -->
	<xsl:template mode="inner-pub"
		match="dim:field[@element='identifier'][starts-with(., 'doi:') or starts-with(., 'http://dx.doi')]">
		<dcterms:isReferencedBy>
		  <xsl:call-template name="normalize_doi" >
		    <xsl:with-param name="node" select="."/>
		  </xsl:call-template>
		</dcterms:isReferencedBy>
	</xsl:template>

	<!-- package and file -->
	<xsl:template mode="inner"
		match="dim:field[@element='date'][@qualifier='accessioned']">
		<dcterms:dateSubmitted>
			<xsl:value-of select="." />
		</dcterms:dateSubmitted>
	</xsl:template>

	<!-- package and file -->
	<xsl:template mode="inner"
		match="dim:field[@element='date'][@qualifier='available']">
		<dcterms:available>
			<xsl:value-of select="." />
		</dcterms:available>
	</xsl:template>

	<!-- file -->
	<xsl:template mode="inner"
		match="dim:field[@element='date'][@qualifier='embargoedUntil']">
		<embargoedUntil>
			<xsl:value-of select="." />
		</embargoedUntil>
	</xsl:template>

	<!-- file -->
	<xsl:template mode="inner"
		match="dim:field[@element='rights'][@qualifier='uri']">
		<dcterms:rights>
			<xsl:value-of select="." />
		</dcterms:rights>
	</xsl:template>

	<!-- file -->
	<xsl:template mode="inner"
		match="dim:field[@element='description'][not(@qualifier)]">
		<dcterms:description>
			<xsl:value-of select="." />
		</dcterms:description>
	</xsl:template>

	<!-- package and file -->
	<xsl:template mode="inner" match="dim:field[@element='subject']">
		<dcterms:subject>
			<xsl:value-of select="." />
		</dcterms:subject>
	</xsl:template>

	<!-- package and file -->
	<xsl:template mode="inner"
		match="dim:field[@element='ScientificName'][@mdschema='dwc']">
		<dwc:scientificName>
			<xsl:value-of select="." />
		</dwc:scientificName>
	</xsl:template>

	<!-- package and file -->
	<xsl:template mode="inner"
		match="dim:field[@element='coverage'][@qualifier='spatial']">
		<dcterms:spatial>
			<xsl:value-of select="." />
		</dcterms:spatial>
	</xsl:template>

	<!-- file -->
	<xsl:template mode="inner"
		match="dim:field[@element='bitstreamId'][@mdschema='dryad']">
		<bitstreamId>
			<xsl:value-of select="." />
		</bitstreamId>
	</xsl:template>

	<!-- file -->
	<xsl:template mode="inner" match="dim:field[@element='format']">
		<dcterms:format>
			<xsl:value-of select="." />
		</dcterms:format>
	</xsl:template>

	<!-- file -->
	<xsl:template mode="inner" match="dim:field[@element='extent']">
		<dcterms:extent>
			<xsl:value-of select="." />
		</dcterms:extent>
	</xsl:template>

	<!-- package and file -->
	<xsl:template mode="inner"
		match="dim:field[@element='coverage'][@qualifier='temporal']">
		<dcterms:temporal>
			<xsl:value-of select="." />
		</dcterms:temporal>
	</xsl:template>

	<!-- package -->
	<xsl:template mode="inner"
		match="dim:field[@element='relation'][@qualifier='haspart']">
		<dcterms:hasPart>
		  <xsl:call-template name="normalize_doi" >
		    <xsl:with-param name="node" select="."/>
		  </xsl:call-template>
		</dcterms:hasPart>
	</xsl:template>

	<!-- file -->
	<xsl:template mode="inner"
		match="dim:field[@element='description'][@qualifier='provenance']">
		<dcterms:provenance>
			<xsl:value-of select="." />
		</dcterms:provenance>
	</xsl:template>

	<!-- file -->
	<xsl:template mode="inner"
		match="dim:field[@element='relation'][@qualifier='ispartof']">
	  <!-- We're not using this template for the partof relationships,
	  because they relevant field doesn't contain the DOI yet. See the partof
	  handling in the DryadDataFile section above -->
	</xsl:template>


	<!-- DOI normalization -->
	<!-- Returns the "full form" of a DOI. The input parameter must be a DOI of some form, either full or short. -->
	<xsl:template name="normalize_doi">
	  <xsl:param name="node" select="NO DOI" />
	  <xsl:choose>
	    <xsl:when test="starts-with($node, 'http://dx.doi')">
	      <xsl:value-of select="$node" />
	    </xsl:when>
	    <xsl:when test="starts-with($node, 'doi:')">
	      <xsl:text>http://dx.doi.org/</xsl:text>
	      <xsl:value-of select="substring-after($node, 'doi:')" />
	    </xsl:when>
	    <xsl:otherwise>
	      <!-- do nothing -->
	    </xsl:otherwise>
	  </xsl:choose>
	</xsl:template>

	<!-- String utility template: Returns the substring before the last occurrence of another string. 
	     Particularly useful for removing the last item in a URL.  substring-before-last($URL, '/')
	-->
        <xsl:template name="substring-before-last">
	  <!-- takes two parameters - the string and the match -->
	  <xsl:param name="string" />
	  <xsl:param name="match" />
	  <xsl:variable name="result-with-match">
	    <xsl:call-template name="remove-after-last">
	      <xsl:with-param name="string" select="$string" />
	      <xsl:with-param name="match" select="$match" />
	    </xsl:call-template>
	  </xsl:variable>
	  <xsl:value-of select="substring($result-with-match, 0, string-length($result-with-match))"/>
	</xsl:template>
	
	<!-- String utility template: Remove everything after the last occurrence of $char-->
	<xsl:template name="remove-after-last">
	  <!-- takes two parameters - the string and the match -->
	  <xsl:param name="string" />
	  <xsl:param name="match" />
	  <xsl:choose>
	    <!-- if the string contains the character... -->
	    <xsl:when test="contains($string, $match)">
	      <!-- output the part of the string before the character, including the character -->
	      <xsl:value-of select="substring-before($string, $match)" />
	      <xsl:value-of select="$match" />
	      <!-- call the template recursively... -->
	      <xsl:call-template name="remove-after-last">
		<!-- with the string being the string after the character -->
		<xsl:with-param name="string" select="substring-after($string, $match)" />
		<!-- and the character being the same as before -->
		<xsl:with-param name="match" select="$match" />
	      </xsl:call-template>
	    </xsl:when>
	    <!-- otherwise, return nothing -->
	    <xsl:otherwise></xsl:otherwise>
	  </xsl:choose>
	</xsl:template>


	<!-- administrative bits below here -->

	<xsl:template match="text()">
		<!-- catch and ignore what we don't explictly crosswalk -->
	</xsl:template>

	<xsl:template match="text()" mode="inner">
		<!-- catch and ignore what we don't explictly crosswalk -->
	</xsl:template>

	<xsl:template match="text()" mode="inner-pub">
		<!-- catch and ignore what we don't explictly crosswalk -->
	</xsl:template>

</xsl:stylesheet>
