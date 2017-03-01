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

    xmlns:encoder="xalan://java.net.URLEncoder"
    
    exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc">
    <xsl:import href="../shared.xsl"/>
    <xsl:output indent="yes"/>

    <!-- TAMU Customization - Overriding template found in Mirage2/xsl/aspect/artifactbrowser/item-view.xsl
        Adding:
            itemSummaryView-DIM-advisors
            itemSummaryView-DIM-client
    -->
    <xsl:template match="dim:dim" mode="itemSummaryView-DIM">
        <div class="item-summary-view-metadata">
            <xsl:call-template name="itemSummaryView-DIM-title"/>
            <div class="row">
                <div class="col-sm-4">
                    <div class="row">
                        <div class="col-xs-6 col-sm-12">
                            <xsl:call-template name="itemSummaryView-DIM-thumbnail"/>
                        </div>
                        <div class="col-xs-6 col-sm-12">
                            <xsl:call-template name="itemSummaryView-DIM-file-section"/>
                        </div>
                    </div>
                    <xsl:call-template name="itemSummaryView-DIM-date"/>
                    <!-- TAMU Customization -->
                    <xsl:call-template name="itemSummaryView-DIM-advisors"/>
                    <xsl:call-template name="itemSummaryView-DIM-authors"/>
                    <xsl:if test="$ds_item_view_toggle_url != ''">
                        <xsl:call-template name="itemSummaryView-show-full"/>
                    </xsl:if>
                </div>
                <div class="col-sm-8">
                    <xsl:call-template name="itemSummaryView-DIM-abstract"/>
                    <xsl:call-template name="itemSummaryView-DIM-URI"/>
                    <!-- TAMU Customization -->
                    <xsl:call-template name="itemSummaryView-DIM-client"/>
                    <!-- TAMU Customization -->
                    <xsl:call-template name="itemSummaryView-DIM-description" />
                    <!-- TAMU Customization -->
                    <xsl:call-template name="itemSummaryView-DIM-subject" />
                    <!-- TAMU Customization -->
                    <xsl:call-template name="itemSummaryView-DIM-department" />
                    <xsl:call-template name="itemSummaryView-collections"/>
                    <!-- TAMU Customization -->
                    <xsl:call-template name="itemSummaryView-DIM-dwc"/>
                    <!-- TAMU Customization -->
                    <xsl:call-template name="itemSummaryView-DIM-citation"/>
                </div>
            </div>
        </div>
    </xsl:template>

    <xsl:template name="itemSummaryView-DIM-advisors">
        <xsl:if test="dim:field[@element='contributor' and @qualifier='advisor']">
            <div class="simple-item-view-advisors item-page-field-wrapper table">
                <h5>Project Advisor</h5>
                <xsl:for-each select="dim:field[@element='contributor' and @qualifier='advisor']">
                    <xsl:copy-of select="node()"/>
                    <xsl:if test="count(following-sibling::dim:field[@element='contributor' and @qualifier='advisor']) != 0"> <br class="simpleItemViewValueBreak"/> </xsl:if>
                </xsl:for-each>                   
            </div>
        </xsl:if>
    </xsl:template>

    <xsl:template name="itemSummaryView-DIM-client">
        <xsl:if test="dim:field[@element='contributor' and @qualifier='sponsor']">
            <div class="simple-item-view-clients item-page-field-wrapper table">
                <h5>Client</h5>
                <xsl:for-each select="dim:field[@element='contributor' and @qualifier='sponsor']">
                    <xsl:copy-of select="node()"/>
                    <xsl:if test="count(following-sibling::dim:field[@element='contributor' and @qualifier='sponsor']) != 0"> <br class="simpleItemViewValueBreak"/> </xsl:if>
                </xsl:for-each>                   
            </div>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>