<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->

<!--
    Rendering of a list of collections (e.g. on a community homepage,
    or on the community-list page)

    Author: art.lowel at atmire.com
    Author: lieven.droogmans at atmire.com
    Author: ben at atmire.com
    Author: Alexey Maslov

-->

<xsl:stylesheet
        xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
        xmlns:dri="http://di.tamu.edu/DRI/1.0/"
        xmlns:mets="http://www.loc.gov/METS/"
        xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
        xmlns:xlink="http://www.w3.org/TR/xlink/"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
        xmlns:atom="http://www.w3.org/2005/Atom"
        xmlns:ore="http://www.openarchives.org/ore/terms/"
        xmlns:oreatom="http://www.openarchives.org/ore/atom/"
        xmlns="http://www.w3.org/1999/xhtml"
        xmlns:xalan="http://xml.apache.org/xalan"
        xmlns:encoder="xalan://java.net.URLEncoder"
        xmlns:util="org.dspace.app.xmlui.utils.XSLUtils"
        xmlns:confman="org.dspace.core.ConfigurationManager"
        exclude-result-prefixes="xalan encoder i18n dri mets dim xlink xsl util confman">

    <xsl:output indent="yes"/>

    <xsl:template match="dri:p[dri:field[@id='aspect.artifactbrowser.ConfigurableBrowse.field.month'
            or @id='aspect.administrative.WithdrawnItems.field.month' or @id='aspect.administrative.WithdrawnItems.field.month' ]]">
        <div class="row">
            <div class="col-sm-2">
                <xsl:apply-templates select="i18n:text"/>
            </div>
            <div class="col-sm-3">
                <xsl:apply-templates select="dri:field[@id='aspect.artifactbrowser.ConfigurableBrowse.field.month'
                    or @id='aspect.administrative.WithdrawnItems.field.month' or @id='aspect.administrative.PrivateItems.field.month']"/>
            </div>
            <div class="col-sm-3">
                <xsl:apply-templates select="dri:field[@id='aspect.artifactbrowser.ConfigurableBrowse.field.year'
                or @id='aspect.administrative.WithdrawnItems.field.year' or @id='aspect.administrative.PrivateItems.field.year']"/>
            </div>
        </div>
    </xsl:template>

    <xsl:template match="dri:p[dri:field/@id='aspect.artifactbrowser.ConfigurableBrowse.field.starts_with'
    or dri:field/@id='aspect.discovery.SearchFacetFilter.field.starts_with'
    or dri:field/@id='aspect.administrative.WithdrawnItems.field.starts_with'
    or dri:field/@id='aspect.administrative.PrivateItems.field.starts_with']">
        <div class="row">
            <div class="col-xs-12 col-sm-6">
                <p class="input-group">
                <xsl:apply-templates
                        select="dri:field[@id='aspect.artifactbrowser.ConfigurableBrowse.field.starts_with'
                        or @id='aspect.discovery.SearchFacetFilter.field.starts_with'
                        or @id='aspect.administrative.WithdrawnItems.field.starts_with'
                        or @id='aspect.administrative.PrivateItems.field.starts_with']"/>

                <span class="input-group-btn">
                    <xsl:apply-templates
                            select="dri:field[@id='aspect.artifactbrowser.ConfigurableBrowse.field.submit'
                            or @id='aspect.discovery.SearchFacetFilter.field.submit'
                            or @id='aspect.administrative.WithdrawnItems.field.submit'
                            or @id='aspect.administrative.PrivateItems.field.submit']"/>
                </span>
                </p>
            </div>
        </div>

    </xsl:template>

    <xsl:template match="dri:field[@id='aspect.artifactbrowser.ConfigurableBrowse.field.starts_with'
    or @id='aspect.discovery.SearchFacetFilter.field.starts_with'
    or @id='aspect.administrative.WithdrawnItems.field.starts_with'
    or @id='aspect.administrative.PrivateItems.field.starts_with']">
        <input>
            <xsl:call-template name="fieldAttributes"/>
            <xsl:attribute name="value">
                <xsl:choose>
                    <xsl:when test="./dri:value[@type='raw']">
                        <xsl:value-of select="./dri:value[@type='raw']"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="./dri:value[@type='default']"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:attribute name="title"><xsl:value-of select="dri:help"/></xsl:attribute>
            <xsl:attribute name="i18n:attr">placeholder title</xsl:attribute>
            <xsl:apply-templates select="*[not(self::dri:help)]"/>
        </input>
    </xsl:template>

    <xsl:template match="dri:p[dri:field/@id='aspect.artifactbrowser.ConfigurableBrowse.field.year'
        or dri:field/@id='aspect.administrative.WithdrawnItems.field.year'
        or dri:field/@id='aspect.administrative.PrivateItems.field.year']">
        <div class="form-group">
            <label><xsl:apply-templates select="i18n:text[1]"/></label>
            <div class="row">
                <div class="col-xs-5 col-sm-3">
                    <xsl:apply-templates select="dri:field[@id='aspect.artifactbrowser.ConfigurableBrowse.field.year'
                    or @id='aspect.administrative.WithdrawnItems.field.year'
                    or @id='aspect.administrative.PrivateItems.field.year' ]"/>
                </div>
                <div class="col-xs-7 col-sm-6">
                    <div class="input-group">
                        <xsl:apply-templates
                                select="../dri:p/dri:field[@id='aspect.artifactbrowser.ConfigurableBrowse.field.starts_with'
                                or @id='aspect.administrative.WithdrawnItems.field.starts_with'
                                or @id='aspect.administrative.PrivateItems.field.starts_with']"/>
                        <span class="input-group-btn">
                            <xsl:apply-templates
                                    select="../dri:p/dri:field[@id='aspect.artifactbrowser.ConfigurableBrowse.field.submit' or @id='aspect.administrative.WithdrawnItems.field.submit'
                                     or @id='aspect.administrative.PrivateItems.field.submit']"/>
                        </span>

                    </div>

                </div>
            </div>
        </div>
    </xsl:template>

    <xsl:template match="dri:div[dri:p/dri:field/@id='aspect.artifactbrowser.ConfigurableBrowse.field.year']/dri:p[dri:field/@id='aspect.artifactbrowser.ConfigurableBrowse.field.starts_with']"/>
    <xsl:template match="dri:div[dri:p/dri:field/@id='aspect.administrative.WithdrawnItems.field.year']/dri:p[dri:field/@id='aspect.administrative.WithdrawnItems.field.starts_with']"/>
    <xsl:template match="dri:div[dri:p/dri:field/@id='aspect.administrative.PrivateItems.field.year']/dri:p[dri:field/@id='aspect.administrative.PrivateItems.field.starts_with']"/>

</xsl:stylesheet>
