
<!--
    Parts of the artifactbrowser which are not
    specific to a single listing or page. These will not
    frequently be adapted in a theme

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
    xmlns:confman="org.dspace.core.ConfigurationManager"
    exclude-result-prefixes="i18n dri mets dim xlink xsl xalan encoder confman">

    <xsl:output indent="yes"/>

<!-- These templates are devoted to handling the referenceSet and reference elements. Although they are considered
    structural elements, neither of the two contains actual content. Instead, references contain references
    to object metadata under objectMeta, while referenceSets group references together.
-->


    <!-- Starting off easy here, with a summaryList -->

    <!-- Current issues:

        1. There is no check for the repository identifier. Need to fix that by concatenating it with the
            object identifier and using the resulting string as the key on items and reps.
        2. The use of a key index across the object store is cryptic and counterintuitive and most likely
            could benefit from better documentation.
    -->

    <!-- When you come to an referenceSet you have to make a decision. Since it contains objects, and each
        object is its own entity (and handled in its own template) the decision of the overall structure
        would logically (and traditionally) lie with this template. However, to accomplish this we would
        have to look ahead and check what objects are included in the set, which involves resolving the
        references ahead of time and getting the information from their METS profiles directly.

        Since this approach creates strong coupling between the set and the objects it contains, and we
        have tried to avoid that, we use the "pioneer" method. -->

    <!-- Summarylist case. This template used to apply templates to the "pioneer" object (the first object
        in the set) and let it figure out what to do. This is no longer the case, as everything has been
        moved to the list model. A special theme, called TableTheme, has beeen created for the purpose of
        preserving the pioneer model. -->
    <xsl:template match="dri:referenceSet[@type = 'summaryList']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <!-- Here we decide whether we have a hierarchical list or a flat one -->
        <xsl:choose>
            <xsl:when test="descendant-or-self::dri:referenceSet/@rend='hierarchy' or ancestor::dri:referenceSet/@rend='hierarchy'">
                <ul class="ds-artifact-list list-unstyled">
                    <xsl:apply-templates select="*[not(name()='head')]" mode="summaryList"/>
                </ul>
            </xsl:when>
            <xsl:otherwise>
                <ul class="ds-artifact-list list-unstyled">
                    <xsl:apply-templates select="*[not(name()='head')]" mode="summaryList"/>
                </ul>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Generate the logo, if present, from the file section -->
    <xsl:template match="mets:fileGrp[@USE='LOGO']">
        <div class="ds-logo-wrapper">
            <img src="{mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href}" class="logo img-responsive">
                <xsl:attribute name="alt">xmlui.dri2xhtml.METS-1.0.collection-logo-alt</xsl:attribute>
                <xsl:attribute name="attr" namespace="http://apache.org/cocoon/i18n/2.1">alt</xsl:attribute>
            </img>
        </div>
    </xsl:template>


    <xsl:template match="dri:referenceSet[@id='aspect.artifactbrowser.ItemViewer.referenceSet.collection-viewer']/dri:reference" mode="summaryView">
        <!-- simplified check to verify whether access rights are available in METS -->
        <xsl:variable name='METSRIGHTS-enabled' select="contains(confman:getProperty('plugin.named.org.dspace.content.crosswalk.DisseminationCrosswalk'), 'METSRIGHTS')" />
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!-- If this is an Item, display the METSRIGHTS section, so we
                 know which files have access restrictions.
                 This requires the METSRightsCrosswalk to be enabled! -->
            <xsl:if test="@type='DSpace Item' and $METSRIGHTS-enabled">
                <xsl:text>?rightsMDTypes=METSRIGHTS</xsl:text>
            </xsl:if>
        </xsl:variable>
        <!-- This comment just displays the full URL in an HTML comment, for easy reference. -->
        <xsl:comment> External Metadata URL: <xsl:value-of select="$externalMetadataURL"/> </xsl:comment>
        <xsl:apply-templates select="document($externalMetadataURL)" mode="summaryView"/>
        <!--<xsl:apply-templates /> prevents the collections section from being rendered in the default way-->
    </xsl:template>

    <xsl:template match="dri:referenceSet[@type = 'itemPageSummaryList']" priority="2">
        <ul class="ds-referenceSet-list">
            <xsl:apply-templates mode="itemPageSummaryList"/>
        </ul>
    </xsl:template>

    <xsl:template match="dri:reference" mode="itemPageSummaryList">
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!-- No options selected, render the full METS document -->
        </xsl:variable>
        <xsl:comment> External Metadata URL: <xsl:value-of select="$externalMetadataURL"/> </xsl:comment>
        <li>
            <xsl:apply-templates select="document($externalMetadataURL)" mode="itemPageSummaryList"/>
            <xsl:apply-templates />
        </li>
    </xsl:template>

    <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']][@LABEL='DSpace Collection']" mode="itemPageSummaryList">
        <xsl:call-template name="collectionItemPageSummaryList-DIM"/>
    </xsl:template>



</xsl:stylesheet>
