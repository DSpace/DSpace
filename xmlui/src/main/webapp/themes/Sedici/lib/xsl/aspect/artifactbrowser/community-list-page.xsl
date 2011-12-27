<!--
	Muestra la community-list para el request /community-list.
	Difiere de cualquier otra que tiene que tener un menu para desplegarse.
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
    
    <!-- Cuando es el div del request /community-list lo muestro para que sea desplegable -->
    
    <xsl:template match="dri:div[@id='aspect.artifactbrowser.CommunityBrowser.div.comunity-browser']">
		 <xsl:apply-templates mode='community-list-page'>
		   <!-- Mando este parametro para definir si es comunidad base o no -->
		   <xsl:with-param name="inicio">true</xsl:with-param>
		 </xsl:apply-templates>
    </xsl:template>
    
    <xsl:template match="dri:head" mode='community-list-page'>
       <h1 class="ds-div-head"><i18n:text><xsl:value-of select="."/></i18n:text></h1>     
    </xsl:template>
    
    <xsl:template match="dri:p" mode='community-list-page'>
       <p class="ds-paragraph" ><i18n:text><xsl:value-of select="."/></i18n:text></p>       
    </xsl:template>
    
    <xsl:template match="dri:referenceSet[@type = 'summaryList']" priority="2" mode='community-list-page'>
        <xsl:param name="inicio">false</xsl:param>
        <xsl:apply-templates select="dri:head"/>
        <!-- Here we decide whether we have a hierarchical list or a flat one -->
        <xsl:choose>
            <xsl:when test="$inicio = 'false'">
                <!-- Si el parametro inicio no viene entonces no es una comunidad base, por lo que debe mostrarse oculta la UL -->
                <ul style='display:none;'>
                    <xsl:apply-templates select="*[not(name()='head')]" mode="community-list-page"/>
                </ul>
            </xsl:when>
            <xsl:when test="descendant-or-self::dri:referenceSet/@rend='hierarchy' or ancestor::dri:referenceSet/@rend='hierarchy'">
                <ul>
                    <xsl:apply-templates select="*[not(name()='head')]" mode="community-list-page"/>
                </ul>
            </xsl:when>
            <xsl:otherwise>
                <ul class="ds-artifact-list">
                    <xsl:apply-templates select="*[not(name()='head')]" mode="community-list-page"/>
                </ul>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    
    <!-- Then we resolve the reference tag to an external mets object -->
    <xsl:template match="dri:reference" mode="community-list-page">
        <xsl:variable name="id" select="translate((translate(substring-after(@url,'/metadata/handle/'),'/','-')),'-mets.xml','')"/>
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!-- Since this is a summary only grab the descriptive metadata, and the thumbnails -->
            <xsl:text>?sections=dmdSec,fileSec&amp;fileGrpTypes=THUMBNAIL</xsl:text>
            <!-- An example of requesting a specific metadata standard (MODS and QDC crosswalks only work for items)->
            <xsl:if test="@type='DSpace Item'">
                <xsl:text>&amp;dmdTypes=DC</xsl:text>
            </xsl:if>-->
        </xsl:variable>
        <xsl:comment> External Metadata URL: <xsl:value-of select="$externalMetadataURL"/> </xsl:comment>
        <li id="li-{$id}">
            <xsl:attribute name="class">
                <xsl:text>ds-artifact-item </xsl:text>
                <xsl:choose>
                    <xsl:when test="position() mod 2 = 0">even</xsl:when>
                    <xsl:otherwise>odd</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            
            <!-- Si es una comunidad muestro el boton para desplegar -->
            <xsl:if test="@type='DSpace Community'">
	           <div id='div-{$id}' class='div-boton-menu-desplegable'>
        	   		<a href="javascript:llamar_alerta('div-{$id}','boton-{$id}','ocultar', 'ver');" type="button" class='ocultador' id='boton-{$id}' value="ver">+</a>
        	   </div>
            </xsl:if>
            
            <xsl:apply-templates select="document($externalMetadataURL)" mode="community-list-page"/>
            
            <xsl:apply-templates mode='community-list-page'/>
        </li>
    </xsl:template>
    
   <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']]" mode="community-list-page">
        <xsl:choose>
            <xsl:when test="@LABEL='DSpace Collection'">
                <xsl:call-template name="collectionSummaryList-DIM-communityPage"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Community'">
                <xsl:call-template name="communitySummaryList-DIM-communityPage"/>
            </xsl:when>                
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    

    <xsl:template name="communitySummaryList-DIM-communityPage">
        <xsl:variable name="data" select="./mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim"/>
        <xsl:variable name="id" select="translate(substring-after(@OBJID,'/'),'/','-')"/>
        <div class="artifact-description-community">
            <div class="artifact-title">
                <a href="{@OBJID}">
                    <span class="Z3988">                    
                        <xsl:choose>
                            <xsl:when test="string-length($data/dim:field[@element='title'][1]) &gt; 0">
                                <xsl:value-of select="$data/dim:field[@element='title'][1]"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                            </xsl:otherwise>
                        </xsl:choose>
                    </span>
                </a>
            </div>
        </div>
    </xsl:template>

    <!-- A collection rendered in the summaryList pattern. Encountered on the community-list page -->
    <xsl:template name="collectionSummaryList-DIM-communityPage">
        <xsl:variable name="data" select="./mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim"/>
        <div class="artifact-description-collection">
            <div class="artifact-title">
                <a href="{@OBJID}">
                    <span class="Z3988">
                        <xsl:choose>
                            <xsl:when test="string-length($data/dim:field[@element='title'][1]) &gt; 0">
                                <xsl:value-of select="$data/dim:field[@element='title'][1]"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                            </xsl:otherwise>
                        </xsl:choose>
                    </span>
                </a>
           
            </div>
        </div>
    </xsl:template>
</xsl:stylesheet>
