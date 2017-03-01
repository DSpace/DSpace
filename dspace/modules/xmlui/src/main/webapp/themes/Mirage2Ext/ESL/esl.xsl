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
    
  <!-- Item Detailed View: The block of templates used to render the complete DIM contents of a DRI object -->
    <xsl:template name="itemDetailView-DIM">
        <p id="visitEsl">
            Visit the <a href="http://esl.tamu.edu/">Energy Systems Laboratory Homepage</a>.
        </p>
        <table class="ds-includeSet-table detailtable table table-striped table-hover">
            <xsl:apply-templates mode="itemDetailView-DIM"/>
        </table>
    </xsl:template>
    
    <xsl:template name="itemSummaryView-DIM">
        <p id="visitEsl">
            <span class="bold">Visit the <a href="http://esl.tamu.edu/">Energy Systems Laboratory Homepage</a>.</span>
        </p>
        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
        mode="itemSummaryView-DIM"/>

        <xsl:copy-of select="$SFXLink" />

        <!-- Generate the Creative Commons license information from the file section (DSpace deposit license hidden by default)-->
        <xsl:if test="./mets:fileSec/mets:fileGrp[@USE='CC-LICENSE' or @USE='LICENSE']">
            <div class="license-info table">
                <p>
                    <i18n:text>xmlui.dri2xhtml.METS-1.0.license-text</i18n:text>
                </p>
                <ul class="list-unstyled">
                    <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CC-LICENSE' or @USE='LICENSE']" mode="simple"/>
                </ul>
            </div>
        </xsl:if>    
    </xsl:template>    
</xsl:stylesheet>