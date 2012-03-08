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
    <xsl:template match="dri:div[@id='aspect.artifactbrowser.CommunityViewer.div.community-home']/dri:head">
          <xsl:choose>
          <xsl:when test="/dri:document/dri:body/dri:div[@id='ar.edu.unlp.sedici.aspect.collectionViewer.CommunityViewer.div.community-view-root']">
          <xsl:apply-templates select="/dri:document/dri:body/dri:div[@id='ar.edu.unlp.sedici.aspect.collectionViewer.CommunityViewer.div.community-view-root']/dri:referenceSet">
		            <xsl:with-param name="forzado">true</xsl:with-param>  
		            <xsl:with-param name="id">community-root-div</xsl:with-param>          
		  </xsl:apply-templates>
		  </xsl:when>
		  <xsl:otherwise>
			   <xsl:apply-templates select="/dri:document/dri:body/dri:div[@id='aspect.artifactbrowser.CommunityViewer.div.community-home']/dri:div[@id='aspect.artifactbrowser.CommunityViewer.div.community-view']/dri:referenceSet" mode='image-header'>
	            <xsl:with-param name="forzado">true</xsl:with-param>  
	            <xsl:with-param name="id">community-actual-div</xsl:with-param>            
	          </xsl:apply-templates>
		  </xsl:otherwise>
		  </xsl:choose>
		  
          <div id="collection-title">
          	<h1 class="ds-div-head"><xsl:value-of select='.'/></h1>
          </div>
          
          <xsl:if test="/dri:document/dri:body/dri:div[@id='ar.edu.unlp.sedici.aspect.collectionViewer.CommunityViewer.div.community-view-root']">
	          <xsl:apply-templates select="/dri:document/dri:body/dri:div[@id='aspect.artifactbrowser.CommunityViewer.div.community-home']/dri:div[@id='aspect.artifactbrowser.CommunityViewer.div.community-view']/dri:referenceSet" mode='image-header'>
	            <xsl:with-param name="forzado">true</xsl:with-param>  
	            <xsl:with-param name="id">community-actual-div</xsl:with-param>            
	          </xsl:apply-templates>
		  </xsl:if>
		  
    </xsl:template>

  
   <xsl:template match="/dri:document/dri:body/dri:div[@id='aspect.artifactbrowser.CommunityViewer.div.community-home']/dri:div[@id='aspect.artifactbrowser.CommunityViewer.div.community-view']/dri:referenceSet" mode='image-header'>
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

   <xsl:template match="/dri:document/dri:body/dri:div[@id='ar.edu.unlp.sedici.aspect.collectionViewer.CommunityViewer.div.community-view-root']/dri:referenceSet">
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
   
   	<!-- A community rendered in the detailView pattern; default way of viewing a community. -->
    <xsl:template name="communityDetailView-DIM">
        <div class="detail-view">&#160;
            <!-- Generate the logo, if present, from the file section -->
            <!-- <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='LOGO']"/> -->
            <!-- Generate the info about the collections from the metadata section -->
            <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
                mode="communityDetailView-DIM"/>
        </div>
        
    </xsl:template>    
   
   <!-- -----------Templates para el manejo de la vista de una comunidad en especial ------------ -->
   
   <xsl:template match="dri:div[@n='community-home']">
     <div id="aspect_artifactbrowser_CommunityViewer_div_community-home" class="ds-static-div primary repository community">
        <xsl:apply-templates select="dri:head"/>
        <xsl:apply-templates select="dri:div[@n='community-view']"/>  
        <xsl:apply-templates select="dri:div[@n='community-recent-submission']"/>
        <xsl:apply-templates select="dri:div[@n='community-view-root']"/>
     </div>              
    </xsl:template>
    
    <xsl:template match="dri:div[@n='community-view']">
         <xsl:apply-templates select="dri:referenceSet/dri:reference" mode='mi-community-view'/>
         <xsl:apply-templates select="/dri:document/dri:body/dri:div[@id='aspect.artifactbrowser.CommunityViewer.div.community-home']/dri:div[@n='community-search-browse']"/>
         <xsl:apply-templates select="dri:referenceSet/dri:reference/dri:referenceSet"/>        
    </xsl:template>
    
    <xsl:template match='dri:reference' mode='mi-community-view'>
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!-- No options selected, render the full METS document -->
        </xsl:variable>
        <xsl:comment> External Metadata URL: <xsl:value-of select="$externalMetadataURL"/> </xsl:comment>
        <xsl:apply-templates select="document($externalMetadataURL)" mode="detailView"/>
     </xsl:template>


</xsl:stylesheet>