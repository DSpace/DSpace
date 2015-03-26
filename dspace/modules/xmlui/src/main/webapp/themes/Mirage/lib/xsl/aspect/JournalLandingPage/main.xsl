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
    <xsl:variable name="landing-placeholder" select="'&#x2013;'"/>

    <!-- 
        Journal info and image
    -->
    <xsl:template match="//dri:document/dri:body/dri:div[@n='journal-landing-banner-outer']">
        <xsl:variable name="journal-name" select="string(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='journalName'])"/>
        <xsl:variable name="journal-abbr" select="string(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='journalAbbr'])"/>
        <xsl:variable name="alt" select="concat($journal-name, ' cover')"/>
        <xsl:variable name="cover" select="concat('/themes/Dryad/images/coverimages/', $journal-abbr, '.png')"/>
        <xsl:variable name="default" select="'/themes/Dryad/images/invisible.gif'"/>
        <div id="{translate(string(@id), '.', '_')}" class="ds-static-div primary clearfix">
            <p style="position: relative; float: right; max-width:100%; max-height:100%">
                <img alt="{$alt}" 
                    src="{$cover}" 
                    id="journal-logo"
                    class="pub-cover"
                    onerror="this.src='{$default}'"></img>
            </p>
            <xsl:apply-templates/>
        </div>
    </xsl:template>

    <!-- 
        Search data in Dryad associated with ... 
    -->
    <xsl:template match="//dri:document/dri:body/dri:div[@n='journal-landing-search']">
        <xsl:variable name="label" select="'Enter keyword, author, title, DOI.'"/>
        <xsl:variable name="placeholder" select="concat($label, ' ', 'Example: herbivory')"/>
        <xsl:variable name="journal-name" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='journalName']"/>
        <xsl:variable name="fq" select="concat('prism.publicationName_filter:',
                                                translate($journal-name, $upper, $lower), 
                                                '|||', $journal-name)"/>
        <xsl:apply-templates select="dri:head"/>
        <form id="{translate(string(@id), '.', '_')}" class="ds-interactive-div primary"
              action="/discover" method="get" onsubmit="javascript:tSubmit(this);">
            <input type="hidden" name="fq" value="{$fq}"></input>
            <p class="ds-paragraph" style="overflow; hidden; margin-bottom: 0px;">
                <label for="aspect_discovery_SiteViewer_field_query" class="accessibly-hidden">
                    <xsl:value-of select="$label"/>
                </label>
                <input id="aspect_journal_landing_JournalSearch_field_query" class="ds-text-field" name="query"
                    placeholder="{$placeholder}" title="{$placeholder}"
                    type="text" value="" style="width: 80%;"/><!-- no whitespace between these!
                     --><input id="aspect_journal_landing_JournalSearch_field_submit" class="ds-button-field" name="submit"
                    type="submit" value="Go" style="margin-right: -4px;"/>
            </p>
        </form>
    </xsl:template>

    <!-- 
        Browse for data
    -->
    <xsl:template match="//dri:document/dri:body/dri:div[@id='aspect.journal.landing.JournalStats.div.journal-landing-stats']">        
        <xsl:variable name="id" select="translate(string(@id), '.', '_')"/>
        <xsl:apply-templates select="dri:head"/>

        <div id="{$id}-browse-data-buttons" class="tab-buttons">
            <xsl:for-each select="dri:list[@n='tablist']/dri:item">
                <a href="#{concat($id, '-', string(position()))}">
                    <xsl:if test="position()=1">
                        <xsl:attribute name="class">selected</xsl:attribute>
                    </xsl:if>
                    <!-- TODO: debug why the i18n:text is necesary -->
                    <span>
                        <i18n:text><xsl:value-of select="."/></i18n:text>
                    </span>
                </a>
            </xsl:for-each>
        </div>

        <div id="{$id}" class="ds-static-div primary">
            <xsl:for-each select="dri:div">
                <xsl:variable name="id2" select="concat($id, '-', string(position()))"/>
                <div id="{$id2}" class="browse-data-panel" style="">
                    <xsl:attribute name="style">
                        <xsl:choose>
                            <xsl:when test="position() = 1">overflow: auto;</xsl:when>
                            <xsl:otherwise>overflow: auto; display: none;</xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <table>
                        <xsl:if test="dri:div[@n='items']/dri:referenceSet/dri:head or 
                                      dri:div[@n='vals']/dri:list/dri:head"
                        >
                            <tr style="width:100%">
                                <th style="float:left"><xsl:apply-templates select="dri:div[@n='items']/dri:referenceSet/dri:head"/></th>
                                <th style="width:20%"><xsl:apply-templates select="dri:div[@n='vals']/dri:list/dri:head"/></th>
                            </tr>
                        </xsl:if>
                        <xsl:choose>
                            <xsl:when test="dri:div[@n='vals']/dri:list/dri:item">
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
                            </xsl:when>
                            <xsl:otherwise>
                                <tr>
                                    <td><xsl:value-of select="$landing-placeholder"/></td>
                                    <td align="center"><xsl:value-of select="$landing-placeholder"/></td>
                                </tr>
                            </xsl:otherwise>
                        </xsl:choose>
                    </table>
                </div>
            </xsl:for-each>
        </div>
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

</xsl:stylesheet>