<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet
	xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/"
	xmlns:mets="http://www.loc.gov/METS/"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:xlink="http://www.w3.org/TR/xlink/"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:output indent="yes"/>


<!-- ########## MISSING FORM LABELS ########## -->


<!-- text fields -->

<xsl:template match="dri:p[@n='search-query' or dri:field[@n='starts_with']]">
		
	<xsl:variable name="field_id" select="translate(dri:field/@id, '.', '_')" />
	
	<p>
		<label for="{$field_id}">
			<xsl:apply-templates select="i18n:text" />
		</label>
		<xsl:apply-templates select="dri:field" />
	</p>
	    
</xsl:template>

<!-- home page search is different -->

<xsl:template match="dri:p[following-sibling::dri:p/dri:field[@n='query']]">
		
	<xsl:variable name="field_id" select="translate(following-sibling::dri:p/dri:field[@n='query']/@id, '.', '_')" />
	
	<p>
		<label for="{$field_id}">
			<i18n:text catalogue="{@catalogue}"><xsl:value-of select="text()" /></i18n:text>
		</label>
	</p>
	    
</xsl:template>

<!-- pull-down menu items -->

<xsl:template match="i18n:text[following-sibling::dri:field[@type='select']]">
	
	<xsl:variable name="field_id" select="translate(following-sibling::dri:field[@type='select']/@id, '.', '_')" />
	
	<label for="{$field_id}">
		<i18n:text catalogue="{@catalogue}"><xsl:value-of select="text()" /></i18n:text>
	</label>
	    
</xsl:template>


<!-- ########## INCORRECTLY ORDERED HEADINGS ########## -->


<!-- h1's on the homepage and login (other than the first one) need to be switched to h2, since there should not 
     be multiple h1's; no real way to do this, I think, except to call them out explicitly by id -->

<xsl:template match="dri:div/dri:head" priority="3">
		
	<xsl:variable name="ancestor_count" select="count(ancestor::dri:div)" />
		
	<xsl:variable name="head_count">
		<xsl:choose>
			<xsl:when test="../@id = 'aspect.artifactbrowser.FrontPageSearch.div.front-page-search' or 
				(//dri:body/dri:div[@n='front-page-search'] and ../@id = 'aspect.artifactbrowser.CommunityBrowser.div.comunity-browser')">
				<xsl:text>2</xsl:text>
			</xsl:when>
			<xsl:when test="$ancestor_count = 1 or $ancestor_count = 2">
				<xsl:value-of select="$ancestor_count" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$ancestor_count" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	
	<xsl:element name="h{$head_count}">
		<xsl:call-template name="standardAttributes">
			<xsl:with-param name="class">ds-div-head</xsl:with-param>
		</xsl:call-template>            
		<xsl:apply-templates />
	</xsl:element>
</xsl:template>

<!-- had to localize this entire template just to catch the <h3> and switch it to <h2> and remove the fieldset without the label -->

<xsl:template match="dri:options">
	<div id="ds-options">
		<h2 id="ds-search-option-head" class="ds-option-set-head">
			<label for="ada-queryField"><i18n:text>xmlui.dri2xhtml.structural.search</i18n:text></label>
		</h2>
		<div id="ds-search-option" class="ds-option-set">

			<form id="ds-search-form" method="post">
				<xsl:attribute name="action">
					<xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='simpleURL']"/>
				</xsl:attribute>
				
				<input class="ds-text-field " type="text" id="ada-queryField">
					<xsl:attribute name="name">
						<xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='queryField']"/>
					</xsl:attribute>                        
				</input>
				
				<input class="ds-button-field " name="submit" type="submit" i18n:attr="value" value="xmlui.general.go" >
					<xsl:attribute name="onclick">
						<xsl:text>
							var radio = document.getElementById(&quot;ds-search-form-scope-container&quot;);
							if (radio != undefined &amp;&amp; radio.checked)
							{
							var form = document.getElementById(&quot;ds-search-form&quot;);
							form.action=
						</xsl:text>
						<xsl:text>&quot;</xsl:text>
						<xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath']"/>
						<xsl:text>/handle/&quot; + radio.value + &quot;/search&quot; ; </xsl:text>
						<xsl:text>
							} 
						</xsl:text>
					</xsl:attribute>
				</input>
				
				<xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='focus'][@qualifier='container']">
					<br />
					<label>
						<input id="ds-search-form-scope-all" type="radio" name="scope" value="" checked="checked"/>
						<i18n:text>xmlui.dri2xhtml.structural.search</i18n:text>
					</label>
					<br/>
					<label>
						<input id="ds-search-form-scope-container" type="radio" name="scope">
							<xsl:attribute name="value">
								<xsl:value-of select="substring-after(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='focus'][@qualifier='container'],':')"/>
							</xsl:attribute>       
						</input> 
						<xsl:choose>
							<xsl:when test="/dri:document/dri:body//dri:div/dri:referenceSet[@type='detailView' and @n='community-view']">
								<i18n:text>xmlui.dri2xhtml.structural.search-in-community</i18n:text>
							</xsl:when>   
							<xsl:otherwise>
								<i18n:text>xmlui.dri2xhtml.structural.search-in-collection</i18n:text>
							</xsl:otherwise>
												  
						</xsl:choose>
					</label>
				</xsl:if>

			</form>
			
			<a>
				<xsl:attribute name="href">
					<xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='advancedURL']"/>
				</xsl:attribute>
				<i18n:text>xmlui.dri2xhtml.structural.search-advanced</i18n:text>
			</a>
		</div>            
		
		<xsl:apply-templates />
		
	</div>
</xsl:template>

<!-- for all of these templates below, we shifted the <h?> value up one: h3 = h2 and h4 = h3  -->

<xsl:template match="dri:table/dri:head" priority="2">
	<h2>
		<xsl:call-template name="standardAttributes">
			<xsl:with-param name="class">ds-table-head</xsl:with-param>
		</xsl:call-template>
		<xsl:apply-templates />
	</h2>
</xsl:template>

<xsl:template match="dri:list/dri:head" priority="2" mode="nested">
	<h2>
		<xsl:call-template name="standardAttributes">
			<xsl:with-param name="class">ds-list-head</xsl:with-param>
		</xsl:call-template>
		<xsl:apply-templates />
	</h2>
</xsl:template>

<xsl:template match="dri:list/dri:list/dri:head" priority="3" mode="nested">
	<h3>
		<xsl:call-template name="standardAttributes">
			<xsl:with-param name="class">ds-sublist-head</xsl:with-param>
		</xsl:call-template>
		<xsl:apply-templates />
	</h3>
</xsl:template>

<xsl:template match="dri:referenceSet/dri:head" priority="2">
	<h2>
		<xsl:call-template name="standardAttributes">
			<xsl:with-param name="class">ds-list-head</xsl:with-param>
		</xsl:call-template>
		<xsl:apply-templates />
	</h2>
</xsl:template>
	
<xsl:template match="dri:head" priority="1">
	<h2>
		<xsl:call-template name="standardAttributes">
			<xsl:with-param name="class">ds-head</xsl:with-param>
		</xsl:call-template>
		<xsl:apply-templates />
	</h2>
</xsl:template>


<!-- FROM DIM-HANDLER.XSL -->

<!-- changed h3 to h2 -->

<xsl:template match="dim:dim" mode="communityDetailView-DIM"> 
	<xsl:if test="string-length(dim:field[@element='description'][not(@qualifier)])&gt;0">
		<p class="intro-text">
			<xsl:copy-of select="dim:field[@element='description'][not(@qualifier)]/node()"/>
		</p>
	</xsl:if>
	
	<xsl:if test="string-length(dim:field[@element='description'][@qualifier='tableofcontents'])&gt;0">
		<div class="detail-view-news">
			<h2><i18n:text>xmlui.dri2xhtml.METS-1.0.news</i18n:text></h2>
			<p class="news-text">
				<xsl:copy-of select="dim:field[@element='description'][@qualifier='tableofcontents']/node()"/>
			</p>
		</div>
	</xsl:if>
	
	<xsl:if test="string-length(dim:field[@element='rights'][not(@qualifier)])&gt;0">
		<div class="detail-view-rights-and-license">
			<h2><i18n:text>xmlui.dri2xhtml.METS-1.0.copyright</i18n:text></h2>
			<p class="copyright-text">
				<xsl:copy-of select="dim:field[@element='rights'][not(@qualifier)]/node()"/>
			</p>
		</div>
	</xsl:if>
</xsl:template>

<!-- changed h3 to h2 -->

<xsl:template match="dim:dim" mode="collectionDetailView-DIM"> 
	<xsl:if test="string-length(dim:field[@element='description'][not(@qualifier)])&gt;0">
		<p class="intro-text">
			<xsl:copy-of select="dim:field[@element='description'][not(@qualifier)]/node()"/>
		</p>
	</xsl:if>
	
	<xsl:if test="string-length(dim:field[@element='description'][@qualifier='tableofcontents'])&gt;0">
		<div class="detail-view-news">
			<h2><i18n:text>xmlui.dri2xhtml.METS-1.0.news</i18n:text></h2>
			<p class="news-text">
				<xsl:copy-of select="dim:field[@element='description'][@qualifier='tableofcontents']/node()"/>
			</p>
		</div>
	</xsl:if>
	
	<xsl:if test="string-length(dim:field[@element='rights'][not(@qualifier)])&gt;0 or string-length(dim:field[@element='rights'][@qualifier='license'])&gt;0">
		<div class="detail-view-rights-and-license">
			<h2><i18n:text>xmlui.dri2xhtml.METS-1.0.copyright</i18n:text></h2>
			<xsl:if test="string-length(dim:field[@element='rights'][not(@qualifier)])&gt;0">
				<p class="copyright-text">
					<xsl:copy-of select="dim:field[@element='rights'][not(@qualifier)]/node()"/>
				</p>
			</xsl:if>
			<xsl:if test="string-length(dim:field[@element='rights'][@qualifier='license'])&gt;0">
				<p class="license-text">
					<xsl:copy-of select="dim:field[@element='rights'][@qualifier='license']/node()"/>
				</p>
			</xsl:if>
		</div>
	</xsl:if>
</xsl:template>


</xsl:stylesheet>