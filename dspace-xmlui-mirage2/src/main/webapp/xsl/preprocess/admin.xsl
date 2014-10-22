<?xml version="1.0" encoding="UTF-8"?>
<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->

<!--
    Author: Art Lowel (art at atmire dot com)

    The purpose of this file is to transform the DRI for some parts of
    DSpace into a format more suited for the theme xsls. This way the
    theme xsl files can stay cleaner, without having to change Java
    code and interfere with other themes

    e.g. this file can be used to add a class to a form field, without
    having to duplicate the entire form field template in the theme xsl
    Simply add it here to the rend attribute and let the default form
    field template handle the rest.
-->

<xsl:stylesheet
                xmlns="http://di.tamu.edu/DRI/1.0/"
                xmlns:dri="http://di.tamu.edu/DRI/1.0/"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
                exclude-result-prefixes="xsl dri i18n">

    <xsl:output indent="yes"/>

    <xsl:template match="dri:list[@rend='horizontal'][@n='options']">
        <list>
            <xsl:call-template name="copy-attributes"/>
            <xsl:attribute name="rend">
                <xsl:value-of select="@rend"/>
                <xsl:text>  nav nav-tabs hidden-xs hidden-sm visible-md visible-lg</xsl:text>
                <xsl:if test="count(dri:item) = 5">
                    <xsl:text>  small</xsl:text>
                </xsl:if>
            </xsl:attribute>
            <xsl:apply-templates/>
        </list>
        <list>
            <xsl:call-template name="copy-attributes"/>
            <xsl:attribute name="rend">
                <xsl:value-of select="@rend"/>
                <xsl:text>  nav nav-pills nav-stacked visible-xs visible-sm hidden-md hidden-lg</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="id">
                <xsl:value-of select="@id"/>
                <xsl:text>.sm.xs</xsl:text>
            </xsl:attribute>
            <xsl:apply-templates/>
        </list>
        <p><hi></hi></p>
    </xsl:template>

    <xsl:template match="dri:list[@rend='horizontal'][@n='options']/dri:item[dri:hi[@rend='bold']]">
        <item>
            <xsl:call-template name="copy-attributes"/>
            <xsl:attribute name="rend">
                <xsl:value-of select="@rend"/>
                <xsl:text> active</xsl:text>
            </xsl:attribute>
            <xsl:apply-templates select="dri:hi/*"/>
        </item>
    </xsl:template>

    <xsl:template match="dri:list[@n='metadataList']/dri:item/dri:figure">
        <div rend="ds-logo-wrapper">
        <figure>
            <xsl:call-template name="copy-attributes"/>
            <xsl:attribute name="rend">
                <xsl:value-of select="@rend"/>
                <xsl:text> logo img-responsive</xsl:text>
            </xsl:attribute>
        </figure>
        </div>
    </xsl:template>


    <xsl:template match="dri:list[@n='metadataList']/dri:item/dri:figure">
        <div rend="ds-logo-wrapper">
        <figure>
            <xsl:call-template name="copy-attributes"/>
            <xsl:attribute name="rend">
                <xsl:value-of select="@rend"/>
                <xsl:text> logo img-responsive</xsl:text>
            </xsl:attribute>
        </figure>
        </div>
    </xsl:template>


  <xsl:template match="*[not(@id='aspect.artifactbrowser.ConfigurableBrowse.div.browse-navigation')
  and not(@id='aspect.administrative.WithdrawnItems.div.browse-navigation') and not(@id='aspect.administrative.PrivateItems.div.browse-navigation')
  and not(@id='aspect.administrative.WithdrawnItems.div.browse-controls') and not(@id='aspect.administrative.PrivateItems.div.browse-controls')]/dri:p[count(./dri:field)>1 and not(dri:field[position()=1 and @type='text'] and dri:field[position()=2 and @type='button'])]">

      <xsl:for-each select="*">
          <p>
              <xsl:copy-of select="."/>
          </p>
      </xsl:for-each>
  </xsl:template>


    <xsl:template match="dri:div[@id='aspect.general.NoticeTransformer.div.general-message']">
        <div>
            <xsl:call-template name="copy-attributes"/>
            <xsl:attribute name="rend">
                <xsl:value-of select="@rend"/>
                <xsl:text> alert</xsl:text>
                <xsl:choose>
                    <xsl:when test="contains(@rend,'success' )"><xsl:text> alert-success</xsl:text></xsl:when>
                    <xsl:when test="contains(@rend,'failure' )"><xsl:text> alert-danger</xsl:text></xsl:when>
                    <xsl:when test="contains(@rend,'neutral' )"><xsl:text> alert-info</xsl:text></xsl:when>
                    <xsl:otherwise><xsl:text> alert-info</xsl:text></xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:if test="dri:head">
                <p>
                    <hi rend="alert-link">
                        <xsl:apply-templates select="dri:head/*"/>
                    </hi>
                </p>
            </xsl:if>
            <xsl:apply-templates select="*[not(name()='head')]"/>
        </div>
    </xsl:template>


    <xsl:template match="dri:p[dri:hi[@rend='warn']][count(dri:hi[@rend='warn'])=count(*)]">
        <p>
            <xsl:call-template name="copy-attributes"/>
            <xsl:attribute name="rend">
                <xsl:value-of select="@rend"/>
                <xsl:text> alert alert-warning</xsl:text>
            </xsl:attribute>
            <xsl:apply-templates/>
        </p>
    </xsl:template>


    <xsl:template match="dri:cell[count(dri:field)=count(*) and count(dri:field[not(@type='hidden')])>1]/dri:field[not(@type='hidden')]">
                <p><xsl:copy-of select="."/></p>
    </xsl:template>

    <xsl:template match="dri:hi[starts-with(@rend,'fade ') or @rend='fade' or  contains(@rend,' fade ')]">
       <hi>
           <xsl:call-template name="copy-attributes"/>
           <xsl:attribute name="rend">
               <xsl:choose>
                   <xsl:when test="starts-with(@rend,'fade ')">
                       <xsl:value-of select="substring-after(@rend, 'fade ')"/>
                   </xsl:when>
                   <xsl:when test="contains(@rend,' fade ')">
                       <xsl:value-of select="substring-before(@rend, ' fade ')"/>
                       <xsl:value-of select="substring-after(@rend, ' fade ')"/>
                   </xsl:when>
               </xsl:choose>
           </xsl:attribute>
           <xsl:apply-templates/>
       </hi>
    </xsl:template>


    <xsl:template match="dri:table[@n='roles-table']">

        <div rend="row">
            <div rend="col-sm-12">
                <xsl:for-each select="dri:row[@role='data']">
                    <xsl:if test="dri:cell[@role='header']">
                        <div rend="panel panel-default">
                            <div rend="panel-body">
                            <div rend="col-sm-4">
                                <p rend="panel-title">
                                    <xsl:apply-templates select="dri:cell[@role='header']"/>
                                </p>
                            </div>
                            <div rend="col-sm-4">
                                <p>
                                    <xsl:apply-templates select="dri:cell[not(@role='header') and not(dri:field)]"/>
                                </p>
                            </div>
                            <div rend="col-sm-2 pull-right col-xs-12 clearfix">
                                <xsl:apply-templates select="dri:cell[dri:field]"/>
                                <p><hi></hi></p>
                            </div>

                            <xsl:if test="following-sibling::dri:row[1][dri:cell[@cols='2' and @rows='1']]">

                                <div rend="col-sm-12 clearfix">
                                    <p>
                                        <xsl:apply-templates
                                                select="following-sibling::dri:row[1]/dri:cell[@cols='2' and @rows='1']/*"/>
                                    </p>
                                </div>
                            </xsl:if>
                        </div>
                        </div>
                    </xsl:if>
                </xsl:for-each>
            </div>
        </div>

    </xsl:template>

    <xsl:template match="dri:field[@id='aspect.eperson.PasswordLogin.field.submit']">
        <p>
            <xsl:copy-of select="."/>
        </p>
    </xsl:template>

    <xsl:template match="dri:list[@id='aspect.submission.StepTransformer.list.licenseclasslist']">
        <list>
            <xsl:call-template name="copy-attributes"/>
            <xsl:apply-templates/>
            <xsl:if test="count(//dri:list[@id='aspect.submission.StepTransformer.list.statusList']/dri:item) >1 ">
                <xsl:copy-of select="//dri:list[@id='aspect.submission.StepTransformer.list.statusList']/dri:item[1]"/>
            </xsl:if>
        </list>
    </xsl:template>

    <!--remove the shared authority stylesheet, this theme has its own-->
     <xsl:template match="dri:meta/dri:pageMeta/dri:metadata[@element='stylesheet'][@qualifier='screen'][@lang='datatables']"/>
     <xsl:template match="dri:meta/dri:pageMeta/dri:metadata[@element='stylesheet'][@qualifier='screen'][@lang='person-lookup']"/>
     <xsl:template match="dri:meta/dri:pageMeta/dri:metadata[@element='javascript'][@lang='person-lookup']"/>
     <xsl:template match="dri:meta/dri:pageMeta/dri:metadata[@element='javascript'][@lang='datatables']"/>



</xsl:stylesheet>
