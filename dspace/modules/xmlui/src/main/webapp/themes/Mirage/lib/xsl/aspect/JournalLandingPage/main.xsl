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
    exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc">
    
    <xsl:template match="//dri:document/dri:body/dri:div[@id='aspect.journal.landing.TopTenDownloads.div.journal-landing-topten']">
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