<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
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
                exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc">

    <xsl:output indent="yes"/>

    <xsl:template match="dri:body[dri:div[@rend='vocabulary-container']]" mode="modal">
        <div class="modal-dialog">
            <div class="modal-content">
                <xsl:variable name="header" select="$pagemeta/dri:metadata[@element='title'][not(@qualifier)]"/>
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button>
                    <xsl:if test="$header">
                        <h4 class="modal-title">
                            <xsl:apply-templates select="$header/*"/>
                        </h4>
                    </xsl:if>
                </div>
                <div class="modal-body">
                    <div class="clearfix">
                    <xsl:apply-templates />
                        </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-default" data-dismiss="modal"><i18n:text>xmlui.ChoiceLookupTransformer.cancel</i18n:text></button>
                </div>
            </div>
        </div>
    </xsl:template>

    <xsl:template match="dri:list[@id='aspect.submission.ControlledVocabularyTransformer.list.filter-list']/dri:item[dri:field[@n='filter']][dri:field[@n='filter_button']]">
        <xsl:attribute name="class">
            <xsl:text>control-group col-sm-12</xsl:text>
            <xsl:if test="dri:field/dri:error">
                <xsl:text> has-error</xsl:text>
            </xsl:if>
        </xsl:attribute>
        <xsl:call-template name="pick-label"/>
        <div class="row">
            <div class="col-xs-12">
                <p class="input-group">

                    <xsl:apply-templates select="dri:field[@n='filter']"/>
                    <span class="input-group-btn">
                        <xsl:apply-templates select="dri:field[@n='filter_button']"/>
                    </span>
                </p>
            </div>
        </div>
    </xsl:template>

    <xsl:template match="dri:list[@id='aspect.submission.ControlledVocabularyTransformer.list.filter-list']/dri:item[@n='vocabulary-error']">
        <div>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">
                    <xsl:value-of select="@rend"/><xsl:text> alert alert-danger</xsl:text>
                </xsl:with-param>
            </xsl:call-template>
        <p class="bg-danger">
            <xsl:apply-templates/>
        </p></div>
    </xsl:template>
    <xsl:template match="dri:list[@id='aspect.submission.ControlledVocabularyTransformer.list.filter-list']/dri:item[@n='vocabulary-loading']">
        <div>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">
                    <xsl:value-of select="@rend"/><xsl:text> alert</xsl:text>
                </xsl:with-param>
            </xsl:call-template>
        <p>
            <xsl:apply-templates/>
        </p>
        </div>
    </xsl:template>

</xsl:stylesheet>