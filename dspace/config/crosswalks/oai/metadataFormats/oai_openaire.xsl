<?xml version="1.0" encoding="UTF-8"?>
<!-- 

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

	Developed by Paulo Graça <paulo.graca@fccn.pt>
	
	> https://www.openaire.eu/schema/repo-lit/4.0/openaire.xsd

 -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:oaire="http://namespace.openaire.eu/schema/oaire/"
	xmlns:datacite="http://datacite.org/schema/kernel-4"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns:doc="http://www.lyncode.com/xoai"
	xmlns:rdf="http://www.w3.org/TR/rdf-concepts/"
	version="1.0">
   <xsl:output omit-xml-declaration="yes" method="xml" indent="yes" />

   <xsl:template match="/">
      <resource xmlns="http://namespace.openaire.eu/schema/oaire/" xmlns:vc="http://www.w3.org/2007/XMLSchema-versioning" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://namespace.openaire.eu/schema/oaire/ https://www.openaire.eu/schema/repo-lit/4.0/openaire.xsd">
		 <datacite:titles>
			 <!-- datacite.title -->
			 <xsl:if test="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']">
				<xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']" mode="title" />
			 </xsl:if>	
		 </datacite:titles>
		 <xsl:variable name="authors" select="doc:metadata/doc:element[@name='authors']" />
		 <datacite:creators>
             <!-- datacite.creator -->		 			 
			 <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value']">
				<xsl:variable name="sibling1" select="following-sibling::*[1]" />
				<!-- authority? -->
				<xsl:variable name="sibling2" select="following-sibling::*[2]" />
				<!-- confidence? -->
				<xsl:if test="not($sibling1[@name='authority'])">
				   <xsl:call-template name="dc_contributor_author">
					  <xsl:with-param name="author" select="." />
				   </xsl:call-template>
				</xsl:if>
				<xsl:if test="$sibling1[@name='authority']">
				   <xsl:variable name="authority_id" select="$sibling1[1]/text()" />
				   <xsl:if test="$authority_id">
					  <xsl:variable name="author_authority" select="$authors/doc:element[@name='author']/doc:field[@name='uuid' and .=$authority_id]/.." />
					  <xsl:if test="$author_authority">
						 <xsl:apply-templates select="$author_authority" mode="creator" />
					  </xsl:if>
					  <xsl:if test="not($author_authority)">
						 <xsl:call-template name="dc_contributor_author">
							<xsl:with-param name="author" select="." />
						 </xsl:call-template>
					  </xsl:if>
				   </xsl:if>
				</xsl:if>
			 </xsl:for-each>
		 </datacite:creators>
		 <datacite:contributors>
			 <!-- datacite.contributor / dc.contributor.advisor -->
			 <xsl:call-template name="dc_contributors">
				<xsl:with-param name="authors" select="$authors" />
				<xsl:with-param name="contributors" select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name!='author']" />
			 </xsl:call-template>
			 <!-- datacite:contributor repository -->
			 <xsl:if test="doc:metadata/doc:element[@name='repository']/doc:field[@name='name']">
				<datacite:contributor contributorType="HostingInstitution">
				   <datacite:contributorName nameType="Organizational">
					  <xsl:value-of select="doc:metadata/doc:element[@name='repository']/doc:field[@name='name']" />
				   </datacite:contributorName>
				   <xsl:variable name="mail" select="doc:metadata/doc:element[@name='repository']/doc:field[@name='mail']" />
				   <xsl:if test="$mail">
					  <datacite:nameIdentifier>
						 <xsl:attribute name="nameIdentifierScheme">
							<xsl:text>e-mail</xsl:text>
						 </xsl:attribute>
						 <xsl:attribute name="schemeURI">
							<xsl:text>mailto:</xsl:text>
							<xsl:value-of select="$mail" />
						 </xsl:attribute>
						 <xsl:value-of select="$mail" />
					  </datacite:nameIdentifier>
				   </xsl:if>
				</datacite:contributor>
			 </xsl:if>		 
         </datacite:contributors>
		 
		 <!-- oaire:fundingReference-->
		 <xsl:if test="doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']">
			 <fundingReferences>
				 <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element/doc:field[@name='value' and contains(text(),'info:eu-repo/grantAgreement')]">
					<xsl:apply-templates select="." mode="oaire" />
				 </xsl:for-each>			 
			 </fundingReferences>
		 </xsl:if>

         <!-- datacite:alternateIdentifier -->
		 <datacite:alternateIdentifiers>
			 <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='tid' or @name='TID']">
				<xsl:apply-templates select="." mode="datacite" />
			 </xsl:for-each>
			 <xsl:for-each select="doc:metadata/doc:element[@name='datacite']/doc:element[@name='alternateIdentifier']/doc:element">
				<xsl:apply-templates select="." mode="datacite" />
			 </xsl:for-each>			 
		 </datacite:alternateIdentifiers>
         <!-- datacite:relatedIdentifier -->
		 <datacite:relatedIdentifiers>
			 <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element">
				<xsl:apply-templates select="." mode="datacite" />
			 </xsl:for-each>
			 <xsl:for-each select="doc:metadata/doc:element[@name='datacite']/doc:element[@name='relatedIdentifier']/doc:element">
				<xsl:apply-templates select="." mode="datacite" />
			 </xsl:for-each>			 
			 <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='publisherversion']/doc:element">
				<xsl:apply-templates select="." mode="datacite" />
			 </xsl:for-each>		 
			 <xsl:for-each select="doc:metadata/doc:element[@name='rcaap']/doc:element[@name='publisherversion']/doc:element">
				<xsl:apply-templates select="." mode="datacite" />
			 </xsl:for-each>		 
		 </datacite:relatedIdentifiers>         
		 <datacite:dates>
		 <!-- datacite:date (embargo) -->
         <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element">
            <xsl:apply-templates select="." mode="datacite" />
         </xsl:for-each>
		 </datacite:dates>
         <!-- dc.language -->
         <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element/doc:field[@name='value']">
            <dc:language>
               <xsl:value-of select="." />
            </dc:language>
         </xsl:for-each>
         <!-- dc.language.* -->
         <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element/doc:element/doc:field[@name='value']">
            <dc:language>
               <xsl:value-of select="." />
            </dc:language>
         </xsl:for-each>
         <!-- dc.publisher -->
         <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='publisher']/doc:element/doc:field[@name='value']">
            <dc:publisher>
               <xsl:value-of select="." />
            </dc:publisher>
         </xsl:for-each>
         <!-- dc.publisher.* -->
         <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='publisher']/doc:element/doc:element/doc:field[@name='value']">
            <dc:publisher>
               <xsl:value-of select="." />
            </dc:publisher>
         </xsl:for-each>
         
         <!-- resourceType -->
         <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value']">
            <xsl:apply-templates select="../.." mode="coartype" />
         </xsl:for-each>
         <!-- resourceType.* -->
         <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:element/doc:field[@name='value']">
            <xsl:apply-templates select="../../.." mode="coartype" />
         </xsl:for-each>
         <!-- dc.description -->
         <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element/doc:field[@name='value']">
            <dc:description>
               <xsl:call-template name="xmlLanguage">
                  <xsl:with-param name="name" select="../@name" />
               </xsl:call-template>
               <xsl:value-of select="." />
            </dc:description>
         </xsl:for-each>		 
         <!-- dc.description.* (not provenance)-->
         <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name!='provenance' and @name!='version']/doc:element/doc:field[@name='value']">
            <dc:description>
               <xsl:call-template name="xmlLanguage">
                  <xsl:with-param name="name" select="../@name" />
               </xsl:call-template>
               <xsl:value-of select="." />
            </dc:description>
         </xsl:for-each>
         <!-- dc.format -->
         <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='format']/doc:element/doc:field[@name='value']">
            <dc:format>
               <xsl:call-template name="xmlLanguage">
                  <xsl:with-param name="name" select="../@name" />
               </xsl:call-template>
               <xsl:value-of select="." />
            </dc:format>
         </xsl:for-each>
         <!-- dc.format.* -->
         <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='format']/doc:element/doc:element/doc:field[@name='value']">
            <dc:format>
               <xsl:call-template name="xmlLanguage">
                  <xsl:with-param name="name" select="../@name" />
               </xsl:call-template>
               <xsl:value-of select="." />
            </dc:format>
         </xsl:for-each>
         <!-- dc.format.* -->
         <xsl:for-each select="doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle']">
            <xsl:if test="doc:field[@name='name' and text()='ORIGINAL']">
               <xsl:for-each select="doc:element[@name='bitstreams']/doc:element[@name='bitstream']">
                  <dc:format>
                     <xsl:value-of select="doc:field[@name='format']" />
                  </dc:format>
               </xsl:for-each>
            </xsl:if>
         </xsl:for-each>
         <!-- datacite.identifier -->
		 <xsl:if test="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']">
				<xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']" mode="dc" />
	     </xsl:if>
         <!-- datacite.rights -->
         <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element/doc:field[@name='value']">
            <xsl:apply-templates select=".." mode="coarrights" />
         </xsl:for-each>
         <!-- datacite.rights.* -->
         <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element/doc:element/doc:field[@name='value']">
            <xsl:apply-templates select=".." />
         </xsl:for-each>
         <!-- dc.source -->
         <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='source']/doc:element/doc:field[@name='value']">
            <dc:source>
               <xsl:call-template name="xmlLanguage">
                  <xsl:with-param name="name" select="../@name" />
               </xsl:call-template>
               <xsl:value-of select="." />
            </dc:source>
         </xsl:for-each>
         <!-- dc.source.* -->
         <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='source']/doc:element/doc:element/doc:field[@name='value']">
            <dc:source>
               <xsl:call-template name="xmlLanguage">
                  <xsl:with-param name="name" select="../@name" />
               </xsl:call-template>
               <xsl:value-of select="." />
            </dc:source>
         </xsl:for-each>
		 <datacite:subjects>
			 <!-- datacite.subject -->
			 <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']" mode="dc" />
		 </datacite:subjects>  
         <!-- dc.coverage -->
         <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='coverage']/doc:element/doc:field[@name='value']">
            <dc:coverage>
               <xsl:value-of select="." />
            </dc:coverage>
         </xsl:for-each>
         <!-- dc.coverage.* -->
         <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='coverage']/doc:element/doc:element/doc:field[@name='value']">
            <dc:coverage>
               <xsl:value-of select="." />
            </dc:coverage>
         </xsl:for-each>		          
		 <datacite:sizes>
			 <!-- datacite:size -->
			 <xsl:for-each select="doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle']">
				<xsl:if test="doc:field[@name='name' and text()='ORIGINAL']">
				   <xsl:for-each select="doc:element[@name='bitstreams']/doc:element[@name='bitstream']">
					  <datacite:size>
						 <xsl:value-of select="concat(doc:field[@name='size'],' bytes')" />
					  </datacite:size>
				   </xsl:for-each>
				</xsl:if>
			 </xsl:for-each>
		 </datacite:sizes>
         <!-- 
		 <datacite:geoLocations>
		  <datacite:geoLocation>
			<datacite:geoLocationPlace>Atlantic Ocean</datacite:geoLocationPlace>
			<datacite:geoLocationPoint>
					 <datacite:pointLongitude>31.233</datacite:pointLongitude>
					 <datacite:pointLatitude>-67.302</datacite:pointLatitude>
			</datacite:geoLocationPoint>
			<datacite:geoLocationBox>
					 <datacite:westBoundLongitude>-71.032</datacite:westBoundLongitude>
					 <datacite:eastBoundLongitude>-68.211</datacite:eastBoundLongitude>
					 <datacite:southBoundLongitude>41.090</datacite:southBoundLongitude>
					 <datacite:northBoundLongitude>42.893</datacite:northBoundLongitude>
			</datacite:geoLocationBox>
		  </datacite:geoLocation>
		</datacite:geoLocations>
         -->
         <!-- version -->
         <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='version']/doc:element">
            <xsl:apply-templates select="." mode="openaire" />
         </xsl:for-each>
		 <xsl:for-each select="doc:metadata/doc:element[@name='oaire']/doc:element[@name='version']/doc:element">
            <xsl:apply-templates select="." mode="openaire" />
         </xsl:for-each>
         <!-- file -->
         <xsl:for-each select="doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle']">
            <xsl:apply-templates select="." mode="bundle"/>
         </xsl:for-each>
		<!-- degois>>oaire:Citation -->
         <xsl:if test="doc:metadata/doc:element[@name='degois']">
            <xsl:apply-templates select="doc:metadata/doc:element[@name='degois']" mode="oaire" />
         </xsl:if>
		 <!-- oaire:Citation -->
         <xsl:if test="doc:metadata/doc:element[@name='oaire']">
            <xsl:apply-templates select="doc:metadata/doc:element[@name='oaire']" mode="oaire" />
         </xsl:if>		 
		<!--dcterms:audience>Researchers</dcterms:audience-->
      </resource>
   </xsl:template>
   

   <xsl:template match="doc:element[@name='dc']/doc:element[@name='title']" mode="title">
	     <!-- datacite.title -->
         <xsl:for-each select="./doc:element/doc:field[@name='value']">
            <datacite:title>
               <xsl:call-template name="xmlLanguage">
                  <xsl:with-param name="name" select="../@name" />
               </xsl:call-template>
               <xsl:value-of select="." />
            </datacite:title>
         </xsl:for-each>
         <!-- datacite.title.* -->
         <xsl:for-each select="./doc:element/doc:element/doc:field[@name='value']">
			<xsl:variable name="lc_title_type">
				 <xsl:call-template name="lowercase">
					<xsl:with-param name="value" select="../../@name" />
				 </xsl:call-template>
			</xsl:variable>		 
            <datacite:title>
               <xsl:call-template name="xmlLanguage">
                  <xsl:with-param name="name" select="../@name" />
               </xsl:call-template>
			   <xsl:attribute name="titleType">
				<xsl:choose>
				   <xsl:when test="$lc_title_type = 'alternativetitle' or $lc_title_type = 'alternative'">
					  <xsl:text>AlternativeTitle</xsl:text>
				   </xsl:when>
				   <xsl:when test="$lc_title_type = 'subtitle'">
					  <xsl:text>Subtitle</xsl:text>
				   </xsl:when>
				   <xsl:when test="$lc_title_type = 'translatedtitle'">
					  <xsl:text>TranslatedTitle</xsl:text>
				   </xsl:when>
				   <xsl:otherwise>
					  <xsl:text>Other</xsl:text>
				   </xsl:otherwise>
				</xsl:choose>
				</xsl:attribute>
               <xsl:value-of select="." />
            </datacite:title>
         </xsl:for-each>
   </xsl:template>
   
   
   
   <xsl:template name="dc_contributor_author">
      <xsl:param name="author" />
      <datacite:creator>
         <datacite:creatorName>
            <xsl:value-of select="$author" />
         </datacite:creatorName>
      </datacite:creator>
   </xsl:template>
   
   
   
   <xsl:template match="doc:element[@name='author']" mode="creator">
      <datacite:creator>
		 <!-- Get the author name from the publication -->
		 <xsl:variable name="author_authority" select="./doc:field[@name='uuid']/text()" />
		 <xsl:variable name="author_name" select="//doc:field[@name='authority' and ./text()=$author_authority]/preceding-sibling::*[1]" />
		 <datacite:creatorName nameType="Personal">
            <xsl:value-of select="$author_name/text()" />
         </datacite:creatorName>
         <!-- creatorName  -->
         <!--xsl:for-each select="doc:element/doc:element[@name='author']/doc:element/doc:field[@name='value']">
            <datacite:creatorName nameType="Personal">
               <xsl:value-of select="." />
            </datacite:creatorName>
         </xsl:for-each-->
         <!-- first name -->
         <xsl:for-each select="doc:element/doc:element[@name='name']/doc:element[@name='first']/doc:element/doc:field[@name='value']">
            <datacite:givenName>
               <xsl:value-of select="." />
            </datacite:givenName>
         </xsl:for-each>
         <!-- last name -->
         <xsl:for-each select="doc:element/doc:element[@name='name']/doc:element[@name='last']/doc:element/doc:field[@name='value']">
            <datacite:familyName>
               <xsl:value-of select="." />
            </datacite:familyName>
         </xsl:for-each>
         <!-- id -->
         <xsl:for-each select="doc:element/doc:element[@name='id']/doc:element">
            <xsl:variable name="nameIdentifierScheme" select="@name" />
            <xsl:for-each select="doc:element/doc:field[@name='value']">
               <!-- id/type -->
               <datacite:nameIdentifier>
                  <xsl:attribute name="nameIdentifierScheme">
                     <xsl:value-of select="$nameIdentifierScheme" />
                  </xsl:attribute>
                  <xsl:value-of select="." />
               </datacite:nameIdentifier>
            </xsl:for-each>
         </xsl:for-each>
         <!-- affiliation -->
         <xsl:for-each select="doc:element/doc:element[@name='affiliation']/doc:element/doc:field[@name='value']">
            <datacite:affiliation>
               <xsl:value-of select="." />
            </datacite:affiliation>
         </xsl:for-each>
      </datacite:creator>
   </xsl:template>
   
   
   
   <xsl:template name="dc_contributors">
      <xsl:param name="authors" />
      <xsl:param name="contributors" />
      <xsl:for-each select="$contributors">
         <xsl:variable name="sibling1" select="following-sibling::*[1]" />
         <!-- authority? -->
         <xsl:variable name="sibling2" select="following-sibling::*[2]" />
         <!-- confidence? -->
         <xsl:if test="not($sibling1[@name='authority'])">
            <xsl:if test="@name='none'">
               <xsl:for-each select="./doc:field[@name='value']">
                  <!-- process: dc.contributor -->
                  <xsl:call-template name="dc_contributor">
                     <xsl:with-param name="contributor" select="../." />
                  </xsl:call-template>
               </xsl:for-each>
            </xsl:if>
            <!-- process: dc.contributor.other -->
            <xsl:for-each select="./doc:element/doc:field[@name='value']">
               <xsl:call-template name="dc_contributor">
                  <xsl:with-param name="contributor" select="../../." />
               </xsl:call-template>
            </xsl:for-each>
         </xsl:if>
         <xsl:if test="$sibling1[@name='authority']">
            <xsl:variable name="authority_id" select="$sibling1[1]/text()" />
            <xsl:if test="$authority_id">
               <xsl:apply-templates select="$authors/doc:element[@name='author']/doc:field[@name='uuid' and .=$authority_id]/.." mode="contributor">
                  <xsl:with-param name="contributor" select="." />
               </xsl:apply-templates>
            </xsl:if>
         </xsl:if>
      </xsl:for-each>
   </xsl:template>
   
   
   
   <xsl:template name="dc_contributor">
      <xsl:param name="contributor" />
      <xsl:variable name="lc_contributor_type">
         <xsl:call-template name="lowercase">
            <xsl:with-param name="value" select="$contributor/@name" />
         </xsl:call-template>
      </xsl:variable>
      <datacite:contributor>
         <xsl:attribute name="contributorType">
            <xsl:choose>
               <xsl:when test="$lc_contributor_type = 'advisor' or $lc_contributor_type = 'supervisor'">
                  <xsl:text>Supervisor</xsl:text>
               </xsl:when>
               <xsl:when test="$lc_contributor_type = 'editor'">
                  <xsl:text>Editor</xsl:text>
               </xsl:when>
               <xsl:otherwise>
                  <xsl:text>Other</xsl:text>
               </xsl:otherwise>
            </xsl:choose>
         </xsl:attribute>
         <datacite:contributorName>
            <xsl:value-of select="./text()" />
         </datacite:contributorName>
      </datacite:contributor>
   </xsl:template>
   
   
   
   <xsl:template match="doc:element[@name='author']" mode="contributor">
      <!--enumeration value="ContactPerson"/>
      <enumeration value="DataCollector"/>
      <enumeration value="DataCurator"/>
      <enumeration value="DataManager"/>
      <enumeration value="Distributor"/>
      <enumeration value="Editor"/>
      <enumeration value="HostingInstitution"/>
      <enumeration value="Other"/>
      <enumeration value="Producer"/>
      <enumeration value="ProjectLeader"/>
      <enumeration value="ProjectManager"/>
      <enumeration value="ProjectMember"/>
      <enumeration value="RegistrationAgency"/>
      <enumeration value="RegistrationAuthority"/>
      <enumeration value="RelatedPerson"/>
      <enumeration value="ResearchGroup"/>
      <enumeration value="RightsHolder"/>
      <enumeration value="Researcher"/>
      <enumeration value="Sponsor"/>
      <enumeration value="Supervisor"/>
      <enumeration value="WorkPackageLeader"/-->
      <xsl:param name="contributor" />
      <xsl:variable name="lc_contributor_type">
         <xsl:call-template name="lowercase">
            <xsl:with-param name="value" select="$contributor/@name" />
         </xsl:call-template>
      </xsl:variable>
      <datacite:contributor>
         <xsl:attribute name="contributorType">
            <xsl:choose>
               <xsl:when test="$lc_contributor_type = 'advisor' or $lc_contributor_type = 'supervisor'">
                  <xsl:text>Supervisor</xsl:text>
               </xsl:when>
               <xsl:when test="$lc_contributor_type = 'editor'">
                  <xsl:text>Editor</xsl:text>
               </xsl:when>
               <xsl:otherwise>
                  <xsl:text>Other</xsl:text>
               </xsl:otherwise>
            </xsl:choose>
         </xsl:attribute>
         <!-- contributorName  -->
         <xsl:for-each select="doc:element/doc:element[@name='author']/doc:element/doc:field[@name='value']">
            <datacite:contributorName>
               <xsl:value-of select="." />
            </datacite:contributorName>
         </xsl:for-each>
         <!-- first name -->
         <xsl:for-each select="doc:element/doc:element[@name='name']/doc:element[@name='first']/doc:element/doc:field[@name='value']">
            <datacite:givenName>
               <xsl:value-of select="." />
            </datacite:givenName>
         </xsl:for-each>
         <!-- last name -->
         <xsl:for-each select="doc:element/doc:element[@name='name']/doc:element[@name='last']/doc:element/doc:field[@name='value']">
            <datacite:familyName>
               <xsl:value-of select="." />
            </datacite:familyName>
         </xsl:for-each>
         <!-- id -->
         <xsl:for-each select="doc:element/doc:element[@name='id']/doc:element">
            <xsl:variable name="nameIdentifierScheme" select="@name" />
            <xsl:for-each select="doc:element/doc:field[@name='value']">
               <!-- id/type -->
               <datacite:nameIdentifier>
                  <xsl:attribute name="nameIdentifierScheme">
                     <xsl:value-of select="$nameIdentifierScheme" />
                  </xsl:attribute>
                  <xsl:value-of select="." />
               </datacite:nameIdentifier>
            </xsl:for-each>
         </xsl:for-each>
         <!-- affiliation -->
         <xsl:for-each select="doc:element/doc:element[@name='affiliation']/doc:element/doc:field[@name='value']">
            <datacite:affiliation>
               <xsl:value-of select="." />
            </datacite:affiliation>
         </xsl:for-each>
      </datacite:contributor>
   </xsl:template>
   
   
   <!-- oaire.fundingReference -->
   <xsl:template match="doc:element[@name='dc']/doc:element[@name='relation']/doc:element/doc:field" mode="oaire">        
		<xsl:variable name="eu_prefix" select="'info:eu-repo/grantAgreement/'" />
		<xsl:variable name="uri_stripped" select="substring-after(./text(),$eu_prefix)" />
		<xsl:variable name="funder" select="substring-before($uri_stripped,'/')" />
		<xsl:variable name="program" select="substring-before(substring-after($uri_stripped,concat($funder,'/')),'/')" />
		<xsl:variable name="awardNumber" select="substring-before(substring-after($uri_stripped,concat($funder,'/',$program,'/')),'/')" />
		
		<xsl:variable name="funder_name">
			<xsl:call-template name="funder_name">
					<xsl:with-param name="funder" select="$funder" />
				</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="funder_doi">
			<xsl:text>http://doi.org/</xsl:text>
			<xsl:call-template name="crossref_funder_id">
				<xsl:with-param name="funder" select="$funder" />
			</xsl:call-template>		
		</xsl:variable>
				
		<fundingReference xmlns="http://namespace.openaire.eu/schema/oaire/">
			<xsl:if test="$funder_name">
				<funderName>
					<xsl:value-of select="$funder_name"/>
				</funderName>
			</xsl:if>
			<xsl:if test="$funder_doi">
				<funderIdentifier funderIdentifierType="Crossref Funder ID">
					<xsl:value-of select="$funder_doi"/>
				</funderIdentifier>
			</xsl:if>
			<xsl:if test="$program">			
				<fundingStream><xsl:value-of select="$program"/></fundingStream>
			</xsl:if>
			<xsl:if test="$awardNumber">
				<awardNumber><xsl:value-of select="$awardNumber"/></awardNumber>
			</xsl:if>
			<!--awardTitle>Open Access Infrastructure for Research in Europe 2020</awardTitle-->
		</fundingReference>
   </xsl:template>
   
   <!-- 
	This template is temporary.
	it's only purpose it to provide a funders name based on the URI
   -->
   <xsl:template name="funder_name">
	    <xsl:param name="funder"/>
		<xsl:choose>
			<xsl:when test="$funder='EC'">European Commission</xsl:when>
			<xsl:when test="$funder='FCT'">Fundação para a Ciência e Tecnologia</xsl:when>
			<xsl:when test="$funder='WT'">Welcome Trust</xsl:when>
			<xsl:otherwise><xsl:value-of select="$funder"/></xsl:otherwise>
		</xsl:choose>
   </xsl:template>
   
   <!-- 
	This template is temporary.
	it's only purpose it to provide a funders doi based on the URI
   -->   
   <xsl:template name="crossref_funder_id">
	    <xsl:param name="funder"/>
		<xsl:choose>
			<xsl:when test="$funder='EC'">10.13039/501100008530</xsl:when>
			<xsl:when test="$funder='FCT'">10.13039/501100001871</xsl:when>
			<xsl:when test="$funder='WT'">10.13039/100010269</xsl:when>
		</xsl:choose>
   </xsl:template>   
   
   
   
   <!-- datacite.date -->
   <xsl:template match="doc:element[@name='dc']/doc:element[@name='date']/doc:element" mode="datacite">
      <xsl:variable name="dc_date_type" select="@name" />
      <xsl:variable name="dc_date_value" select="doc:element/doc:field[@name='value']/text()" />
      <xsl:variable name="lc_dc_date_type">
         <xsl:call-template name="lowercase">
            <xsl:with-param name="value" select="$dc_date_type" />
         </xsl:call-template>
      </xsl:variable>
      <datacite:date>
         <xsl:choose>            
            <xsl:when test="$lc_dc_date_type='available' or  $lc_dc_date_type = 'embargo'">
               <xsl:attribute name="dateType">
                  <xsl:text>Available</xsl:text>
               </xsl:attribute>
            </xsl:when>
            <xsl:when test="$lc_dc_date_type='collected'">
               <xsl:attribute name="dateType">
                  <xsl:text>Collected</xsl:text>
               </xsl:attribute>
            </xsl:when>
            <xsl:when test="$lc_dc_date_type='copyrighted' or $lc_dc_date_type='copyright'">
               <xsl:attribute name="dateType">
                  <xsl:text>Copyrighted</xsl:text>
               </xsl:attribute>
            </xsl:when>
            <xsl:when test="$lc_dc_date_type='created'">
               <xsl:attribute name="dateType">
                  <xsl:text>Created</xsl:text>
               </xsl:attribute>
            </xsl:when>
            <!--xsl:when test="$lc_dc_date_type='issued'">
               <xsl:attribute name="dateType">
                  <xsl:text>Issued</xsl:text>
               </xsl:attribute>               
            </xsl:when-->
            <xsl:when test="$lc_dc_date_type='submitted'">
               <xsl:attribute name="dateType">
                  <xsl:text>Submitted</xsl:text>
               </xsl:attribute>
            </xsl:when>
            <xsl:when test="$lc_dc_date_type='updated'">
               <xsl:attribute name="dateType">
                  <xsl:text>Updated</xsl:text>
               </xsl:attribute>
            </xsl:when>
            <xsl:when test="$lc_dc_date_type='valid'">
               <xsl:attribute name="dateType">
                  <xsl:text>Valid</xsl:text>
               </xsl:attribute>
            </xsl:when>
         </xsl:choose>
		 <xsl:value-of select="$dc_date_value" />
      </datacite:date>
   </xsl:template>
   
   <!-- datacite.date |issued|accepted -->
   <xsl:template match="doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued' or @name='accepted']" mode="datacite">
	  <xsl:variable name="dc_date_value" select="doc:element/doc:field[@name='value']/text()" />	  
	  <datacite:date dateType="Accepted">
		<xsl:value-of select="$dc_date_value" />
	  </datacite:date>	  
      <datacite:date dateType="Issued">
		<xsl:value-of select="$dc_date_value" />
	  </datacite:date>	  
   </xsl:template> 
   
   <!-- datacite.date |accessioned -->
   <xsl:template match="doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='accessioned']" mode="datacite">
   </xsl:template> 
      
   <!-- dc.type -->
   <xsl:template match="doc:element[@name='dc']/doc:element[@name='type']" mode="coartype">
      <xsl:variable name="dc_type" select="doc:element/doc:field[@name='value']/text()" />
      <xsl:variable name="lc_dc_type">
         <xsl:call-template name="lowercase">
            <xsl:with-param name="value" select="$dc_type" />
         </xsl:call-template>
      </xsl:variable>
      <resourceType xmlns="http://namespace.openaire.eu/schema/oaire/">
         <xsl:choose>
            <xsl:when test="$lc_dc_type = 'annotation' or $dc_type = 'http://purl.org/coar/resource_type/c_1162'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_1162</xsl:text>
               </xsl:attribute>
               <xsl:text>annotation</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'journal'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_0640</xsl:text>
               </xsl:attribute>
               <xsl:text>journal</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'journal article' or $lc_dc_type = 'article' or $lc_dc_type = 'journalarticle' or $dc_type = 'http://purl.org/coar/resource_type/c_6501'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_6501</xsl:text>
               </xsl:attribute>
			   <xsl:attribute name="resourceTypeGeneral">
                  <xsl:text>literature</xsl:text>
               </xsl:attribute>
               <xsl:text>journal article</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'editorial' or $dc_type = 'http://purl.org/coar/resource_type/c_b239'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_b239</xsl:text>
               </xsl:attribute>
               <xsl:text>editorial</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'bachelor thesis' or $lc_dc_type = 'bachelorthesis' or $dc_type = 'http://purl.org/coar/resource_type/c_7a1f'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_7a1f</xsl:text>
               </xsl:attribute>
               <xsl:text>bachelor thesis</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'bibliography' or $dc_type = 'http://purl.org/coar/resource_type/c_86bc'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_86bc</xsl:text>
               </xsl:attribute>
               <xsl:text>bibliography</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'book' or $dc_type = 'http://purl.org/coar/resource_type/c_2f33'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_2f33</xsl:text>
               </xsl:attribute>
			   <xsl:attribute name="resourceTypeGeneral">
                  <xsl:text>literature</xsl:text>
               </xsl:attribute>
               <xsl:text>book</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'book part' or $lc_dc_type = 'bookpart' or $dc_type = 'http://purl.org/coar/resource_type/c_3248'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_3248</xsl:text>
               </xsl:attribute>
               <xsl:text>book part</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'book review' or $lc_dc_type = 'bookreview' or $dc_type = 'http://purl.org/coar/resource_type/c_ba08'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_ba08</xsl:text>
               </xsl:attribute>
               <xsl:text>book review</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'website' or $dc_type = 'http://purl.org/coar/resource_type/c_7ad9'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_7ad9</xsl:text>
               </xsl:attribute>
               <xsl:text>website</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'interactive resource' or $lc_dc_type = 'interactiveresource' or $dc_type = 'http://purl.org/coar/resource_type/c_e9a0'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_e9a0</xsl:text>
               </xsl:attribute>
               <xsl:text>interactive resource</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'conference proceedings' or $lc_dc_type = 'conferenceproceedings' or $dc_type = 'http://purl.org/coar/resource_type/c_f744'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_f744</xsl:text>
               </xsl:attribute>
               <xsl:text>conference proceedings</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'conference object' or $lc_dc_type = 'conferenceobject' or $dc_type = 'http://purl.org/coar/resource_type/c_c94f'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_c94f</xsl:text>
               </xsl:attribute>
               <xsl:text>conference object</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'conferencepaper' or $dc_type = 'http://purl.org/coar/resource_type/c_5794'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_5794</xsl:text>
               </xsl:attribute>
               <xsl:text>conference paper</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'conferenceposter' or $dc_type = 'http://purl.org/coar/resource_type/c_6670'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_6670</xsl:text>
               </xsl:attribute>
               <xsl:text>conference poster</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'contributiontojournal' or $dc_type = 'http://purl.org/coar/resource_type/c_3e5a'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_3e5a</xsl:text>
               </xsl:attribute>
               <xsl:text>contribution to journal</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'datapaper' or $dc_type = 'http://purl.org/coar/resource_type/c_beb9'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_beb9</xsl:text>
               </xsl:attribute>
               <xsl:text>data paper</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'dataset' or $dc_type = 'http://purl.org/coar/resource_type/c_ddb1'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_ddb1</xsl:text>
               </xsl:attribute>
			   <xsl:attribute name="resourceTypeGeneral">
                  <xsl:text>dataset</xsl:text>
               </xsl:attribute>
               <xsl:text>dataset</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'doctoralthesis' or $dc_type = 'http://purl.org/coar/resource_type/c_db06'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_db06</xsl:text>
               </xsl:attribute>
               <xsl:text>doctoral thesis</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'image' or $dc_type = 'http://purl.org/coar/resource_type/c_c513'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_c513</xsl:text>
               </xsl:attribute>
               <xsl:text>image</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'lecture' or $dc_type = 'http://purl.org/coar/resource_type/c_8544'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_8544</xsl:text>
               </xsl:attribute>
               <xsl:text>lecture</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'letter' or $dc_type = 'http://purl.org/coar/resource_type/c_0857'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_0857</xsl:text>
               </xsl:attribute>
               <xsl:text>letter</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'masterthesis' or $dc_type = 'http://purl.org/coar/resource_type/c_bdcc'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_bdcc</xsl:text>
               </xsl:attribute>
               <xsl:text>master thesis</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'movingimage' or $dc_type = 'http://purl.org/coar/resource_type/c_8a7e'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_8a7e</xsl:text>
               </xsl:attribute>
               <xsl:text>moving image</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'periodical' or $dc_type = 'http://purl.org/coar/resource_type/c_2659'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_2659</xsl:text>
               </xsl:attribute>
               <xsl:text>periodical</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'lettertotheeditor' or $dc_type = 'http://purl.org/coar/resource_type/c_545b'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_545b</xsl:text>
               </xsl:attribute>
               <xsl:text>letter to the editor</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'patent' or $dc_type = 'http://purl.org/coar/resource_type/c_15cd'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_15cd</xsl:text>
               </xsl:attribute>
               <xsl:text>patent</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'preprint' or $dc_type = 'http://purl.org/coar/resource_type/c_816b'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_816b</xsl:text>
               </xsl:attribute>
               <xsl:text>preprint</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'report' or $dc_type = 'http://purl.org/coar/resource_type/c_93fc'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_93fc</xsl:text>
               </xsl:attribute>
               <xsl:text>report</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'reportpart' or $dc_type = 'http://purl.org/coar/resource_type/c_ba1f'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_ba1f</xsl:text>
               </xsl:attribute>
               <xsl:text>report part</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'researchproposal' or $dc_type = 'http://purl.org/coar/resource_type/c_baaf'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_baaf</xsl:text>
               </xsl:attribute>
               <xsl:text>research proposal</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'review' or $dc_type = 'http://purl.org/coar/resource_type/c_efa0'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_efa0</xsl:text>
               </xsl:attribute>
               <xsl:text>review</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'software' or $dc_type = 'http://purl.org/coar/resource_type/c_5ce6'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_5ce6</xsl:text>
               </xsl:attribute>
			   <xsl:attribute name="resourceTypeGeneral">
                  <xsl:text>software</xsl:text>
               </xsl:attribute>
               <xsl:text>software</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'still image' or $lc_dc_type = 'stillimage' or $dc_type = 'http://purl.org/coar/resource_type/c_ecc8'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_ecc8</xsl:text>
               </xsl:attribute>
               <xsl:text>still image</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'technical documentation' or $lc_dc_type = 'technicaldocumentation' or $dc_type = 'http://purl.org/coar/resource_type/c_71bd'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_71bd</xsl:text>
               </xsl:attribute>
               <xsl:text>technical documentation</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'workflow' or $dc_type = 'http://purl.org/coar/resource_type/c_393c'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_393c</xsl:text>
               </xsl:attribute>
               <xsl:text>workflow</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'working paper' or $lc_dc_type = 'workingpaper' or $dc_type = 'http://purl.org/coar/resource_type/c_8042'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_8042</xsl:text>
               </xsl:attribute>
               <xsl:text>working paper</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'thesis' or $dc_type = 'http://purl.org/coar/resource_type/c_46ec'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_46ec</xsl:text>
               </xsl:attribute>
               <xsl:text>thesis</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'cartographic material' or $lc_dc_type = 'cartographicmaterial' or $dc_type = 'http://purl.org/coar/resource_type/c_12cc'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_12cc</xsl:text>
               </xsl:attribute>
               <xsl:text>cartographic material</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'map' or $dc_type = 'http://purl.org/coar/resource_type/c_12cd'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_12cd</xsl:text>
               </xsl:attribute>
               <xsl:text>map</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'video' or $dc_type = 'http://purl.org/coar/resource_type/c_12ce'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_12ce</xsl:text>
               </xsl:attribute>
               <xsl:text>video</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'sound' or $dc_type = 'http://purl.org/coar/resource_type/c_18cc'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_18cc</xsl:text>
               </xsl:attribute>
               <xsl:text>sound</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'musical composition' or $lc_dc_type = 'musicalcomposition' or $dc_type = 'http://purl.org/coar/resource_type/c_18cd'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_18cd</xsl:text>
               </xsl:attribute>
               <xsl:text>musical composition</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'text' or $dc_type = 'http://purl.org/coar/resource_type/c_18cf'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_18cf</xsl:text>
               </xsl:attribute>
               <xsl:text>text</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'conference paper not in proceedings' or $lc_dc_type = 'conferencepapernotinproceedings' or $dc_type = 'http://purl.org/coar/resource_type/c_18cp'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_18cp</xsl:text>
               </xsl:attribute>
               <xsl:text>conference paper not in proceedings</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'conference poster not in proceedings' or $lc_dc_type = 'conferenceposternotinproceedings' or $dc_type = 'http://purl.org/coar/resource_type/c_18co'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_18co</xsl:text>
               </xsl:attribute>
               <xsl:text>conference poster not in proceedings</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'musical notation' or $dc_type = 'http://purl.org/coar/resource_type/c_18cw'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_18cw</xsl:text>
               </xsl:attribute>
               <xsl:text>musical notation</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'internal report' or $lc_dc_type = 'internalreport' or $dc_type = 'http://purl.org/coar/resource_type/c_18ww'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_18ww</xsl:text>
               </xsl:attribute>
               <xsl:text>internal report</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'memorandum' or $dc_type = 'http://purl.org/coar/resource_type/c_18wz'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_18wz</xsl:text>
               </xsl:attribute>
               <xsl:text>memorandum</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'other type of report'  or $lc_dc_type = 'othertypeofreport' or $dc_type = 'http://purl.org/coar/resource_type/c_18wq'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_18wq</xsl:text>
               </xsl:attribute>
               <xsl:text>other type of report</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'policy report' or $lc_dc_type = 'policyreport'  or $dc_type = 'http://purl.org/coar/resource_type/c_186u'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_186u</xsl:text>
               </xsl:attribute>
               <xsl:text>policy report</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'project deliverable' or $lc_dc_type = 'projectdeliverable' or $dc_type = 'http://purl.org/coar/resource_type/c_18op'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_18op</xsl:text>
               </xsl:attribute>
               <xsl:text>project deliverable</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'report to funding agency' or $lc_dc_type = 'reporttofundingagency' or $dc_type = 'http://purl.org/coar/resource_type/c_18hj'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_18hj</xsl:text>
               </xsl:attribute>
               <xsl:text>report to funding agency</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'research report' or $lc_dc_type = 'researchreport' or $dc_type = 'http://purl.org/coar/resource_type/c_18ws'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_18ws</xsl:text>
               </xsl:attribute>
               <xsl:text>research report</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'technical report' or $lc_dc_type = 'technicalreport' or $dc_type = 'http://purl.org/coar/resource_type/c_18gh'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_18gh</xsl:text>
               </xsl:attribute>
               <xsl:text>technical report</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'review article' or $lc_dc_type = 'reviewarticle' or $dc_type = 'http://purl.org/coar/resource_type/c_dcae04bc'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_dcae04bc</xsl:text>
               </xsl:attribute>
               <xsl:text>review article</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'research article' or $lc_dc_type = 'researcharticle' or $dc_type = 'http://purl.org/coar/resource_type/c_2df8fbb1'">
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_2df8fbb1</xsl:text>
               </xsl:attribute>
               <xsl:text>research article</xsl:text>
            </xsl:when>
            <xsl:otherwise>
               <xsl:attribute name="uri">
                  <xsl:text>http://purl.org/coar/resource_type/c_1843</xsl:text>
               </xsl:attribute>
			   <xsl:attribute name="resourceTypeGeneral">
                  <xsl:text>other research product</xsl:text>
               </xsl:attribute>			   
               <xsl:text>other</xsl:text>
            </xsl:otherwise>
         </xsl:choose>
      </resourceType>
   </xsl:template>
   
   
   <!-- datacite.rights -->
   <xsl:template match="doc:element[@name='dc']/doc:element[@name='rights']/doc:element" mode="coarrights">
	  
	  <xsl:variable name="coarrights_value">
         <xsl:call-template name="coarrights_value">
            <xsl:with-param name="value" select="doc:field[@name='value']/text()" />
         </xsl:call-template>
      </xsl:variable>
	  
	  <xsl:variable name="coarrights_attribute_uri">
         <xsl:call-template name="coarrights_attribute_uri">
            <xsl:with-param name="value" select="doc:field[@name='value']/text()" />
         </xsl:call-template>
      </xsl:variable>
	  
      <datacite:rights>
		 <xsl:if test="$coarrights_attribute_uri">
			<xsl:attribute name="rightsURI">
				<xsl:value-of select="$coarrights_attribute_uri" />
			</xsl:attribute>
		 </xsl:if>
		 <xsl:value-of select="$coarrights_value" />
      </datacite:rights>
   </xsl:template>
   
    <!-- datacite:rights atribute uri -->
	<xsl:template name="coarrights_attribute_uri">
      <xsl:param name="value"/>
      <xsl:variable name="lc_value">
         <xsl:call-template name="lowercase">
            <xsl:with-param name="value" select="$value" />
         </xsl:call-template>
      </xsl:variable>
		<xsl:choose>
            <xsl:when test="$lc_value = 'open access' or $lc_value = 'openaccess' or $value = 'http://purl.org/coar/access_right/c_abf2'">
               <xsl:text>http://purl.org/coar/access_right/c_abf2</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_value = 'embargoed access' or $lc_value = 'embargoedaccess' or $value = 'http://purl.org/coar/access_right/c_f1cf'">
               <xsl:text>http://purl.org/coar/access_right/c_f1cf</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_value = 'restricted access' or $lc_value = 'restrictedaccess' or $value = 'http://purl.org/coar/access_right/c_16ec'">
               <xsl:text>http://purl.org/coar/access_right/c_16ec</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_value = 'metadata only access' or $lc_value = 'closedaccess' or $value = 'http://purl.org/coar/access_right/c_14cb'">
               <xsl:text>http://purl.org/coar/access_right/c_14cb</xsl:text>
            </xsl:when>
        </xsl:choose>	
	</xsl:template>
	
	<!-- datacite:rights value -->
	<xsl:template name="coarrights_value">
      <xsl:param name="value"/>
      <xsl:variable name="lc_value">
         <xsl:call-template name="lowercase">
            <xsl:with-param name="value" select="$value" />
         </xsl:call-template>
      </xsl:variable>
		<xsl:choose>
            <xsl:when test="$lc_value = 'open access' or $lc_value = 'openaccess' or $value = 'http://purl.org/coar/access_right/c_abf2'">
               <xsl:text>open access</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_value = 'embargoed access' or $lc_value = 'embargoedaccess' or $value = 'http://purl.org/coar/access_right/c_f1cf'">
               <xsl:text>embargoed access</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_value = 'restricted access' or $lc_value = 'restrictedaccess' or $value = 'http://purl.org/coar/access_right/c_16ec'">
               <xsl:text>restricted access</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_value = 'metadata only access' or $lc_value = 'closedaccess' or $value = 'http://purl.org/coar/access_right/c_14cb'">
               <xsl:text>metadata only access</xsl:text>
            </xsl:when>
            <xsl:otherwise>
               <xsl:value-of select="$value" />
            </xsl:otherwise>
        </xsl:choose>	
	</xsl:template>
   
   <xsl:template match="doc:element[@name='dc']/doc:element[@name='identifier']" mode="dc">
		<!-- datacite.identifier -->
         <!-- xsl:for-each select="./doc:element/doc:field[@name='value']">
            <datacite:identifier>
               <xsl:value-of select="." />
            </datacite:identifier>
         </xsl:for-each -->
         <!-- dc.identifier.* -->
		<datacite:identifier>
		<xsl:choose>
			<xsl:when test="./doc:element[@name='uri']/doc:element/doc:field[@name='value' and contains(text(),'hdl.handle.net')]">
				<xsl:attribute name="identifierType">
					<xsl:text>Handle</xsl:text>
				</xsl:attribute>
			   <xsl:value-of select="./doc:element[@name='uri']/doc:element/doc:field[@name='value' and contains(text(),'hdl.handle.net')]" />			
			</xsl:when>
			<xsl:when test="./doc:element[@name='doi']">
				<xsl:attribute name="identifierType">
					<xsl:text>DOI</xsl:text>
				</xsl:attribute>
			   <xsl:value-of select="./doc:element[@name='doi']/doc:element/doc:field[@name='value']" />			
			</xsl:when>
			<xsl:when test="./doc:element[@name='uri']/doc:element/doc:field[@name='value' and not(contains(text(),'hdl.handle.net'))]">
				<xsl:attribute name="identifierType">
					<xsl:text>URL</xsl:text>
				</xsl:attribute>
			   <xsl:value-of select="./doc:element[@name='uri']/doc:element/doc:field[@name='value' and not(contains(text(),'hdl.handle.net'))]" />			
			</xsl:when>
		</xsl:choose>
		</datacite:identifier>
   </xsl:template>
   
   <!-- licenseCondition -->
   <!--xsl:template match="doc:element[@name='dc']/doc:element[@name='rights']/doc:element/doc:element">
      <xsl:variable name="dc_right" select="doc:field[@name='value']/text()" />
      <licenseCondition xmlns="http://namespace.openaire.eu/schema/oaire/">
         <xsl:value-of select="$dc_right" />
      </licenseCondition>
   </xsl:template-->
   <!-- licenseCondition << dc.rights.uri -->
   <xsl:template match="doc:element[@name='dc']/doc:element[@name='rights']/doc:element[@name='uri']/doc:element">
      <xsl:variable name="dc_right_uri" select="doc:field[@name='value']/text()" />
      <licenseCondition xmlns="http://namespace.openaire.eu/schema/oaire/">
         <xsl:choose>
            <xsl:when test="contains($dc_right_uri, 'creativecommons.org')">
			   <xsl:attribute name="startDate">
				    <xsl:call-template name="getIssuedDate">
                        <xsl:with-param name="dc_node" select="../../../." />
                    </xsl:call-template>
			   </xsl:attribute>
               <xsl:attribute name="uri">
                  <xsl:value-of select="$dc_right_uri" />
               </xsl:attribute>
               <xsl:choose>
                  <xsl:when test="contains($dc_right_uri, 'by-nc-sa')">
                     <xsl:text>Creative Commons Attribution-NonCommercial-ShareAlike</xsl:text>
                  </xsl:when>
                  <xsl:when test="contains($dc_right_uri, 'by-nc-nd')">
                     <xsl:text>Creative Commons Attribution-NonCommercial-NoDerivs</xsl:text>
                  </xsl:when>
                  <xsl:when test="contains($dc_right_uri, 'by-nc')">
                     <xsl:text>Creative Commons Attribution-NonCommercial</xsl:text>
                  </xsl:when>
                  <xsl:when test="contains($dc_right_uri, 'by-sa')">
                     <xsl:text>Creative Commons Attribution-ShareAlike</xsl:text>
                  </xsl:when>
                  <xsl:when test="contains($dc_right_uri, 'by-nd')">
                     <xsl:text>Creative Commons Attribution-NoDerivs</xsl:text>
                  </xsl:when>
                  <xsl:when test="contains($dc_right_uri, 'by')">
                     <xsl:text>Creative Commons Attribution</xsl:text>
                  </xsl:when>
                  <xsl:otherwise>
                     <xsl:text />
                  </xsl:otherwise>
               </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
               <xsl:value-of select="$dc_right_uri" />
            </xsl:otherwise>
         </xsl:choose>
      </licenseCondition>
   </xsl:template>
   
   
   <!-- datacite.subject -->
   <xsl:template match="doc:element[@name='dc']/doc:element[@name='subject']" mode="dc">			 
		 <!-- dc.subject -->
		 <xsl:for-each select="./doc:element/doc:field[@name='value']">
			<datacite:subject>
			   <xsl:value-of select="." />
			</datacite:subject>
		 </xsl:for-each>
		 <!-- dc.subject.* (qualified) -->
		 <xsl:for-each select="./doc:element/doc:element/doc:field[@name='value']">
			<datacite:subject>
				<xsl:choose>
                  <xsl:when test="contains(../../@name, 'fos')">
					   <xsl:attribute name="subjectScheme">
						  <xsl:text>FOS</xsl:text>
					   </xsl:attribute>
					   <xsl:attribute name="schemeURI">
						  <xsl:text>FOS</xsl:text>
					   </xsl:attribute>
					   <xsl:attribute name="valueURI">
						  <xsl:text>FOS</xsl:text>
					   </xsl:attribute>
			      </xsl:when>
			   </xsl:choose>
			   <xsl:value-of select="." />
			</datacite:subject>
		 </xsl:for-each>   
   </xsl:template>
   
   <!-- oaire:file -->
   <xsl:template match="doc:element[@name='bundles']/doc:element[@name='bundle']" mode="bundle">
	   <xsl:if test="doc:field[@name='name' and text()='ORIGINAL']">
		   <xsl:for-each select="doc:element[@name='bitstreams']/doc:element[@name='bitstream']">
			  <file xmlns="http://namespace.openaire.eu/schema/oaire/">
				 <xsl:attribute name="accessRightsURI">				 
					<xsl:call-template name="coarrights_attribute_uri">
						<xsl:with-param name="value" select="../../../../doc:element[@name='dc']/doc:element[@name='rights']/doc:element/doc:field[@name='value']/text()" />
					</xsl:call-template>      							
				 </xsl:attribute>
				 <xsl:attribute name="mimeType">
					<xsl:value-of select="doc:field[@name='format']" />
				 </xsl:attribute>
				 <xsl:attribute name="objectType">
					<xsl:choose>
						<xsl:when test="1">
							<xsl:text>fulltext</xsl:text>
						</xsl:when>
						<!--xsl:when test="$type='dataset'">
							<xsl:text>dataset</xsl:text>
						</xsl:when>
						<xsl:when test="$type='software'">
							<xsl:text>software</xsl:text>
						</xsl:when>
						<xsl:when test="$type='article'">
							<xsl:text>fulltext</xsl:text>
						</xsl:when-->
						<xsl:otherwise>					 
							<xsl:text>other</xsl:text>
						</xsl:otherwise>
					 </xsl:choose>
				   </xsl:attribute>
				 <xsl:value-of select="doc:field[@name='url']" />
			  </file>
		   </xsl:for-each>
       </xsl:if>
	</xsl:template>
   
   <!-- degois>>oaire -->
   <xsl:template match="doc:element[@name='degois']" mode="oaire">
      <!-- citationTitle -->
      <xsl:for-each select="doc:element[@name='publication']/doc:element[@name='title']/doc:element/doc:field[@name='value']">
         <citationTitle xmlns="http://namespace.openaire.eu/schema/oaire/">
            <xsl:value-of select="." />
         </citationTitle>
      </xsl:for-each>
      <!-- citationVolume -->
      <xsl:for-each select="doc:element[@name='publication']/doc:element[@name='volume']/doc:element/doc:field[@name='value']">
         <citationVolume xmlns="http://namespace.openaire.eu/schema/oaire/">
            <xsl:value-of select="." />
         </citationVolume>
      </xsl:for-each>
      <!-- citationIssue -->
      <xsl:for-each select="doc:element[@name='publication']/doc:element[@name='issue']/doc:element/doc:field[@name='value']">
         <citationIssue xmlns="http://namespace.openaire.eu/schema/oaire/">
            <xsl:value-of select="." />
         </citationIssue>
      </xsl:for-each>
      <!-- citationStartPage -->
      <xsl:for-each select="doc:element[@name='publication']/doc:element[@name='firstPage']/doc:element/doc:field[@name='value']">
         <citationStartPage xmlns="http://namespace.openaire.eu/schema/oaire/">
            <xsl:value-of select="." />
         </citationStartPage>
      </xsl:for-each>
      <!-- citationEndPage -->
      <xsl:for-each select="doc:element[@name='publication']/doc:element[@name='lastPage']/doc:element/doc:field[@name='value']">
         <citationEndPage xmlns="http://namespace.openaire.eu/schema/oaire/">
            <xsl:value-of select="." />
         </citationEndPage>
      </xsl:for-each>
      <!-- citationConferencePlace -->
      <xsl:for-each select="doc:element[@name='publication']/doc:element[@name='location']/doc:element/doc:field[@name='value']">
         <citationConferencePlace xmlns="http://namespace.openaire.eu/schema/oaire/">
            <xsl:value-of select="." />
         </citationConferencePlace>
      </xsl:for-each>
   </xsl:template>
   
   <!-- oaire -->
   <xsl:template match="doc:element[@name='oaire']" mode="oaire">
      <!-- citationTitle -->
      <xsl:for-each select="doc:element[@name='citationTitle']/doc:element/doc:field[@name='value']">
         <citationTitle xmlns="http://namespace.openaire.eu/schema/oaire/">
            <xsl:value-of select="." />
         </citationTitle>
      </xsl:for-each>
	  <!-- citationEdition -->
      <xsl:for-each select="doc:element[@name='citationEdition']/doc:element/doc:field[@name='value']">
         <citationEdition xmlns="http://namespace.openaire.eu/schema/oaire/">
            <xsl:value-of select="." />
         </citationEdition>
      </xsl:for-each>	  
      <!-- citationVolume -->
      <xsl:for-each select="doc:element[@name='citationVolume']/doc:element/doc:field[@name='value']">
         <citationVolume xmlns="http://namespace.openaire.eu/schema/oaire/">
            <xsl:value-of select="." />
         </citationVolume>
      </xsl:for-each>
      <!-- citationIssue -->
      <xsl:for-each select="doc:element[@name='citationIssue']/doc:element/doc:field[@name='value']">
         <citationIssue xmlns="http://namespace.openaire.eu/schema/oaire/">
            <xsl:value-of select="." />
         </citationIssue>
      </xsl:for-each>
      <!-- citationStartPage -->
      <xsl:for-each select="doc:element[@name='citationStartPage']/doc:element/doc:field[@name='value']">
         <citationStartPage xmlns="http://namespace.openaire.eu/schema/oaire/">
            <xsl:value-of select="." />
         </citationStartPage>
      </xsl:for-each>
      <!-- citationEndPage -->
      <xsl:for-each select="doc:element[@name='citationEndPage']/doc:element/doc:field[@name='value']">
         <citationEndPage xmlns="http://namespace.openaire.eu/schema/oaire/">
            <xsl:value-of select="." />
         </citationEndPage>
      </xsl:for-each>
      <!-- citationConferencePlace -->
      <xsl:for-each select="doc:element[@name='citationConferencePlace']/doc:element/doc:field[@name='value']">
         <citationConferencePlace xmlns="http://namespace.openaire.eu/schema/oaire/">
            <xsl:value-of select="." />
         </citationConferencePlace>
      </xsl:for-each>
      <!-- citationConferenceDate -->
      <xsl:for-each select="doc:element[@name='citationConferenceDate']/doc:element/doc:field[@name='value']">
         <citationConferenceDate xmlns="http://namespace.openaire.eu/schema/oaire/">
            <xsl:value-of select="." />
         </citationConferenceDate>
      </xsl:for-each>
   </xsl:template>   
   
   <!-- datacite:relatedIdentifier |dc-->
   <xsl:template match="doc:element[@name='dc']/doc:element[@name='identifier']/doc:element" mode="datacite">
      <xsl:variable name="dc_identifier_type" select="@name" />
      <xsl:variable name="dc_identifier_value" select="doc:element/doc:field[@name='value']/text()" />   
		<!-- Don't consider Handles as related identifiers -->
		<xsl:if test="not(contains($dc_identifier_value, 'hdl.handle.net'))"> 
			<xsl:call-template name="relatedIdentifier">
				<xsl:with-param name="identifier_type" select="$dc_identifier_type" />
				<xsl:with-param name="identifier_value" select="$dc_identifier_value" />
			</xsl:call-template>		
		</xsl:if>
   </xsl:template>
      
   <!-- datacite:relatedIdentifier |datacite-->
   <xsl:template match="doc:element[@name='datacite']/doc:element[@name='relatedIdentifier']/doc:element" mode="datacite">
      <xsl:if test="./doc:field[@name='value']">
		  <xsl:call-template name="relatedIdentifier">
			<xsl:with-param name="identifier_type" select="URN" />
			<xsl:with-param name="identifier_value" select="doc:field[@name='value']/text()" />
		  </xsl:call-template>
	  </xsl:if>   
      <xsl:if test="not(./doc:field[@name='value'])">
		  <xsl:variable name="openaire_identifier_type" select="@name" />
		  <xsl:variable name="openaire_identifier_value" select="doc:element/doc:field[@name='value']/text()" />
		  <xsl:call-template name="relatedIdentifier">
			<xsl:with-param name="identifier_type" select="$openaire_identifier_type" />
			<xsl:with-param name="identifier_value" select="$openaire_identifier_value" />
		  </xsl:call-template>
	  </xsl:if>
   </xsl:template>
   
   <!-- datacite:relatedIdentifier -->
   <xsl:template name="relatedIdentifier">
      <xsl:param name="identifier_type" />
      <xsl:param name="identifier_value" />
      <xsl:variable name="lc_identifier_type">
         <xsl:call-template name="lowercase">
            <xsl:with-param name="value" select="$identifier_type" />
         </xsl:call-template>
      </xsl:variable>
      <xsl:variable name="lc_identifier_value">
         <xsl:call-template name="lowercase">
            <xsl:with-param name="value" select="$identifier_value" />
         </xsl:call-template>
      </xsl:variable>	  
	  
		  <datacite:relatedIdentifier>
			 <xsl:attribute name="relatedIdentifierType">
				 <xsl:choose>
					<!-- relationType="Continues" relatedMetadataScheme="" schemeURI="" schemeType="" -->
					<xsl:when test="$lc_identifier_type = 'ark'">				   
						<xsl:text>ARK</xsl:text>				   				   
					</xsl:when>
					<xsl:when test="$lc_identifier_type = 'arxiv'">
						  <xsl:text>arXiv</xsl:text>
					</xsl:when>
					<xsl:when test="$lc_identifier_type = 'bibcode'">
						<xsl:text>bibcode</xsl:text>
					</xsl:when>
					<xsl:when test="contains($lc_identifier_value, 'doi.org') or $lc_identifier_value = 'doi:10.' or $lc_identifier_type = 'doi'">
						<xsl:text>DOI</xsl:text>
					</xsl:when>
					<xsl:when test="$lc_identifier_type = 'ean13'">
						<xsl:text>EAN13</xsl:text>
					</xsl:when>
					<xsl:when test="$lc_identifier_type = 'eissn'">
						<xsl:text>EISSN</xsl:text>
					</xsl:when>
					<xsl:when test="contains($lc_identifier_value, 'hdl.handle.net') or $lc_identifier_type = 'handle'">
						<xsl:text>Handle</xsl:text>
					</xsl:when>
					<xsl:when test="$lc_identifier_type = 'igsn'">
						<xsl:text>IGSN</xsl:text>
					</xsl:when>
					<xsl:when test="$lc_identifier_type = 'isbn'">
						<xsl:text>ISBN</xsl:text>
					</xsl:when>
					<xsl:when test="$lc_identifier_type = 'issn'">
						<xsl:text>ISSN</xsl:text>
					</xsl:when>
					<xsl:when test="$lc_identifier_type = 'istc'">
						<xsl:text>ISTC</xsl:text>
					</xsl:when>
					<xsl:when test="$lc_identifier_type = 'lissn'">
						<xsl:text>LISSN</xsl:text>
					</xsl:when>
					<xsl:when test="$lc_identifier_type = 'lsid'">
						<xsl:text>LSID</xsl:text>
					</xsl:when>
					<xsl:when test="$lc_identifier_type = 'pmid'">
						<xsl:text>PMID</xsl:text>
					</xsl:when>
					<xsl:when test="$lc_identifier_type = 'purl'">
						<xsl:text>PURL</xsl:text>
					</xsl:when>
					<xsl:when test="$lc_identifier_type = 'upc'">
						<xsl:text>UPC</xsl:text>
					</xsl:when>
					<xsl:when test="$lc_identifier_type = 'url' or contains($lc_identifier_value, 'http://') or contains($lc_identifier_value, 'https://')">
						<xsl:text>URL</xsl:text>
					</xsl:when>
					<xsl:otherwise>					 
						<xsl:text>URN</xsl:text>
					</xsl:otherwise>					
				 </xsl:choose>
			 </xsl:attribute>
			 <xsl:value-of select="$identifier_value" />
		  </datacite:relatedIdentifier>	  
   </xsl:template>
   
   <!-- relatedIdentifier < dc.relation.publisherversion -->
   <xsl:template match="doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='publisherversion']/doc:element" mode="datacite">
      <xsl:variable name="publisherversion" select="doc:element/doc:field[@name='value']/text()" />
	  <xsl:call-template name="generic_publisherversion">
            <xsl:with-param name="publisherversion" select="$publisherversion" />
      </xsl:call-template>	  
   </xsl:template>
   
   <!-- relatedIdentifier < rcaap.publisherversion -->   
   <xsl:template match="doc:element[@name='rcaap']/doc:element[@name='publisherversion']/doc:element" mode="datacite">
      <xsl:variable name="publisherversion" select="doc:element/doc:field[@name='value']/text()" />
	  <xsl:call-template name="generic_publisherversion">
            <xsl:with-param name="publisherversion" select="$publisherversion" />
      </xsl:call-template>
   </xsl:template>      
   
   <xsl:template name="generic_publisherversion">
      <xsl:param name="publisherversion"/>
	  <xsl:variable name="lc_publisherversion">
         <xsl:call-template name="lowercase">
            <xsl:with-param name="value" select="$publisherversion" />
         </xsl:call-template>
      </xsl:variable>	  
      <datacite:relatedIdentifier>   
			<xsl:attribute name="relatedIdentifierType">
				 <xsl:choose>
					<xsl:when test="contains($lc_publisherversion, 'doi.org') or contains($lc_publisherversion, 'doi:10.')">
						<xsl:text>DOI</xsl:text>				 
					</xsl:when>					
					<xsl:when test="contains($lc_publisherversion, 'http://') or contains($lc_publisherversion, 'https://')">
						<xsl:text>URL</xsl:text>				 
					</xsl:when>					
				 </xsl:choose>
			</xsl:attribute>
			<xsl:attribute name="relationType"> 
				<xsl:text>IsVersionOf</xsl:text>
			</xsl:attribute>
		  <xsl:value-of select="$publisherversion" />
      </datacite:relatedIdentifier>
   </xsl:template>    
   
   <!-- datacite:alternateIdentifier -->
   <xsl:template match="doc:element[@name='datacite']/doc:element[@name='alternateIdentifier']/doc:element[@name='tid' or @name='TID']" mode="datacite">
      <datacite:alternateIdentifier>
         <xsl:attribute name="alternateIdentifierType">
            <xsl:value-of select="@name" />
         </xsl:attribute>
         <xsl:value-of select="doc:element/doc:field[@name='value']/text()" />
      </datacite:alternateIdentifier>
   </xsl:template>
   
   <!-- datacite:alternateIdentifier dc.identifier.tid -->
   <xsl:template match="doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='tid' or @name='TID']" mode="datacite">
      <datacite:alternateIdentifier>
         <xsl:attribute name="alternateIdentifierType">
            <xsl:value-of select="@name" />
         </xsl:attribute>
         <xsl:value-of select="doc:element/doc:field[@name='value']/text()" />
      </datacite:alternateIdentifier>
   </xsl:template>

   
   <!-- openaire.version << dc.description.version -->
   <xsl:template match="doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='version']/doc:element" mode="openaire">
      <xsl:variable name="version" select="doc:field[@name='value']/text()" />
	  <xsl:call-template name="generic_version">
            <xsl:with-param name="version" select="$version" />
      </xsl:call-template>
   </xsl:template>
   <!-- openaire.version -->
   <xsl:template match="doc:element[@name='oaire']/doc:element[@name='version']/doc:element" mode="openaire">
      <xsl:variable name="version" select="doc:field[@name='value']/text()" />
	  <xsl:call-template name="generic_version">
            <xsl:with-param name="version" select="$version" />
      </xsl:call-template>
   </xsl:template>
   
   <!-- version -->
   <xsl:template name="generic_version">
      <xsl:param name="version" />
      <xsl:variable name="eu_prefix" select="'http://purl.org/coar/version/'" />
      <xsl:variable name="lc_version">
         <xsl:call-template name="lowercase">
            <xsl:with-param name="value" select="$version" />
         </xsl:call-template>
      </xsl:variable>
      <version xmlns="http://namespace.openaire.eu/schema/oaire/">		 
		 <xsl:attribute name="uri">
			 <xsl:choose>
				<xsl:when test="$lc_version = concat($eu_prefix,'acceptedversion') or $version = 'http://purl.org/coar/version/c_ab4af688f83e57aa'  or $lc_version = 'am'">
				   <xsl:text>http://purl.org/coar/version/c_ab4af688f83e57aa</xsl:text><!--AM-->
				</xsl:when>
				<xsl:when test="$lc_version = concat($eu_prefix,'publishedversion') or $lc_version = 'published'  or $lc_version = 'vor'">
				   <xsl:text>http://purl.org/coar/version/c_970fb48d4fbd8a85</xsl:text><!--VoR-->
				</xsl:when>
				<xsl:when test="$lc_version = concat($eu_prefix,'draftversion') or $lc_version = 'draft' or $lc_version = 'ao'">
				   <xsl:text>http://purl.org/coar/version/c_b1a7d7d4d402bcce</xsl:text><!--AO-->
				</xsl:when>
				<xsl:when test="$lc_version = concat($eu_prefix,'submittedversion') or $version = 'http://purl.org/coar/version/c_71e4c1898caa6e32'  or $lc_version = 'smur'">
				   <xsl:text>http://purl.org/coar/version/c_71e4c1898caa6e32</xsl:text><!--SMUR-->
				</xsl:when>
				<xsl:when test="$lc_version = concat($eu_prefix,'updatedversion') or $lc_version = 'updated'  or $lc_version = 'cvor'">
				   <xsl:text>http://purl.org/coar/version/c_e19f295774971610</xsl:text><!--CVoR-->
				</xsl:when>
				<xsl:otherwise>
				   <xsl:text>http://purl.org/coar/version/c_be7fb7dd8ff6fe43</xsl:text>
				   <!--NA (Not Applicable (or Unknown))-->
				</xsl:otherwise>
			 </xsl:choose>
		 </xsl:attribute>
		 <xsl:value-of select="$version" />
      </version>
   </xsl:template>   
   
   <!-- xml:language -->
   <xsl:template name="xmlLanguage">
      <xsl:param name="name" />
      <xsl:variable name="lc_name">
         <xsl:call-template name="lowercase">
            <xsl:with-param name="value" select="$name" />
         </xsl:call-template>
      </xsl:variable>
      <xsl:if test="$lc_name!='none' and $name!=''">
         <xsl:attribute name="xml:lang">
            <xsl:value-of select="$name" />
         </xsl:attribute>
      </xsl:if>
   </xsl:template>

   <!-- ------------------- -->
   <!-- Auxiliary templates -->
   <!-- ------------------- -->
   <xsl:param name="smallcase" select="'abcdefghijklmnopqrstuvwxyzàèìòùáéíóúýâêîôûãñõäëïöüÿåæœçðø'" />
   <xsl:param name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZÀÈÌÒÙÁÉÍÓÚÝÂÊÎÔÛÃÑÕÄËÏÖÜŸÅÆŒÇÐØ'" />
   
   <!-- to retrieve a string in uppercase -->
   <xsl:template name="uppercase">
      <xsl:param name="value" />
      <xsl:value-of select="translate($value, $smallcase, $uppercase)" />
   </xsl:template>
   
   <!-- to retrieve a string in lowercase -->
   <xsl:template name="lowercase">
      <xsl:param name="value" />
      <xsl:value-of select="translate($value, $uppercase, $smallcase)" />
   </xsl:template>
   
   <!-- to retrieve a string which the first letter is in uppercase -->
   <xsl:template name="ucfirst">
      <xsl:param name="value" />
      <xsl:call-template name="uppercase">
         <xsl:with-param name="value" select="substring($value, 1, 1)" />
      </xsl:call-template>
      <xsl:call-template name="lowercase">
         <xsl:with-param name="value" select="substring($value, 2)" />
      </xsl:call-template>
   </xsl:template>
   
   <!-- get the issued date -->
   <xsl:template name="getIssuedDate">
	  <xsl:param name="dc_node" />
	  <xsl:value-of select="$dc_node/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']/text()" />	  
   </xsl:template>   

</xsl:stylesheet>