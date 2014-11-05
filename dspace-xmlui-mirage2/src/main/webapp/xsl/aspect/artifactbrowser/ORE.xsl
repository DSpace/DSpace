<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->

<!--
    Files listing rendering specific to the ORE bundle

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
    xmlns:dcterms="http://purl.org/dc/terms/"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    exclude-result-prefixes="xalan encoder i18n dri mets dim  xlink xsl">


    <xsl:output indent="yes"/>

    <xsl:template match="mets:fileGrp[@USE='ORE']" mode="itemSummaryView-DIM">
        <xsl:variable name="AtomMapURL" select="concat('cocoon:/',substring-after(mets:file/mets:FLocat[@LOCTYPE='URL']//@*[local-name(.)='href'],$context-path))"/>
        <div class="item-page-field-wrapper table">
            <h5>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-viewOpen</i18n:text>
            </h5>

            <xsl:for-each select="document($AtomMapURL)/atom:entry/atom:link[@rel='http://www.openarchives.org/ore/terms/aggregates']">
                <xsl:variable name="link_href" select="@href"/>
                <xsl:if test="/atom:entry/oreatom:triples/rdf:Description[@rdf:about=$link_href][dcterms:description='ORIGINAL']
                            or not(/atom:entry/oreatom:triples/rdf:Description[@rdf:about=$link_href])">
                    <xsl:call-template name="itemSummaryView-DIM-file-section-entry">
                        <xsl:with-param name="href" select="@href" />
                        <xsl:with-param name="mimetype" select="@type" />
                        <xsl:with-param name="label-1" select="'title'" />
                        <xsl:with-param name="label-2" select="'title'" />
                        <xsl:with-param name="title" select="@title" />
                        <xsl:with-param name="label" select="@title" />
                        <xsl:with-param name="size" select="@length" />
                    </xsl:call-template>
                </xsl:if>
            </xsl:for-each>
        </div>
    </xsl:template>

    <!-- Rendering the file list from an Atom ReM bitstream stored in the ORE bundle -->
    <xsl:template match="mets:fileGrp[@USE='ORE']" mode="itemDetailView-DIM">
        <xsl:variable name="AtomMapURL" select="concat('cocoon:/',substring-after(mets:file/mets:FLocat[@LOCTYPE='URL']//@*[local-name(.)='href'],$context-path))"/>
        <h2><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-head</i18n:text></h2>
        <xsl:for-each select="document($AtomMapURL)/atom:entry/atom:link[@rel='http://www.openarchives.org/ore/terms/aggregates']">
            <xsl:variable name="link_href" select="@href"/>
            <xsl:if test="/atom:entry/oreatom:triples/rdf:Description[@rdf:about=$link_href][dcterms:description='ORIGINAL']
                        or not(/atom:entry/oreatom:triples/rdf:Description[@rdf:about=$link_href])">
                <xsl:call-template name="itemDetailView-DIM-file-section-entry">
                    <xsl:with-param name="href" select="@href" />
                    <xsl:with-param name="mimetype" select="@type" />
                    <xsl:with-param name="title" select="@title" />
                    <xsl:with-param name="size" select="@length" />
                </xsl:call-template>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>

</xsl:stylesheet>
