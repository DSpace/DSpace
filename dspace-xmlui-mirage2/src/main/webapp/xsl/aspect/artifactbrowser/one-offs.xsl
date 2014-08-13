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

    <xsl:template match="dri:p[dri:field[@id='aspect.discovery.SiteViewer.field.query']][dri:field[@id='aspect.discovery.SiteViewer.field.submit']]">
        <div class="row">
            <div class="input-group col-md-7">
            <xsl:apply-templates select="dri:field[@id='aspect.discovery.SiteViewer.field.query']"/>
            <span class="input-group-btn">
                <xsl:apply-templates
                        select="dri:field[@id='aspect.discovery.SiteViewer.field.submit']"/>
            </span>
        </div>
        </div>

    </xsl:template>

</xsl:stylesheet>
