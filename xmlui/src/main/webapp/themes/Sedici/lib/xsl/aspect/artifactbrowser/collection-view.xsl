<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<!--
    Rendering specific to the collection home page.

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
    exclude-result-prefixes="xalan encoder i18n dri mets dim  xlink xsl">

    <xsl:output indent="yes"/>


    <!-- Capturo el id del div header, y muestra el titulo con el logo de la comunidad -->    
    <xsl:template match="dri:div[@id='aspect.artifactbrowser.CollectionViewer.div.collection-home']/dri:head">
          <xsl:apply-templates select="/dri:document/dri:body/dri:div[@id='ar.edu.unlp.sedici.aspect.collectionViewer.CollectionViewer.div.community-view-root']/dri:referenceSet">
            <xsl:with-param name="forzado">true</xsl:with-param>    
            <xsl:with-param name="id">community-root-div</xsl:with-param>           
          </xsl:apply-templates>
          <div id="collection-title">
          <h1 class="ds-div-head"><xsl:value-of select='.'/></h1>
          </div>
          <xsl:apply-templates select="/dri:document/dri:body/dri:div[@id='ar.edu.unlp.sedici.aspect.collectionViewer.CollectionViewer.div.community-view-top']/dri:referenceSet">
            <xsl:with-param name="forzado">true</xsl:with-param>   
            <xsl:with-param name="id">community-actual-div</xsl:with-param>            
          </xsl:apply-templates>

    </xsl:template>

   <xsl:template match="/dri:document/dri:body/dri:div[@id='ar.edu.unlp.sedici.aspect.collectionViewer.CollectionViewer.div.community-view-root']/dri:referenceSet">
           <xsl:param name="forzado"/>
           <xsl:param name="id">community-div</xsl:param>
          <xsl:choose>
           <xsl:when test="$forzado = 'true'">
            <div class="{$id}">
             <xsl:apply-templates select='dri:reference' mode='image-header'/> 
            </div>
           </xsl:when>
           <xsl:otherwise>
           <p></p>
           </xsl:otherwise>
           </xsl:choose>
   </xsl:template>
   
   <xsl:template match="/dri:document/dri:body/dri:div[@id='ar.edu.unlp.sedici.aspect.collectionViewer.CollectionViewer.div.community-view-top']/dri:referenceSet">
           <xsl:param name="forzado"/>
           <xsl:param name="id">community-div</xsl:param>
          <xsl:choose>
           <xsl:when test="$forzado = 'true'">
           <div class="{$id}">
             <xsl:apply-templates select='dri:reference' mode='image-header'/> 
            </div>
           </xsl:when>
           <xsl:otherwise>
           <p></p>
           </xsl:otherwise>
           </xsl:choose>
   </xsl:template>

   <xsl:template match='dri:reference' mode='image-header'>
     <xsl:call-template name="image-header">
            <xsl:with-param name="url">
               <xsl:value-of select="@url"/>
            </xsl:with-param>
     </xsl:call-template>
   </xsl:template>
     
   <xsl:template name="image-header">
       <xsl:param name="url"/>
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="$url"/>
            <!-- No options selected, render the full METS document -->
        </xsl:variable>
        <xsl:comment> External Metadata URL: <xsl:value-of select="$externalMetadataURL"/> </xsl:comment>
        <xsl:apply-templates select="document($externalMetadataURL)" mode="image-header"/>


   </xsl:template>


    
    <xsl:template match='mets:METS' mode='image-header'>
            <img src="{mets:fileSec/mets:fileGrp[@USE='LOGO']/mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href}" class="logo">
                <xsl:attribute name="alt">xmlui.dri2xhtml.METS-1.0.collection-logo-alt</xsl:attribute>
                <xsl:attribute name="attr" namespace="http://apache.org/cocoon/i18n/2.1">alt</xsl:attribute>
            </img>
    </xsl:template>

       

</xsl:stylesheet>