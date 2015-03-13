<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml" xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/" 

    xmlns:xalan="http://xml.apache.org/xalan"
    xmlns:java="http://xml.apache.org/xalan/java" 
	xmlns:helper="ar.edu.unlp.sedici.dspace.util.HelperFunctions" 
	xmlns:confman="org.dspace.core.ConfigurationManager"
    xmlns:math="http://exslt.org/math"
    xmlns:sets="http://exslt.org/sets"
    xmlns:common="http://exslt.org/common"
    xmlns:dyn="http://exslt.org/dynamic"
    xmlns:str="http://exslt.org/strings"
    xmlns:regexp="http://exslt.org/regular-expressions"
    xmlns:xmlui="xalan://ar.edu.unlp.sedici.dspace.xmlui.util.XSLTHelper"
    extension-element-prefixes="str regexp xmlui math sets common dyn"
	xmlns="http://www.w3.org/1999/xhtml" exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc regexp str xmlui math sets common  dyn helper confman java xalan"
	>

<!-- 	<xsl:template match="//dri:div[@n='item-view']"> -->
<!-- 		 Shhhh -->
<!-- 	</xsl:template> -->
	
	<xsl:template name="render-metadata-values">
		<xsl:param name="separator">;</xsl:param>
		<xsl:param name="nodes"></xsl:param>
		<xsl:param name="anchor"></xsl:param>

		<xsl:for-each select="$nodes">
			<span>
				<xsl:if test="@language">
					<xsl:attribute name="xml:lang" ><xsl:value-of select="@language"/></xsl:attribute>
				</xsl:if>
				<xsl:choose>
					<xsl:when test="$anchor">
						<xsl:call-template name="build-anchor">
							<xsl:with-param name="a.href" select="@authority"/>
							<xsl:with-param name="a.value" select="text()"/>
						</xsl:call-template>
					</xsl:when>
					<xsl:otherwise>
						<xsl:copy-of select="text()"/>
					</xsl:otherwise>				
				</xsl:choose>
			</span>
			<xsl:if test="not(position()=last())">
	        	<xsl:value-of select="$separator" /> 
	        </xsl:if>
		</xsl:for-each>
	</xsl:template>
	
<!-- 	<xsl:template name="render-metadata-group"> -->
<!-- 		<xsl:param name="fields">dc.title</xsl:param>Comma separated list of metadata -->
<!-- 		<xsl:param name="separator">;</xsl:param> -->
<!-- 		<xsl:param name="filter"></xsl:param>Si tiene filtername es un link, sino no -->
<!-- 		<xsl:param name="label"></xsl:param> -->
<!-- 		<xsl:param name="context" select="." /> -->
<!-- 		<xsl:if test="$label"> -->
<!-- 			<div class="metadata-group-label"> -->
<!-- 				<xsl:copy-of select="$label" /> -->
<!-- 			</div> -->
<!-- 		</xsl:if> -->
		
<!-- 		<xsl:for-each select="str:split($fields,',')"> -->
<!-- 			<xsl:call-template name="render-metadata"> -->
<!-- 				<xsl:with-param name="field" select="."/> -->
<!-- 				<xsl:with-param name="context" select="$context" /> -->
<!-- 				<xsl:with-param name="separator" select="$separator"/> -->
<!-- 				<xsl:with-param name="filter" select="$filter"/>Si tiene filtername es un link, sino no -->
<!-- 				<xsl:with-param name="show_label"> -->
<!-- 					<xsl:if test="(last() = 1) and not($label)"> -->
<!-- 						<xsl:text>true</xsl:text> -->
<!-- 					</xsl:if> -->
<!-- 				</xsl:with-param> -->
<!-- 			</xsl:call-template> -->
<!-- 		</xsl:for-each> -->
<!-- 	</xsl:template> -->
	
	<!-- Imprime un metadato y sus valores -->
	<xsl:template name="render-metadata">
		<xsl:param name="field"></xsl:param>
		<xsl:param name="context" select="." />
		<xsl:param name="separator">; </xsl:param>
		<xsl:param name="is_linked_authority"></xsl:param><!-- Si viene en true, es un link, sino no -->
		<xsl:param name="show_label">true</xsl:param>
		<xsl:param name="container">div</xsl:param>
		<xsl:param name="null_message"></xsl:param>
		
		<xsl:variable name="mp" select="str:split($field,'.')" />
		<xsl:variable name="schema" select="$mp[1]"/>
		<xsl:variable name="element" select="$mp[position()=2]"/>
		<xsl:variable name="qualifier" >
			<xsl:if test="$mp[last()=3]">
					<xsl:value-of select="$mp[position()=3]/text()"/>
			</xsl:if>
		</xsl:variable>
		<xsl:variable name="fqmn" select="xmlui:replaceAll(string($field), '[\.\*]', '_')"/>
		
		<xsl:variable name="nodes" select="$context/dim:field[@mdschema=$schema and @element=$element and (($qualifier='' and not(@qualifier)) or ($qualifier!='' and (@qualifier=$qualifier or $qualifier='*')) ) ]"/>
		
		<xsl:if test="$nodes or $null_message">
			<xsl:element name="{$container}" >
				<xsl:attribute name="class"><xsl:value-of select="concat('metadata-', $fqmn)"/></xsl:attribute>
				<xsl:if test="$show_label ='true'">
					<span class="metadata-label">
						<i18n:text>xmlui.dri2xhtml.METS-1.0.item-<xsl:value-of select="$fqmn" /></i18n:text>: 
					</span>
				</xsl:if>
			
				<span class="metadata-values" >
					<xsl:choose>
						<xsl:when test="$nodes">
							<xsl:call-template name="render-metadata-values">
								<xsl:with-param name="separator" select="$separator"/>
								<xsl:with-param name="nodes" select="$nodes"/>
								<xsl:with-param name="anchor" select="$is_linked_authority"/>
							</xsl:call-template>
						</xsl:when>
						<xsl:when test="$null_message">
							<xsl:copy-of select="$null_message"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:text>Sin datos (no debería mostrarse)</xsl:text>
						</xsl:otherwise>
					</xsl:choose>
				</span>
			</xsl:element>
		</xsl:if>
	</xsl:template>
	
    <xsl:template name="itemSummaryView-DIM">
    	<xsl:for-each select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim">
	    	<div class="row item-head">
		    	<div class="col-md-12">
			    	<xsl:call-template name="render-metadata">
			    		<xsl:with-param name="field" select="'dc.type'" />
			    		<xsl:with-param name="show_label" select="'false'" />
			    	</xsl:call-template>
		    		<xsl:call-template name="render-metadata">
		    			<xsl:with-param name="field" select="'dc.title'" />
		    			<xsl:with-param name="show_label" select="'false'" />
						<xsl:with-param name="container" select="'h1'" />
		    			<xsl:with-param name="null_message"><i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text></xsl:with-param>
		    		</xsl:call-template>
		    		<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.title.subtitle'"/>
						<xsl:with-param name="show_label" select="false"/>
						<xsl:with-param name="container" select="'h4'"/>
		    		</xsl:call-template>
			    	<xsl:call-template name="render-metadata">
		    			<xsl:with-param name="field" select="'dcterms.creator.*'" />
		    			<xsl:with-param name="separator" select="''" />
		    			<xsl:with-param name="show_label" select="'false'" />
		    			<xsl:with-param name="null_message"><i18n:text>xmlui.dri2xhtml.METS-1.0.no-author</i18n:text></xsl:with-param>
		    		</xsl:call-template>
		    		
				</div>
	    	</div>
	    </xsl:for-each>
    	
    	<div class="row">
	    	<div class="col-md-9">
	    	
	    <!-- Generate the info about the item from the metadata section -->
		        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
		        mode="itemSummaryView-DIM" />
		    	
		    	
	    	</div>
	    	<div class="col-md-3">
	    	
	    		<!-- Advertencia de embargo (por ahora esta deshabilitado) -->
				<xsl:if test="has-embargo">
					<div class="alert alert-warning" role="alert">
						<i18n:text>xmlui.dri2xhtml.METS-1.0.item-dcterms_rights_embargoPeriod.msg</i18n:text>
						<xsl:call-template name="render-metadata">
							<xsl:with-param name="field" select="'dcterms.rights.embargo_period'" />
							<xsl:with-param name="show_label" select="'false'" />
						</xsl:call-template>
					</div>
				</xsl:if>
				
				<div class="item-preview">
				    <a href="#" class="thumbnail">
				      <img alt="Preview" />
				    </a>
				</div>

		        <!-- Generate the bitstream information from the file section -->
		        <xsl:choose>
		            <xsl:when test="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']/mets:file">
		            	<ul class="media-list item-file-list">
		                <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']/mets:file" />
		                </ul>
		            </xsl:when>
		            <xsl:otherwise>
		                <i18n:text>xmlui.dri2xhtml.METS-1.0.item-no-files</i18n:text>
		            </xsl:otherwise>
		        </xsl:choose>
		        
				<!-- Falta acomodar el campo dcterms.identifier.url -->
				<xsl:call-template name="render-metadata">
					<xsl:with-param name="field" select="'dcterms.identifier.url '" />
					<xsl:with-param name="container" select="'h4'" />
				</xsl:call-template>
					
		        
		        <!-- optional: Altmeric.com badge and PlumX widget -->
		        <xsl:if test='confman:getProperty("altmetrics", "altmetric.enabled") and ($identifier_doi or $identifier_handle)'>
		            <xsl:call-template name='impact-altmetric'/>
		        </xsl:if>
		        <xsl:if test='confman:getProperty("altmetrics", "plumx.enabled") and $identifier_doi'>
		            <xsl:call-template name='impact-plumx'/>
		        </xsl:if>
	    	</div>
    	</div>

	    <!-- Creative Commons Logo -->
        <div class="row">
        	<xsl:variable name="cc-uri">
				<xsl:copy-of select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='dcterms' and @element='license']/text()"/>
			</xsl:variable>
        	<div class="col-md-1">
		        <!-- Generate the Creative Commons license information from the file section (DSpace deposit license hidden by default)-->
				<!-- <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CC-LICENSE']"/> -->
				<xsl:call-template name="generate-CC-Anchor-Logo">
					<xsl:with-param name="cc-uri" select="$cc-uri"/>
				</xsl:call-template>
			</div>
			<div class="col-md-6">
				<i18n:text>xmlui.dri2xhtml.structural.cc-item-view-text</i18n:text>
				<i18n:text><xsl:value-of select="concat('xmlui.dri2xhtml.structural.cc-',xmlui:replaceAll(substring-after($cc-uri, 'http://creativecommons.org/licenses/'), '/', '-'))"/></i18n:text>
			</div>
	     </div>
    </xsl:template>
	
	<xsl:template match="mets:file" priority="10">
		<li class="media">
			<xsl:variable name="file_url" select="mets:FLocat[@LOCTYPE='URL']/@xlink:href" />
			<a class="media-left thumbnail_file" href="{$file_url}">
				<xsl:variable name="thumbnail_file" select="../../mets:fileGrp[@USE='THUMBNAIL']/mets:file[@GROUPID=current()/@GROUPID]"/>
				<xsl:choose>
					  <xsl:when test="$thumbnail_file">
	                           <img alt="Thumbnail">
	                                <xsl:attribute name="src">
	                                    <xsl:value-of select="$thumbnail_file/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
	                                </xsl:attribute>
	                            </img>
	                        </xsl:when>
					<xsl:otherwise>
						<img alt="Icon" src="{concat($theme-path, '/images/mime.png')}" />
					</xsl:otherwise>
				</xsl:choose>
				<xsl:if
					test="contains(mets:FLocat[@LOCTYPE='URL']/@xlink:href,'isAllowed=n')">
					<img>
						<xsl:attribute name="src">
		                                <xsl:value-of select="$context-path" />
		                                <xsl:text>/static/icons/lock24.png</xsl:text>
		                            </xsl:attribute>
						<xsl:attribute name="alt">xmlui.dri2xhtml.METS-1.0.blocked</xsl:attribute>
						<xsl:attribute name="attr"
							namespace="http://apache.org/cocoon/i18n/2.1">alt</xsl:attribute>
					</img>
				</xsl:if>
			</a>
			<div class="media-body">
				<h4 class="media-heading"><a href="{$file_url}">
			<xsl:value-of select="mets:FLocat/@xlink:title" /></a></h4>
				<p>
					<xsl:value-of select="mets:FLocat/@xlink:label" /> 
					<span> 
						(<xsl:choose>
							<xsl:when test="@SIZE &lt; 1024">
								<xsl:value-of select="@SIZE" />
								<i18n:text>xmlui.dri2xhtml.METS-1.0.size-bytes</i18n:text>
							</xsl:when>
							<xsl:when test="@SIZE &lt; 1024 * 1024">
								<xsl:value-of select="substring(string(@SIZE div 1024),1,5)" />
								<i18n:text>xmlui.dri2xhtml.METS-1.0.size-kilobytes</i18n:text>
							</xsl:when>
							<xsl:when test="@SIZE &lt; 1024 * 1024 * 1024">
								<xsl:value-of select="substring(string(@SIZE div (1024 * 1024)),1,5)" />
								<i18n:text>xmlui.dri2xhtml.METS-1.0.size-megabytes</i18n:text>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of
									select="substring(string(@SIZE div (1024 * 1024 * 1024)),1,5)" />
								<i18n:text>xmlui.dri2xhtml.METS-1.0.size-gigabytes</i18n:text>
							</xsl:otherwise>
						</xsl:choose>)
					</span>
				</p>
			</div>
		</li>
	</xsl:template>
	
	 <xsl:template match="dim:dim" mode="itemSummaryView-DIM">
       	
       	<div class="row">
       		<div class="col-md-12">
       		<xsl:call-template name="render-metadata">
				<xsl:with-param name="field" select="'dcterms.abstract'" />
				<xsl:with-param name="separator" select="''" />
			</xsl:call-template>
      		</div>
      	</div>
      	
	    <div class="row">
	    
	
		   	<div class="col-md-6">
		   		<h3><i18n:text>xmlui.ArtifactBrowser.ItemViewer.general_info</i18n:text></h3>
				<ul class="list-unstyled">
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.alternative'" />
						<xsl:with-param name="container" select="'li'" />
				    </xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'cic.lugarDesarrollo'" />
						<xsl:with-param name="container" select="'li'" />
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.publisher'" />
						<xsl:with-param name="container" select="'li'" />
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.subject.materia'" />
						<xsl:with-param name="container" select="'li'" />
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.subject'" />
						<xsl:with-param name="container" select="'li'" />
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.spatial'" />
						<xsl:with-param name="container" select="'li'" />
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.language'" />
						<xsl:with-param name="container" select="'li'" />
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.extent'" />
						<xsl:with-param name="container" select="'li'" />
					</xsl:call-template>
				</ul>

		   	</div>
		   	
		   	<div class="col-md-6">
		   		<h3><i18n:text>xmlui.ArtifactBrowser.ItemViewer.specific_info</i18n:text></h3>
				<ul class="list-unstyled">
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.title.investigacion'" />
						<xsl:with-param name="container" select="'li'" />
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.subject.areas'" />
						<xsl:with-param name="container" select="'li'" />
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.contributor.director'" />
						<xsl:with-param name="container" select="'li'" />
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'cic.thesis.degree'" />
						<xsl:with-param name="container" select="'li'" />
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'cic.thesis.grantor'" />
						<xsl:with-param name="container" select="'li'" />
					</xsl:call-template>
				</ul>
				
				<ul class="list-unstyled">
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.isPartOf.item'" />
						<xsl:with-param name="container" select="'li'" />
						<xsl:with-param name="is_linked_authority" select="'true'"/>
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.isPartOf.issue'" />
						<xsl:with-param name="container" select="'li'" />
						<xsl:with-param name="is_linked_authority" select="'true'"/>
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.isPartOf.series'" />
						<xsl:with-param name="container" select="'li'" />
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.identifier.isbn'" />
						<xsl:with-param name="container" select="'li'" />
					</xsl:call-template>
	
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.relation'" />
						<xsl:with-param name="container" select="'li'" />
						<xsl:with-param name="is_linked_authority" select="'true'"/>
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.hasPart'" />
						<xsl:with-param name="container" select="'li'" />
						<xsl:with-param name="is_linked_authority" select="'true'"/>
					</xsl:call-template>
					<xsl:call-template name="render-metadata">
						<xsl:with-param name="field" select="'dcterms.isVersionOf'" />
						<xsl:with-param name="container" select="'li'" />
						<xsl:with-param name="is_linked_authority" select="'true'"/>
					</xsl:call-template>
				</ul>
		   	</div>
		   	
		   	
    	</div>
			<div class="row">
				<div class="col-md-12">
					<h3><i18n:text>xmlui.ArtifactBrowser.ItemViewer.other_info</i18n:text></h3>
					<ul class="list-unstyled">
						<xsl:call-template name="render-metadata">
							<xsl:with-param name="field" select="'dcterms.description'" />
							<xsl:with-param name="container" select="'li'" />
						</xsl:call-template>
						<xsl:call-template name="render-metadata">
							<xsl:with-param name="field" select="'dc.date.available'" />
							<xsl:with-param name="container" select="'li'" />
						</xsl:call-template>
						<xsl:call-template name="render-metadata">
							<xsl:with-param name="field" select="'dc.date.accessioned'" />
							<xsl:with-param name="container" select="'li'" />
						</xsl:call-template>
						<xsl:call-template name="render-metadata">
							<xsl:with-param name="field" select="'dcterms.identifier.other'" />
							<xsl:with-param name="container" select="'li'" />
						</xsl:call-template>
						<xsl:call-template name="render-metadata">
							<xsl:with-param name="field" select="'dc.identifier.uri'" />
							<xsl:with-param name="container" select="'li'" />
						</xsl:call-template>
						<xsl:call-template name="render-metadata">
							<xsl:with-param name="field" select="'dcterms.license'" />
							<xsl:with-param name="container" select="'li'" />
						</xsl:call-template>
					
					</ul>
				</div>
			</div>
			
			<!-- Show full link -->
	        <div class="row ds-paragraph item-view-toggle item-view-toggle-bottom">
	            <div class="col-md-12">
	            	<a>
	                	<xsl:attribute name="href"><xsl:value-of select="$ds_item_view_toggle_url"/></xsl:attribute>
	                	<i18n:text>xmlui.ArtifactBrowser.ItemViewer.show_full</i18n:text>
	            	</a>
	            </div>
	        </div>
    	
    	
    	
        <span class="Z3988">
            <xsl:attribute name="title">
                <xsl:call-template name="renderCOinS"/>
            </xsl:attribute>
            &#xFEFF; <!-- non-breaking space to force separating the end tag -->
        </span>
        
    </xsl:template>
    
</xsl:stylesheet>