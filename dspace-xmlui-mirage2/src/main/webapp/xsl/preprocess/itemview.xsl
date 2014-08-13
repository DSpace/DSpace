<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<xsl:stylesheet
        xmlns="http://di.tamu.edu/DRI/1.0/"
        xmlns:dri="http://di.tamu.edu/DRI/1.0/"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
        xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
        exclude-result-prefixes="xsl dri i18n">

    <xsl:output indent="yes"/>

    <xsl:template match="dri:referenceSet[@id='aspect.artifactbrowser.ItemViewer.referenceSet.collection-viewer'][@type='summaryView']/dri:reference/dri:referenceSet[@type='detailList']">
        <referenceSet>
            <xsl:call-template name="copy-attributes"/>
            <xsl:attribute name="type">
                <xsl:text>itemPageSummaryList</xsl:text>
            </xsl:attribute>
            <xsl:apply-templates select="*[not(name()='head')]"/>
        </referenceSet>
    </xsl:template>

</xsl:stylesheet>
