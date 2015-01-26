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

    <xsl:template match="//dri:document/dri:body/dri:div[@id='aspect.journal.landing.TopTenDownloads.div.journal-landing-topten-downloads']">
        <xsl:call-template name="journal-landing-panel-tabs"/>
    </xsl:template>

    <xsl:template match="//dri:document/dri:body/dri:div[@id='aspect.journal.landing.TopTenViews.div.journal-landing-topten-views']">
        <xsl:call-template name="journal-landing-panel-tabs"/>
    </xsl:template>
    
    <xsl:template match="//dri:document/dri:body/dri:div[@id='aspect.journal.landing.MostRecentDeposits.div.journal-landing-recent']">
        <xsl:call-template name="journal-landing-panel"/>
    </xsl:template>
    
    <xsl:template match="//dri:document/dri:body/dri:div[@id='aspect.journal.landing.UserGeography.div.journal-landing-user-geo']">
        <xsl:call-template name="journal-landing-panel-tabs"/>
    </xsl:template>

    <xsl:template name="journal-landing-panel">
        <xsl:apply-templates select="dri:head"/>
        <div id="aspect_journal_landing_TopTenDownloads_journal-landing-topten" class="ds-static-div primary">
            <table>
                <tr>
                    <th><xsl:apply-templates select="dri:div[@n='items']/dri:head"/></th>
                    <th><xsl:apply-templates select="dri:div[@n='count']/dri:head"/></th>
                </tr>
                <xsl:for-each select="dri:div[@n='items']/dri:referenceSet/dri:reference">
                    <xsl:variable name="position" select="position()"/>
                    <tr>
                        <td>
                            <xsl:apply-templates select="." mode="summaryList"/>
                        </td>
                        <td>
                            <xsl:apply-templates select="ancestor::dri:div[@n='journal-landing-topten']/dri:div[@n='count']/dri:list/dri:item[position()=$position]"/>
                        </td>
                    </tr>
                </xsl:for-each>
            </table>
        </div>
    </xsl:template>

    <!-- tabbed panels: Top-10, User-Geography -->
    <xsl:template name="journal-landing-panel-tabs">
        
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
                            <tr>
                                <th style="float:left"><xsl:apply-templates select="dri:div[@n='items']/dri:referenceSet/dri:head"/></th>
                                <th><xsl:apply-templates select="dri:div[@n='vals']/dri:list/dri:head"/></th>
                            </tr>
                        </xsl:if>
                        <xsl:for-each select="dri:div[@n='vals']/dri:list/dri:item">
                            <xsl:variable name="position" select="position()"/>
                            <tr>
                                <td>
                                    <xsl:apply-templates select="ancestor::dri:div[@n='items']/preceding-sibling::dri:div[@n='items']/dri:referneceSet/dri:reference[position()=$position]" mode="summaryList"/>
                                </td>
                                <td>
                                    <xsl:apply-templates select="."/>
                                </td>
                            </tr>
                        </xsl:for-each>
                    </table>
                </div>
            </xsl:for-each>
        </div>
    </xsl:template>
    
    
    <!-- non-headered panels, which need @class="ds-static-div primary" for panel whitespace -->
    <xsl:template match="//dri:document/dri:body/dri:div[@n='journal-landing-banner-outer' or @n='journal-landing-dryadinfo-wrapper']">
        <div id="{translate(string(@id), '.', '_')}" class="ds-static-div primary">    
            <xsl:apply-templates/>
        </div>
    </xsl:template>
    
    <!-- 
    TODO: filter to journal name
    http://datadryad.org/discover?query=submit&fq=prism.publicationName_filter:molecular\%20ecology\|\|\|Molecular\%20Ecology
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

    <xsl:template match="//dri:document/dri:body/dri:div[@id='aspect.journal.landing.TopTenDownloads.div.journal-landing-recent']">
        <xsl:apply-templates select="dri:head"/>
        <div id="aspect_journal_landing_TopTenDownloads_journal-journal-landing-recent" class="ds-static-div primary">
            <table>
                <tr>
                    <th><xsl:apply-templates select="dri:div[@n='items']/dri:head"/></th>
                    <th><xsl:apply-templates select="dri:div[@n='date']/dri:head"/></th>
                </tr>
                <xsl:for-each select="dri:div[@n='items']/dri:referenceSet/dri:reference">
                    <xsl:variable name="position" select="position()"/>
                    <tr>
                        <td>
                            <xsl:apply-templates select="." mode="summaryList"/>
                        </td>
                        <td>
                            <xsl:apply-templates select="ancestor::dri:div[@n='journal-landing-topten']/dri:div[@n='date']/dri:list/dri:item[position()=$position]"/>
                        </td>
                    </tr>
                </xsl:for-each>
            </table>
        </div>
    </xsl:template>
    
</xsl:stylesheet>
