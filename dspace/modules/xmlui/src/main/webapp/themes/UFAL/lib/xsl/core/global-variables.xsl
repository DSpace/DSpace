<!--
	/* Created for LINDAT/CLARIN */
    Global variables accessible from other templates

    Author: Amir Kamran

-->

<xsl:stylesheet version="1.0"
	xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/"
	xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns="http://www.w3.org/1999/xhtml"
    xmlns:confman="org.dspace.core.ConfigurationManager"
	exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc confman">

    

    <!-- Global variables -->


    <!--the max thumbnail height & width from dspace.cfg, needed for item view and item list pages-->
    <xsl:variable name="thumbnail.maxheight" select="confman:getIntProperty('thumbnail.maxheight', 80)"/>
    <xsl:variable name="thumbnail.maxwidth" select="confman:getIntProperty('thumbnail.maxwidth', 80)"/>

    <!-- file download options -->
    <xsl:variable name="lr.download.all.limit.min.file.count" select="confman:getIntProperty('lr', 'lr.download.all.limit.min.file.count', 1)"/>
    <xsl:variable name="lr.download.all.limit.max.file.size" select="confman:getLongProperty('lr', 'lr.download.all.limit.max.file.size', 1073741824)"/>
    <xsl:variable name="lr.download.all.alert.min.file.size" select="confman:getLongProperty('lr', 'lr.download.all.alert.min.file.size', 10485760)"/>
     
    <!-- file download options -->
    <xsl:variable name="lr.upload.file.alert.max.file.size" select="confman:getLongProperty('lr', 'lr.upload.file.alert.max.file.size', 2147483648)"/>
    
    <!-- help desk -->
    <xsl:variable name="lr.help.mail" select="confman:getProperty('lr.help.mail')"/>

    <!-- item details url -->
    <xsl:variable name="ds_item_view_toggle_url" select="//dri:p[contains(@rend , 'item-view-toggle') and
        (preceding-sibling::dri:referenceSet[@type = 'summaryView'] or following-sibling::dri:referenceSet[@type = 'summaryView'])]/dri:xref/@target"/>
        
    <xsl:variable name="context-path" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>

    <xsl:variable name="theme-path" select="concat($context-path,'/themes/UFAL')"/>
    
	<xsl:variable name="request-uri" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='URI']" />
	
	<xsl:variable name="query-string" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='queryString']" />

	<xsl:variable name="oai-url" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='OAIURL']" />
	<xsl:variable name="oai-handle"	select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='OAIHANDLE']" />

	<!-- dynamically select the static html file based on requested page name -->
    <xsl:variable name="static-page-name">
    	<xsl:if test="starts-with($request-uri, 'page/')">
    		<xsl:choose>
    		<xsl:when test="document(concat('../../html/', substring-after($request-uri, 'page/'), '.xml'))">
	    		<xsl:copy-of select="substring-after($request-uri, 'page/')" />
    		</xsl:when>
    		</xsl:choose>    		
    	</xsl:if>
    </xsl:variable>
        

</xsl:stylesheet>
