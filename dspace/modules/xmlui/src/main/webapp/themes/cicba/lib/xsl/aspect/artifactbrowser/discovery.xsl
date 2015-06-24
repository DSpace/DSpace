<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<xsl:stylesheet
    xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
    xmlns:dri="http://di.tamu.edu/DRI/1.0/"
    xmlns:mets="http://www.loc.gov/METS/"
    xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
    xmlns:xlink="http://www.w3.org/TR/xlink/"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:xalan="http://xml.apache.org/xalan"
    xmlns:encoder="xalan://java.net.URLEncoder"
    xmlns:xmlui="xalan://ar.edu.unlp.sedici.dspace.xmlui.util.XSLTHelper"
    extension-element-prefixes="xmlui"
    exclude-result-prefixes="xalan encoder i18n dri mets dim  xlink xsl">

    <xsl:output indent="yes"/>

<!--
    These templates are devoted to rendering the search results for Discovery.
    Since Discovery uses hit highlighting separate templates are required !
-->


    <xsl:template match="dri:list[@type='dsolist']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <ul class="discovery-list-results container-fluid">
            <xsl:apply-templates select="*[not(name()='head')]" mode="dsoList"/>
        </ul>
    </xsl:template>


    <xsl:template match="dri:list/dri:list" mode="dsoList" priority="7">
<!--         <xsl:apply-templates select="dri:head"/> -->
<!--         <ul class="container-fluid"> -->
            <xsl:apply-templates select="*[not(name()='head')]" mode="dsoList"/>
<!--         </ul> -->
    </xsl:template>


    <xsl:template match="dri:list/dri:list/dri:list" mode="dsoList" priority="8">
        <li class="row artifact-info" >
            <!--
                Retrieve the type from our name, the name contains the following format:
                    {handle}:{metadata}
            -->
            <xsl:variable name="handle">
                <xsl:value-of select="substring-before(@n, ':')"/>
            </xsl:variable>
            <xsl:variable name="type">
                <xsl:value-of select="substring-after(@n, ':')"/>
            </xsl:variable>
            <xsl:variable name="externalMetadataURL">
                <xsl:text>cocoon://metadata/handle/</xsl:text>
                <xsl:value-of select="$handle"/>
                <xsl:text>/mets.xml</xsl:text>
                <!-- Since this is a summary only grab the descriptive metadata, and the thumbnails -->
                <xsl:text>?sections=dmdSec,fileSec&amp;fileGrpTypes=THUMBNAIL</xsl:text>
            </xsl:variable>


            <xsl:choose>
                <xsl:when test="$type='community'">
                    <xsl:call-template name="communitySummaryList">
                        <xsl:with-param name="handle">
                            <xsl:value-of select="$handle"/>
                        </xsl:with-param>
                        <xsl:with-param name="externalMetadataUrl">
                            <xsl:value-of select="$externalMetadataURL"/>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:when>
                <xsl:when test="$type='collection'">
                    <xsl:call-template name="collectionSummaryList">
                        <xsl:with-param name="handle">
                            <xsl:value-of select="$handle"/>
                        </xsl:with-param>
                        <xsl:with-param name="externalMetadataUrl">
                            <xsl:value-of select="$externalMetadataURL"/>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:when>
                <xsl:when test="$type='item'">
                    <xsl:call-template name="itemSummaryList">
                        <xsl:with-param name="handle">
                            <xsl:value-of select="$handle"/>
                        </xsl:with-param>
                        <xsl:with-param name="externalMetadataUrl">
                            <xsl:value-of select="$externalMetadataURL"/>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:when>
            </xsl:choose>
        </li>
    </xsl:template>

    <xsl:template name="communitySummaryList">
        <xsl:param name="handle"/>
        <xsl:param name="externalMetadataUrl"/>

        <xsl:variable name="metsDoc" select="document($externalMetadataUrl)"/>
  		<div class="col-md-1 icono-carpeta">
  			<i18n:text>xmlui.ArtifactBrowser.AdvancedSearch.type_location.comm</i18n:text>
		</div>
        <div class="artifact-title col-md-11">
            <a href="{$metsDoc/mets:METS/@OBJID}">
                <xsl:choose>
                    <xsl:when test="dri:list[@n=(concat($handle, ':dc.title')) and descendant::text()]">
                        <xsl:apply-templates select="dri:list[@n=(concat($handle, ':dc.title'))]/dri:item"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                    </xsl:otherwise>
                </xsl:choose>
            </a>
            <!--Display community strengths (item counts) if they exist-->
            <xsl:if test="string-length($metsDoc/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='format'][@qualifier='extent'][1]) &gt; 0">
                <xsl:text> [</xsl:text>
                <xsl:value-of
                        select="$metsDoc/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='format'][@qualifier='extent'][1]"/>
                <xsl:text>]</xsl:text>
            </xsl:if>
        </div>
    </xsl:template>

    <xsl:template name="collectionSummaryList">
        <xsl:param name="handle"/>
        <xsl:param name="externalMetadataUrl"/>

        <xsl:variable name="metsDoc" select="document($externalMetadataUrl)"/>
		<div class="col-md-1 icono-carpeta" >
  			<i18n:text>xmlui.ArtifactBrowser.AdvancedSearch.type_location.coll</i18n:text>
		</div>
        <div class="artifact-title col-md-11">
            <a href="{$metsDoc/mets:METS/@OBJID}">
                <xsl:choose>
                    <xsl:when test="dri:list[@n=(concat($handle, ':dc.title')) and descendant::text()]">
                        <xsl:apply-templates select="dri:list[@n=(concat($handle, ':dc.title'))]/dri:item"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                    </xsl:otherwise>
                </xsl:choose>
            </a>

        <!--Display collection strengths (item counts) if they exist-->
        <xsl:if test="string-length($metsDoc/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='format'][@qualifier='extent'][1]) &gt; 0">
            <xsl:text> [</xsl:text>
            <xsl:value-of
                    select="$metsDoc/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='format'][@qualifier='extent'][1]"/>
            <xsl:text>]</xsl:text>
        </xsl:if>
        <br/>
        <!-- Show parent of community/collection -->
        <xsl:if test="dri:list[@n=(concat($handle, ':dc.other')) and descendant::text()]">
        	<xsl:value-of select="dri:list[@n=(concat($handle, ':dc.other'))]/dri:item"/>
        </xsl:if>
        </div>


    </xsl:template>

    <xsl:template name="itemSummaryList">
        <xsl:param name="handle" />
        <xsl:param name="externalMetadataUrl" />
        <xsl:variable name="metsDoc" select="document($externalMetadataUrl)" />
        <!--Generates thumbnails (if present)-->
        <!--         <xsl:apply-templates select="$metsDoc/mets:METS/mets:fileSec" mode="artifact-preview"><xsl:with-param name="href" select="concat($context-path, '/handle/', $handle)"/></xsl:apply-templates> -->
        <!-- Símbolo utilizado para separar (en la visualización) multiples instancias de un metadato -->
        <xsl:variable name="separador">; </xsl:variable>
        <!-- Primera fila -->
           <div class="col-md-1" style="text-align:center">
            	<!-- dcterms-issued -->
            	
                <xsl:call-template name="renderDiscoveryField">
                    <xsl:with-param name="value">
                    	
                    	<xsl:choose>
	                        <xsl:when test="dri:list[@n=(concat($handle, ':dcterms.issued')) and descendant::text()]">
	                            <xsl:value-of select="substring(dri:list[@n=(concat($handle, ':dcterms.issued'))]/dri:item[position()=1]/text(), 1, 4)" />
	                        </xsl:when>
	                        <xsl:otherwise>
<!-- 	                        		Sin título -->
	                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-date</i18n:text>
	                        </xsl:otherwise>
	                    </xsl:choose>
                    </xsl:with-param>
                    <xsl:with-param name="classname" select="'dcterms-issued'"/>
                </xsl:call-template>
            </div>
            <div class="col-md-9">
            	<div class="row"><div class="col-md-12 artifact-title" >
            
                <!-- dc.title -->
                <xsl:call-template name="renderDiscoveryField">
                    <xsl:with-param name="href">
                    	<xsl:choose>
                        	<xsl:when test="$metsDoc/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/@withdrawn">
                            	<xsl:value-of select="$metsDoc/mets:METS/@OBJEDIT" />
                        	</xsl:when>
                        	<xsl:otherwise>
                            	<xsl:value-of select="concat($context-path, '/handle/', $handle)" />
                        	</xsl:otherwise>
                    	</xsl:choose>
                    </xsl:with-param>
                    <xsl:with-param name="value">
                    	<xsl:choose>
	                        <xsl:when test="dri:list[@n=(concat($handle, ':dc.title')) and descendant::text()]">
	                            <xsl:apply-templates select="dri:list[@n=(concat($handle, ':dc.title'))]/dri:item" />
	                            <xsl:if test="dri:list[@n=(concat($handle, ':dcterms.title.subtitle')) and descendant::text()]">
	                            	<xsl:text>: </xsl:text>
	                            	<xsl:apply-templates select="dri:list[@n=(concat($handle, ':dcterms.title.subtitle'))]/dri:item" />
	                            </xsl:if>
	                        </xsl:when>
	                        <xsl:otherwise>
<!-- 	                        		Sin título -->
	                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
	                        </xsl:otherwise>
	                    </xsl:choose>
                    </xsl:with-param>
                    <xsl:with-param name="classname" select="'dc-title'"/>
                </xsl:call-template>
                </div></div>
                <div class="row"><div class="col-md-12" >
                
	        	<!-- Segunda Fila -->
	        	<!-- dcterms.creator.(corporate|author|compilator|editor) -->
                <xsl:call-template name="renderDiscoveryField">
                    <xsl:with-param name="value">
                    	<xsl:choose>
                    		<xsl:when test="dri:list[(@n=(concat($handle, ':dcterms.creator.corporate')) or @n=(concat($handle, ':dcterms.creator.author')) or @n=(concat($handle, ':dcterms.creator.compilator')) or @n=(concat($handle, ':dcterms.creator.editor'))) and descendant::text()]">
								<xsl:for-each select="dri:list[(@n=(concat($handle, ':dcterms.creator.corporate')) or @n=(concat($handle, ':dcterms.creator.author')) or @n=(concat($handle, ':dcterms.creator.compilator')) or @n=(concat($handle, ':dcterms.creator.editor'))) and descendant::text()]">	
		                            <xsl:for-each select="dri:item">
		                            	<xsl:apply-templates select="." />; 
		                            </xsl:for-each>
				                </xsl:for-each>                    		
                    		</xsl:when>
                    		<xsl:otherwise>
                    			<i18n:text>xmlui.dri2xhtml.METS-1.0.no-author</i18n:text>
                    		</xsl:otherwise>
                    	</xsl:choose>
                    </xsl:with-param>
                    <xsl:with-param name="classname" select="'dcterms-creators'"/>
                </xsl:call-template>
                </div></div>
                <!-- dcterms.abstract -->
                <xsl:if test="dri:list[@n=(concat($handle, ':dcterms.abstract'))]/dri:item/dri:hi">
                	<div class="row"><div class="col-md-12">
                        <xsl:for-each select="dri:list[@n=(concat($handle, ':dcterms.abstract'))]/dri:item[string-length(dri:hi) > 0]">
                            <xsl:apply-templates select="."/>
                            <xsl:text>...</xsl:text>
                            <br/>
                        </xsl:for-each>
                    </div></div>
                </xsl:if>
                <xsl:if test="dri:list[@n=(concat($handle, ':fulltext'))]">
                    <div class="row"><div class="col-md-12">
                        <xsl:for-each select="dri:list[@n=(concat($handle, ':fulltext'))]/dri:item">
                            <xsl:apply-templates select="."/>
                            <xsl:text>...</xsl:text>
                            <br/>
                        </xsl:for-each>
                    </div></div>
                </xsl:if>
                <!-- Generate COinS with empty content per spec but force Cocoon to not create a minified tag  -->
                <span class="Z3988">
                    <xsl:attribute name="title">
                        <xsl:for-each select="$metsDoc/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim">
                            <xsl:call-template name="renderCOinS" />
                        </xsl:for-each>
                    </xsl:attribute>
                    ﻿
                    <!-- non-breaking space to force separating the end tag -->
                </span>
            </div>
            
            <div class="col-md-2">
            	<div class="row discovery-metadata-cic_lugarDeDesarrollo">
	            	<!-- cic.lugarDesarrollo -->
	                <xsl:if test="dri:list[@n=(concat($handle, ':cic.lugarDesarrollo')) and descendant::text()]">
	                    <xsl:call-template name="renderDiscoveryField">
	            	        <xsl:with-param name="value">
	                            <xsl:value-of select="xmlui:getLugarDesarrollo(dri:list[@n=(concat($handle, ':cic.lugarDesarrollo'))]/dri:item[position()=1]/text())" />
	        	            </xsl:with-param>
	    	                <xsl:with-param name="classname" select="'metadata-cic_lugarDeDesarrollo'"/>
		                </xsl:call-template>
	                </xsl:if>
                </div>
                
                <div class="row discovery-metadata-dc_type">
	            	<!-- dc.type -->
	            	<xsl:call-template name="renderDiscoveryField">
	            		<xsl:with-param name="value">
	                        <xsl:if test="dri:list[@n=(concat($handle, ':dc.type')) and descendant::text()]">
	                            <xsl:value-of select="dri:list[@n=(concat($handle, ':dc.type'))]/dri:item[position()=1]/text()" />
	                        </xsl:if>
	            		</xsl:with-param>
	            		<xsl:with-param name="classname" select="'metadata-dc_type'"/>
	            	</xsl:call-template>
            	</div>
            </div>
        
        
        	
    </xsl:template>

</xsl:stylesheet>
