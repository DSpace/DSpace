<!-- 
		This is a recursive template. This builds a hierarchical "n" levels list, 
		being "n" a configurable number. This .xsl file focuses on the generation of
		an arbitrary structure expected by the widget 'ACCORDION' of the JQUERY-UI library.
-->
<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:mets="http://www.loc.gov/METS/"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim" xmlns:xlink="http://www.w3.org/TR/xlink/"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:atom="http://www.w3.org/2005/Atom" xmlns:ore="http://www.openarchives.org/ore/terms/"
	xmlns:oreatom="http://www.openarchives.org/ore/atom/" xmlns="http://www.w3.org/1999/xhtml"
	xmlns:xalan="http://xml.apache.org/xalan" xmlns:encoder="xalan://java.net.URLEncoder"
	xmlns:util="org.dspace.app.xmlui.utils.XSLUtils" xmlns:confman="org.dspace.core.ConfigurationManager"
	exclude-result-prefixes="xalan encoder i18n dri mets dim xlink xsl util confman">

	<xsl:output indent="yes" />

	<!-- $autoarchiveId is the ID of the Autoarchive collection, enclosed by the '|' character.-->
	<xsl:variable name="autoarchiveId">
		<xsl:value-of
			select="concat('|',substring-after(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='autoArchive'][@qualifier='handle'], '/'), '|')"></xsl:value-of>
	</xsl:variable>

	<!-- "topCommunitiesId" mantains the handle ID of all the topCommunities, 
		separated by the '|' character. -->
	<xsl:variable name="topCommunitiesId">
		<xsl:text>|</xsl:text>
		<xsl:for-each
			select="/dri:document/dri:body/dri:div/dri:referenceSet[@n='community-browser']/dri:reference">
			<xsl:value-of
				select="concat(substring-after(substring-before(substring-after(@url,'/metadata/handle/'),'/mets.xml'), '/'), '|')">
			</xsl:value-of>
		</xsl:for-each>
	</xsl:variable>
	<!-- Change next varible's value to set	the deep of the hierarchy -->
	<xsl:variable name="maxHierarchyLevel">
		<xsl:value-of select="number('2')" />
	</xsl:variable>
	
	<!-- Begin of the hierarchy structure generation.-->
	<xsl:template match="dri:referenceSet[@n='community-browser']">
		<div id="accordion">
			<xsl:apply-templates select="." mode="initial"/>
		</div>
	</xsl:template>
	
	<!-- This template will iterate over each Top Community, located under the main referenceSet tag. -->
	<xsl:template match="dri:referenceSet[@n='community-browser']/dri:reference" mode="initial">
		<xsl:param name="currentLevel"><xsl:value-of select="number('1')"/></xsl:param>
		<xsl:if test="not(contains(@url, '27909'))">
		<xsl:variable name="externalMetadataURL">
			<xsl:text>cocoon:/</xsl:text>
			<xsl:value-of select="@url" />
			<!-- Since this is a summary only grab the descriptive metadata, and 
				the thumbnails -->
			<xsl:text>?sections=dmdSec,fileSec&amp;fileGrpTypes=THUMBNAIL</xsl:text>
		</xsl:variable>
		<xsl:apply-templates select="document($externalMetadataURL)" mode="accordionHeader"/>
			<div>
<!-- 				<xsl:apply-templates select="document($externalMetadataURL)" mode="communityLink"/> -->
				<xsl:if test="$currentLevel &lt; $maxHierarchyLevel">
				<ul>
					<xsl:apply-templates select="dri:referenceSet/dri:reference" mode="flatMode">
						<xsl:with-param name="currentLevel">
							<xsl:value-of select="$currentLevel" />
						</xsl:with-param>
					</xsl:apply-templates>
				</ul>	
				</xsl:if>
				<xsl:text> </xsl:text>
			</div>
		</xsl:if>
	</xsl:template>
	
	<!-- This template will be called every time a new level of the hierarchy is reached. -->
	<xsl:template match="dri:referenceSet" mode="render-flat-communities">
		<xsl:param name="currentLevel"></xsl:param>
		<xsl:if test="$currentLevel &lt; $maxHierarchyLevel">
			<ul>
				<xsl:apply-templates mode="flatMode">
					<xsl:with-param name="currentLevel">
						<xsl:value-of select="$currentLevel" />
					</xsl:with-param>
				</xsl:apply-templates>
			</ul>
		</xsl:if>	
	</xsl:template>

	<xsl:template match="dri:reference" mode="flatMode">
		<xsl:param name="currentLevel" />
		<!-- $communityId is the ID of the current community, enclosed by '|' character. -->
		<xsl:variable name="communityId"
			select="concat('|',substring-after(substring-before(substring-after(@url,'/metadata/handle/'),'/mets.xml'),'/'), '|')" />
		<xsl:if test="not($communityId = $autoarchiveId)">
			<xsl:variable name="externalMetadataURL">
				<xsl:text>cocoon:/</xsl:text>
				<xsl:value-of select="@url" />
				<!-- Since this is a summary only grab the descriptive metadata, and 
					the thumbnails -->
				<xsl:text>?sections=dmdSec,fileSec&amp;fileGrpTypes=THUMBNAIL</xsl:text>
			</xsl:variable>
			<xsl:choose>
				<xsl:when test="not(contains($topCommunitiesId,$communityId))">
					<li>
						<xsl:call-template name="classStandardAttributes"/>
						<xsl:apply-templates select="document($externalMetadataURL)" mode="summaryList" />
						<xsl:apply-templates select="dri:referenceSet" mode="render-flat-communities">
							<xsl:with-param name="currentLevel">
								<xsl:value-of select="number($currentLevel + 1)" />
							</xsl:with-param>
						</xsl:apply-templates>
					</li>
				</xsl:when>
			</xsl:choose>
		</xsl:if>
	</xsl:template>

	<xsl:template match="mets:METS" mode="accordionHeader">
		<xsl:variable name="data" select="./mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim"/>
		<h3>
			<a class="communityAccessLink topCommunity" href="{@OBJID}">
                 <xsl:choose>
                     <xsl:when test="string-length($data/dim:field[@element='title'][1]) &gt; 0">
                         <xsl:value-of select="$data/dim:field[@element='title'][1]"/>
                     </xsl:when>
                     <xsl:otherwise>
                         <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                     </xsl:otherwise>
                 </xsl:choose>
			</a>
<!--             <span id="{@OBJID}" class="topCommunity"> -->
<!--                  <xsl:choose> -->
<!--                      <xsl:when test="string-length($data/dim:field[@element='title'][1]) &gt; 0"> -->
<!--                          <xsl:value-of select="$data/dim:field[@element='title'][1]"/> -->
<!--                      </xsl:when> -->
<!--                      <xsl:otherwise> -->
<!--                          <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text> -->
<!--                      </xsl:otherwise> -->
<!--                  </xsl:choose> -->
<!--              </span> -->
		</h3>
	</xsl:template>
	
<!-- 	<xsl:template match="mets:METS" mode="communityLink"> -->
<!-- 		<a class="communityAccessLink" href="{@OBJID}">Acceder</a> -->
<!-- 	</xsl:template> -->
		
	<xsl:template name="classStandardAttributes">
		<xsl:attribute name="class">
			<xsl:text>ds-artifact-item </xsl:text>
			<xsl:choose>
				<xsl:when test="contains(@type, 'Community')">
					<xsl:text>community </xsl:text>
				</xsl:when>
				<xsl:when test="contains(@type, 'Collection')">
					<xsl:text>collection </xsl:text>
				</xsl:when>
			</xsl:choose>
			<xsl:choose>
				<xsl:when test="position() mod 2 = 0">even</xsl:when>
				<xsl:otherwise>odd</xsl:otherwise>
			</xsl:choose>
		</xsl:attribute>
	</xsl:template>
</xsl:stylesheet>