<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
    xmlns:dri="http://di.tamu.edu/DRI/1.0/"
    xmlns:mets="http://www.loc.gov/METS/"
    xmlns:xlink="http://www.w3.org/TR/xlink/"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    xmlns:mods="http://www.loc.gov/mods/v3"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:confman="org.dspace.core.ConfigurationManager"
    exclude-result-prefixes="confman dc dim dri i18n mets mods xhtml xlink xsl">

    <xsl:variable name="upper" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'"/>
    <xsl:variable name="lower" select="'abcdefghijklmnopqrstuvwxyz'"/>
    <xsl:variable name="space"      select="' '"/>
    <xsl:variable name="space-esc"  select="'\ '"/>
    <xsl:variable name="field-sep"  select="':'"/>
    <xsl:variable name="field-join" select="'|||'"/>
    <xsl:variable name="field"      select="'prism.publicationName_filter'"/>
    <xsl:variable name="default-image" select="'/themes/Dryad/images/invisible.gif'"/>
    <xsl:variable name="downloads-class"    select="'download-query'"/>
    <xsl:variable name="downloads-url-base" select="concat('/', string(//dri:metadata[@element='request'][@qualifier='URI']), '/downloads/')"/>
    <xsl:variable name="data-download"      select="'data-download'"/>

    <!--
        Journal info and image
    -->
    <!-- suppress this element in document order -->
    <xsl:template match="dri:p[@id='aspect.journal.landing.JournalStats.p.hidden-fields']"/>
    <xsl:template match="//dri:document/dri:body/dri:div[@n='journal-landing-banner-outer']">
        <xsl:variable name="journal-name" select="string(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='journalName'])"/>
        <xsl:variable name="alt" select="concat($journal-name, ' cover')"/>
        <xsl:variable name="cover" select="string(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='journalCover'])"/>
        <xsl:variable name="website" select="string(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='journalWebsite'])"/>

        <div id="{translate(string(@id), '.', '_')}" class="ds-static-div primary clearfix">
            <table width="100%">
                <tr>
                    <td>
                        <h1 class="ds-div-head">
                            <xsl:value-of select=".//dri:p[@id='aspect.journal.landing.JournalStats.p.hidden-fields']/dri:field/@n"/>
                        </h1>
                        <xsl:apply-templates/>
                    </td>
                    <td>
                        <a href="{$website}">
                        <img alt="{$alt}"
                            src="{$cover}"
                            id="journal-logo"
                            class="pub-cover"
                            onerror="this.src='{$default-image}'"/>
                        </a>
                    </td>
                </tr>
            </table>
        </div>
    </xsl:template>

    <!--
        Find data in Dryad associated with ...
    -->
    <xsl:template match="//dri:document/dri:body/dri:div[@n='journal-landing-search']">
        <xsl:variable name="label" select="'Enter keyword, author, title, DOI.'"/>
        <xsl:variable name="placeholder" select="concat($label, ' ', 'Example: herbivory')"/>

        <!-- journal name filter parameters for solr query -->
        <xsl:variable name="journal-name" select="string(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='journalName'])"/>
        <xsl:variable name="journal-name-esc">
            <xsl:call-template name="escape-name">
                <xsl:with-param name="str" select="$journal-name"/>
                <xsl:with-param name="find" select="$space"/>
                <xsl:with-param name="replace" select="$space-esc"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="journal-name-lower" select="translate($journal-name-esc, $upper, $lower)"/>
        <xsl:variable name="fq"                 select="concat($field, $field-sep, $journal-name-lower, $field-join, $journal-name-esc)"/>

        <xsl:apply-templates select="dri:head"/>
        <form id="{translate(string(@id), '.', '_')}" class="ds-interactive-div primary"
	      action="/discover" method="get" onsubmit="javascript:tSubmit(this);">
	  <p class="ds-paragraph" style="overflow; hidden;
					 margin-bottom: 0px;"> 
	    <input type="hidden" name="fq" value="{$fq}"></input> 
	    <input id="aspect_journal_landing_JournalSearch_field_submit" class="ds-button-field" name="submit"
		   type="submit" value="{concat('View all data from ', $journal-name)}" style="margin-right: -4px;"/> 
	    <br/> 
	    <br/>
	    <label for="aspect_discovery_SiteViewer_field_query" class="accessibly-hidden"> 
	      <xsl:value-of select="$label"/> 
	    </label>
	    <input id="aspect_journal_landing_JournalSearch_field_query" class="ds-text-field" name="query"
		   placeholder="{$placeholder}" title="{$placeholder}"
		   type="text" value="" style="width: 60%;"/><!-- no whitespace between these!
		   --><input id="aspect_journal_landing_JournalSearch_field_submit" class="ds-button-field" name="submit"
		   type="submit" value="Search" style="margin-right:-4px;"/>
	  </p>
	</form>    
    </xsl:template>

    <!--
        Browse for data
    -->
    <xsl:template name="handle-tab-data">
        <xsl:param name="id"/>
        <div id="{$id}" class="ds-static-div primary">
            <xsl:for-each select="dri:div">
                <xsl:variable name="id2" select="concat($id, '-', string(position()))"/>
                
                <div id="{$id2}" class="browse-data-panel">
                    <xsl:attribute name="style">
                        <xsl:choose>
                            <xsl:when test="position() = 1">overflow: auto;</xsl:when>
                            <xsl:otherwise>overflow: auto; display: none;</xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <!-- add on-load handler for downloads data query 
                        <metadata element="request" qualifier="URI">journal/0002-9122</metadata>
                    -->
                    <xsl:if test="@n='downloads'">
                        <xsl:attribute name="class"><xsl:value-of select="$downloads-class"/></xsl:attribute>
                        <xsl:attribute name="{$data-download}"><xsl:value-of select="concat($downloads-url-base, @rend)"/></xsl:attribute>
                    </xsl:if>
                    
                    <table>
                        <xsl:choose>
                            <xsl:when test="dri:div[@n='vals']/dri:list/dri:item">
                                <thead>
                                    <tr style="width:100%">
                                        <th style="float:left">
                                            <h2 class="ds-head">
                                                <i18n:text><xsl:value-of select="dri:div[@n='items']/dri:referenceSet/dri:head"/></i18n:text>
                                            </h2>
                                        </th>
                                        <th style="width:20%">
                                            <h2 class="ds-head">
                                                <i18n:text><xsl:value-of select="dri:div[@n='vals']/dri:list/dri:head"/></i18n:text>
                                            </h2>
                                        </th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <xsl:for-each select="dri:div[@n='vals']/dri:list/dri:item">
                                        <xsl:variable name="position" select="position()"/>
                                        <tr>
                                            <td>
                                                <xsl:apply-templates select="ancestor::dri:div[@n='vals']/preceding-sibling::dri:div[@n='items']/dri:referenceSet/dri:reference[$position]" mode="journalLanding"/>
                                            </td>
                                            <td align="center">
                                                <xsl:apply-templates select="."/>
                                            </td>
                                        </tr>
                                    </xsl:for-each>
                                </tbody>
                            </xsl:when>
                            <xsl:otherwise>
                                <tbody>
                                    <tr>
                                        <td colspan="2">
                                            <i18n:text>xmlui.JournalLandingPage.JournalLandingTabbedTransformer.noDataAvailable</i18n:text>
                                        </td>
                                    </tr>
                                </tbody>
                            </xsl:otherwise>
                        </xsl:choose>
                    </table>
                </div>
            </xsl:for-each>
        </div>
    </xsl:template>
    
    <xsl:template match="//dri:document/dri:body/dri:div[@id='aspect.journal.landing.JournalDownloads.div.journal-landing-stats']">
        <xsl:call-template name="handle-tab-data">
            <xsl:with-param name="id" select="translate(string(@id), '.', '_')"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="//dri:document/dri:body/dri:div[@id='aspect.journal.landing.JournalStats.div.journal-landing-stats']">
        <xsl:variable name="id" select="translate(string(@id), '.', '_')"/>
        <xsl:apply-templates select="dri:head"/>

        <div id="{$id}-browse-data-buttons" class="tab-buttons">
            <xsl:for-each select="dri:list[@n='tablist']/dri:item">
                <a href="#{concat($id, '-', string(position()))}">
                    <xsl:if test="position()=1">
                        <xsl:attribute name="class">selected</xsl:attribute>
                    </xsl:if>
                    <xsl:if test="position() != 1">
                        <xsl:attribute name="class">disabled</xsl:attribute>
                    </xsl:if>
                    <span>
                        <i18n:text><xsl:value-of select="."/></i18n:text>
                    </span>
                </a>
            </xsl:for-each>
        </div>
        <xsl:call-template name="handle-tab-data">
            <xsl:with-param name="id" select="$id"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="dri:reference" mode="journalLanding">
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!-- Since this is a summary only grab the descriptive metadata, and the thumbnails -->
            <xsl:text>?sections=dmdSec,fileSec&amp;fileGrpTypes=THUMBNAIL</xsl:text>
        </xsl:variable>
        <xsl:attribute name="class">
            <xsl:choose>
                <xsl:when test="position() mod 2 = 0">even</xsl:when>
                <xsl:otherwise>odd</xsl:otherwise>
            </xsl:choose>
        </xsl:attribute>
        <xsl:apply-templates select="document($externalMetadataURL)" mode="summaryList"/>
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="//dri:div[@n='journal-landing-banner-inner']/dri:p/dri:hi" priority="2">
        <strong><i18n:text><xsl:value-of select="."/></i18n:text></strong>
    </xsl:template>

    <!--
        Utility template to replace all $find in $str with $replace.
    -->
    <xsl:template name="escape-name">
        <xsl:param name="str"/>
        <xsl:param name="find"/>
        <xsl:param name="replace"/>
        <xsl:choose>
            <xsl:when test="string-length($str) &gt; 0 and contains($str, $find)">
                <xsl:variable name="str-before" select="substring-before($str,$find)"/>
                <xsl:variable name="str-after">
                    <xsl:call-template name="escape-name">
                        <xsl:with-param name="find" select="$find"/>
                        <xsl:with-param name="replace" select="$replace"/>
                        <xsl:with-param name="str" select="substring-after($str, $find)"/>
                    </xsl:call-template>
                </xsl:variable>
                <xsl:value-of select="concat($str-before,$replace,$str-after)"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$str"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
