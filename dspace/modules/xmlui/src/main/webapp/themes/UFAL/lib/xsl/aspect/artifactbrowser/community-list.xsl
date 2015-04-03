<!--
	/* Created for LINDAT/CLARIN */
	Rendering of a list of communities (e.g. on a community homepage, or on the community-list page)
	Author: Amir Kamran
-->

<xsl:stylesheet version="1.0"
	xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/"
	xmlns:mets="http://www.loc.gov/METS/"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xlink="http://www.w3.org/TR/xlink/"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:atom="http://www.w3.org/2005/Atom"
	xmlns:ore="http://www.openarchives.org/ore/terms/"
	xmlns:oreatom="http://www.openarchives.org/ore/atom/"
	xmlns="http://www.w3.org/1999/xhtml"
	xmlns:xalan="http://xml.apache.org/xalan"
	xmlns:encoder="xalan://java.net.URLEncoder"
	xmlns:util="org.dspace.app.xmlui.utils.XSLUtils"
	xmlns:confman="org.dspace.core.ConfigurationManager"
	exclude-result-prefixes="xalan encoder i18n dri mets dim xlink xsl util confman">

	<xsl:output indent="yes" />


	<xsl:template match="dri:div[@n='comunity-browser']" priority="1">
		<div class="well well-light well-small">
			<xsl:apply-templates select="dri:head" />
			<xsl:call-template name="standardAttributes" />
			<xsl:choose>
				<xsl:when test="child::node()">
					<xsl:apply-templates select="*[not(name()='head')]" />
				</xsl:when>
				<xsl:otherwise>
					&#160;
				</xsl:otherwise>
			</xsl:choose>
		</div>
	</xsl:template>

	<xsl:template match="dri:referenceSet[@n = 'community-browser']">
		<xsl:apply-templates select="dri:head" />
		<ul class="unstyled no-margin">
			<xsl:apply-templates select="*[not(name()='head')]" mode="communityList" />
		</ul>
	</xsl:template>

	<xsl:template match="dri:reference" mode="communityList">
		<xsl:variable name="externalMetadataURL">
			<xsl:text>cocoon:/</xsl:text>
			<xsl:value-of select="@url" />
			<xsl:text>?sections=dmdSec,fileSec&amp;fileGrpTypes=THUMBNAIL</xsl:text>
		</xsl:variable>
		<xsl:comment>External Metadata URL: <xsl:value-of select="$externalMetadataURL" /></xsl:comment>
		<li class="item-box" style="min-height: 100px;">
			<xsl:apply-templates select="document($externalMetadataURL)" mode="summaryList" />
			<div class="artifact-abstract-head">Collections in this community:</div>
			<div class="artifact-abstract">
				<xsl:choose>
					<xsl:when test="dri:referenceSet/dri:reference[@type='DSpace Collection']">
						<ul>			
							<xsl:apply-templates select="dri:referenceSet/dri:reference[@type='DSpace Collection']" mode="shortSummary" />
						</ul>
					</xsl:when>
					<xsl:otherwise>
						<div class="text-error" style="margin-left: 10px;">No collections in this community.</div>
					</xsl:otherwise>
				</xsl:choose>
			</div>
			<div class="artifact-abstract-head">Sub-Communities:</div>
			<div class="artifact-abstract">
				<xsl:choose>
					<xsl:when test="dri:referenceSet/dri:reference[@type='DSpace Community']">
						<ul>			
							<xsl:apply-templates select="dri:referenceSet/dri:reference[@type='DSpace Community']" mode="shortSummary" />
						</ul>
					</xsl:when>
					<xsl:otherwise>
						<div class="text-error" style="margin-left: 10px;">No sub-communities in this community.</div>
					</xsl:otherwise>
				</xsl:choose>
			</div>			
		</li>
	</xsl:template>

	<xsl:template match="dri:reference" mode="shortSummary">
		<li>
			<xsl:variable name="externalMetadataURL">
				<xsl:text>cocoon:/</xsl:text>
				<xsl:value-of select="@url" />
				<xsl:text>?sections=dmdSec,fileSec&amp;fileGrpTypes=THUMBNAIL</xsl:text>
			</xsl:variable>
			<xsl:comment>External Metadata URL: <xsl:value-of select="$externalMetadataURL" /></xsl:comment>
			<xsl:apply-templates select="document($externalMetadataURL)" mode="subList" />
		</li>		
	</xsl:template>
	
	<xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']]" mode="subList">
		<xsl:variable name="data" select="./mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim" />
		<div class="artifact-title" style="font-size: 95%;">
			<a href="{@OBJID}">
				<span class="Z3988">
					<xsl:choose>
						<xsl:when
							test="string-length($data/dim:field[@element='title'][1]) &gt; 0">
							<xsl:value-of select="$data/dim:field[@element='title'][1]" />
						</xsl:when>
						<xsl:otherwise>
							<i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
						</xsl:otherwise>
					</xsl:choose>
				</span>
			</a>
			<!--Display community strengths (item counts) if they exist -->
			<xsl:if
				test="string-length($data/dim:field[@element='format'][@qualifier='extent'][1]) &gt; 0">
				<xsl:text> [</xsl:text>
				<xsl:value-of
					select="$data/dim:field[@element='format'][@qualifier='extent'][1]" />
				<xsl:text>]</xsl:text>
			</xsl:if>
		</div>
		<xsl:variable name="abstract"
			select="$data/dim:field[@element = 'description' and @qualifier='abstract']/node()" />
		<xsl:if test="$abstract and string-length($abstract[1]) &gt; 0">
			<div class="artifact-abstract" style="font-size: 95%; margin-left: 10px;">
				<xsl:value-of select="$abstract" />
			</div>
		</xsl:if>		
	</xsl:template>    
	    

	<!-- A community rendered in the summaryList pattern. Encountered on the 
		community-list and on on the front page. -->
	<xsl:template name="communitySummaryList-DIM">
		<xsl:variable name="data" select="./mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim" />
		<xsl:variable name="logo" select="./mets:fileSec/mets:fileGrp[@USE='LOGO']/mets:file/mets:FLocat/@xlink:href" />		
		<div class="item-type">Community</div>
		<xsl:if test="$logo">
			<img class="artifact-icon pull-right img-rounded">
				<xsl:attribute name="src">
					<xsl:value-of select="$logo" />
				</xsl:attribute>
			</img>
		</xsl:if>
		<div class="artifact-title">
			<a href="{@OBJID}">
				<span class="Z3988">
					<xsl:choose>
						<xsl:when
							test="string-length($data/dim:field[@element='title'][1]) &gt; 0">
							<xsl:value-of select="$data/dim:field[@element='title'][1]" />
						</xsl:when>
						<xsl:otherwise>
							<i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
						</xsl:otherwise>
					</xsl:choose>
				</span>
			</a>
			<!--Display community strengths (item counts) if they exist -->
			<xsl:if
				test="string-length($data/dim:field[@element='format'][@qualifier='extent'][1]) &gt; 0">
				<xsl:text> [</xsl:text>
				<xsl:value-of
					select="$data/dim:field[@element='format'][@qualifier='extent'][1]" />
				<xsl:text>]</xsl:text>
			</xsl:if>
		</div>
		<xsl:variable name="abstract"
			select="$data/dim:field[@element = 'description' and @qualifier='abstract']/node()" />
		<xsl:if test="$abstract and string-length($abstract[1]) &gt; 0">
			<div class="artifact-abstract-head">Description:</div>
			<div class="artifact-abstract">
				<xsl:value-of select="$abstract" />
			</div>
		</xsl:if>
		
		<xsl:if test="string-length($data/dim:field[@element='format'][@qualifier='extent'][1]) &gt; 0">
			<div class="label label-info" style="margin-bottom: 20px;">
				<xsl:text>This community contains </xsl:text>
				<xsl:value-of select="$data/dim:field[@element='format'][@qualifier='extent'][1]" />
				<xsl:text> item(s).</xsl:text>
			</div>
		</xsl:if>
		
	</xsl:template>

</xsl:stylesheet>