<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<!--
    This stylesheet contains helper templates for things like i18n and standard attributes.

    Author: art.lowel at atmire.com
    Author: lieven.droogmans at atmire.com
    Author: ben at atmire.com
    Author: Alexey Maslov

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
	xmlns:exslt="http://exslt.org/common"
	xmlns="http://www.w3.org/1999/xhtml"
	xmlns:ex="ar.edu.unlp.sedici.xmlui.xsl.XslExtensions"
	exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc exslt"
	xmlns:java="http://xml.apache.org/xalan/java">

    <xsl:output indent="yes"/>

    <!--added classes to differentiate between collections, communities and items-->
    <xsl:template match="dri:reference" mode="summaryList">
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!-- Since this is a summary only grab the descriptive metadata, and the thumbnails -->
           <!-- <xsl:text>?sections=dmdSec,fileSec&amp;fileGrpTypes=THUMBNAIL</xsl:text> -->
            <!-- An example of requesting a specific metadata standard (MODS and QDC crosswalks only work for items)->
            <xsl:if test="@type='DSpace Item'">
                <xsl:text>&amp;dmdTypes=DC</xsl:text>
            </xsl:if>-->
        </xsl:variable>
       
        <li>
            <xsl:attribute name="class">
                <xsl:text>ds-artifact-item </xsl:text>
                <xsl:choose>
                    <xsl:when test="contains(@type, 'Community')">
                        <xsl:text>community </xsl:text>
                    </xsl:when>
                    <xsl:when test="contains(@type, 'Collection')">
                        <xsl:text>collection </xsl:text>
                    </xsl:when>
                </xsl:choose>
                <xsl:choose>
                    <xsl:when test="position() mod 2 = 0">even</xsl:when>
                    <xsl:otherwise>odd</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:apply-templates select="document($externalMetadataURL)" mode="summaryList"/>
            <xsl:apply-templates />
        </li>
    </xsl:template>
	<xsl:template match="/dri:document/dri:body/dri:div[@id='aspect.submission.StepTransformer.div.submit-upload' and (dri:list[@id='aspect.submission.StepTransformer.list.submit-upload-new']) ]" >
		<xsl:apply-templates select="." mode="ordered">
			<xsl:with-param name="elements">
	     		<xsl:copy-of select="dri:list[@id='aspect.submission.StepTransformer.list.submit-progress']"/>
	     		<i18n:text>xmlui.Submission.submit.UploadStep.infoEmbargo</i18n:text>
	     		<xsl:copy-of select="dri:table[@id='aspect.submission.StepTransformer.table.submit-upload-summary']"/>
	     		<xsl:copy-of select="dri:list[@id='aspect.submission.StepTransformer.list.submit-upload-new']"/>
	     		<xsl:copy-of select="dri:list[@id='aspect.submission.StepTransformer.list.submit-upload-new-part2']"/>
	     		
	     		
			</xsl:with-param>
		</xsl:apply-templates>
	</xsl:template>
	<xsl:template match="/dri:document/dri:body/dri:div[@id='aspect.xmlworkflow.WorkflowTransformer.div.perform-task']" >
		<xsl:apply-templates select="." mode="ordered">
			<xsl:with-param name="elements">
		  		<xsl:copy-of select="dri:table[@id='aspect.xmlworkflow.WorkflowTransformer.table.workflow-actions']"/>
	     		<xsl:copy-of select="dri:referenceSet[@id='aspect.xmlworkflow.WorkflowTransformer.referenceSet.narf']"/>
	     		<xsl:copy-of select="dri:p[@id='aspect.xmlworkflow.WorkflowTransformer.p.hidden-fields']"/>
	     		<xsl:copy-of select="dri:p/dri:field[@id='aspect.xmlworkflow.WorkflowTransformer.field.submit_full_item_info']"/>
	    		<xsl:copy-of select="dri:referenceSet[@id='ar.edu.unlp.sedici.aspect.collectionViewer.CollectionViewer.referenceSet.community-view-root']"/>
	  		    <xsl:copy-of select="dri:div[@id='aspect.xmlworkflow.WorkflowTransformer.div.general-message']"/>
	    		<xsl:copy-of select="dri:list[@id='aspect.xmlworkflow.WorkflowTransformer.list.delete-workflow']"/>
	    		<xsl:copy-of select="dri:list[@id='aspect.xmlworkflow.WorkflowTransformer.list.reject-workflow']"/>
	  		</xsl:with-param>
		</xsl:apply-templates>
	</xsl:template>


    <!-- Interactive divs get turned into forms. The priority attribute on the template itself
        signifies that this template should be executed if both it and the one above match the
        same element (namely, the div element).

        Strictly speaking, XSL should be smart enough to realize that since one template is general
        and other more specific (matching for a tag and an attribute), it should apply the more
        specific once is it encounters a div with the matching attribute. However, the way this
        decision is made depends on the implementation of the XSL parser is not always consistent.
        For that reason explicit priorities are a safer, if perhaps redundant, alternative. -->
    <xsl:template match="dri:div[@interactive='yes']" mode="ordered">
    	<xsl:param name="elements"/>
    
        <xsl:apply-templates select="dri:head"/>
        <xsl:apply-templates select="@pagination">
            <xsl:with-param name="position">top</xsl:with-param>
        </xsl:apply-templates>
        <form>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-interactive-div</xsl:with-param>
            </xsl:call-template>
            <xsl:attribute name="action"><xsl:value-of select="@action"/></xsl:attribute>
            <xsl:attribute name="method"><xsl:value-of select="@method"/></xsl:attribute>
            <xsl:if test="@method='multipart'">
                <xsl:attribute name="method">post</xsl:attribute>
                <xsl:attribute name="enctype">multipart/form-data</xsl:attribute>
            </xsl:if>
            <xsl:attribute name="onsubmit">javascript:tSubmit(this);</xsl:attribute>
                        <!--For Item Submission process, disable ability to submit a form by pressing 'Enter'-->
                        <xsl:if test="starts-with(@n,'submit')">
                                <xsl:attribute name="onkeydown">javascript:return disableEnterKey(event);</xsl:attribute>
            </xsl:if>

            <xsl:choose>
            	<xsl:when test="$elements">
            		<xsl:apply-templates select="exslt:node-set($elements)"/>
            	</xsl:when>
            	<xsl:otherwise>
            		<xsl:apply-templates select="*[not(name()='head')]"/>
            	</xsl:otherwise>
            </xsl:choose>
				
        </form>
        <!-- JS to scroll form to DIV parent of "Add" button if jump-to -->
        <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='page'][@qualifier='jumpTo']">
          <script type="text/javascript">
            <xsl:text>var button = document.getElementById('</xsl:text>
            <xsl:value-of select="translate(@id,'.','_')"/>
            <xsl:text>').elements['</xsl:text>
            <xsl:value-of select="concat('submit_',/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='page'][@qualifier='jumpTo'],'_add')"/>
            <xsl:text>'];</xsl:text>
            <xsl:text>
                      if (button != null) {
                        var n = button.parentNode;
                        for (; n != null; n = n.parentNode) {
                            if (n.tagName == 'DIV') {
                              n.scrollIntoView(false);
                              break;
                           }
                        }
                      }
            </xsl:text>
          </script>
        </xsl:if>
        <xsl:apply-templates select="@pagination">
            <xsl:with-param name="position">bottom</xsl:with-param>
        </xsl:apply-templates>
    </xsl:template>

	<xsl:template name="buildHomeSearch">
         <div id='home_search'>
         	 <h2><i18n:text>sedici.home.buscar_material.title</i18n:text></h2>
         	 <p>
         	 	<i18n:text>sedici.home.buscar_material.info_pre</i18n:text>
         	 	<span class="resource_count">55000</span>
         	 	<i18n:text>sedici.home.buscar_material.info_post</i18n:text>
         	 </p>
	     <form>
          <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-interactive-div</xsl:with-param>
            </xsl:call-template>
            <xsl:attribute name="action"><xsl:value-of select="$context-path"/>/discover</xsl:attribute>
            <xsl:attribute name="method">GET</xsl:attribute>
            <xsl:attribute name="onsubmit">javascript:tSubmit(this);</xsl:attribute>
            <p class="ds-paragraph">
		 		<input id="aspect_discovery_SiteViewer_field_query" class="ds-text-field" type="text" value="" name="query" />
				<input id="aspect_discovery_SiteViewer_field_submit" class="ds-button-field" type="submit" value="" name="submit" />
		    </p>
        </form>
	     </div>
	</xsl:template>

	<xsl:template name="buildHomeAutoarchivo">
		 <div id="home_autoarchivo">
        	<h2><i18n:text>sedici.home.subir_material.title</i18n:text></h2>
        	<p><i18n:text>sedici.home.subir_material.info</i18n:text></p>
         	<div>
			 	<a>
		 			<xsl:attribute name="href">
		 				<xsl:value-of select="$context-path"/>
						<xsl:text>/pages/comoAgregarTrabajos</xsl:text>
		 			</xsl:attribute>
			 		<img title="sedici.home.subir_material.linktext" i18n:attr="title">
			            <xsl:attribute name="src">
			                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
			                <xsl:text>/themes/</xsl:text>
			                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
			                <xsl:text>/images/autoarchivo_home.png</xsl:text>
			            </xsl:attribute>&#160;
			 		</img>
			 	</a>
		 	</div>
			<div>
				<a>
					<xsl:attribute name="href">
						<xsl:value-of select="$context-path"/>
						<xsl:text>/pages/ayudaInvestigadores</xsl:text>
					</xsl:attribute>
					<img title="sedici.home.ayuda_investigadores.linktext" i18n:attr="title">
			            <xsl:attribute name="src">
			                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
			                <xsl:text>/themes/</xsl:text>
			                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
			                <xsl:text>/images/ayuda_investigadores_home.png</xsl:text>
			            </xsl:attribute>&#160;
					</img>
				</a>
			</div>
		 </div>
	</xsl:template>
	
	<xsl:template match="dri:list[@id='aspect.browseArtifacts.CommunityBrowse.list.community-browse' or @id='aspect.browseArtifacts.CollectionBrowse.list.collection-browse']" >
		<xsl:variable name="defaultDiscoveryQuery">
			<xsl:text>?sort_by=dc.date.accessioned_dt&amp;order=desc</xsl:text>
		</xsl:variable>
		<xsl:variable name="URL">
          	<xsl:value-of select="$context-path"/>
			<xsl:text>/</xsl:text>
			<xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='URI']"/>
        </xsl:variable>
       
		<xsl:apply-templates select="dri:head" />
		<ul id="aspect_artifactbrowser_CommunityViewer_list_community-browse" class="ds-simple-list community-browse" xmlns="http://di.tamu.edu/DRI/1.0/" xmlns:i18n="http://apache.org/cocoon/i18n/2.1">
			<li>
				<a>
			 		<xsl:attribute name="href">
			 			<xsl:value-of select="$URL"/>
						<xsl:text>/discover</xsl:text>
						<xsl:value-of select="$defaultDiscoveryQuery"/>
				 	</xsl:attribute>
				 	<i18n:text>xmlui.ArtifactBrowser.CommunityViewer.all_of_dspace</i18n:text>
				 </a>
			</li>
			<xsl:apply-templates select="dri:item" mode="community-browse"/>
			
		</ul>	    
   </xsl:template>
   
   <xsl:template match="dri:item" mode="community-browse">
		<li>
			<xsl:apply-templates/>
		</li>
	</xsl:template>
	
	<xsl:template match="dri:document/dri:options/dri:list[@id='aspect.discovery.Navigation.list.discovery']">
		 <xsl:choose>
            <xsl:when test="contains(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='URI'], 'discover')">
		    	<h1 class="ds-option-set-head"><i18n:text>xmlui.discovery.AbstractFiltersTransformer.filters.head</i18n:text></h1> 
		    </xsl:when>
		    <xsl:otherwise>
				<h1 class="ds-option-set-head"><i18n:text>xmlui.discovery.AbstractFiltersTransformer.filters.head_collection</i18n:text></h1> 
			</xsl:otherwise>
        </xsl:choose>
		<div id="aspect_discovery_Navigation_list_discovery" class="ds-option-set">
			<ul class="ds-options-list">
				<xsl:apply-templates select="dri:list" mode="local-list"/>
			</ul>
		</div>	
	</xsl:template>
	
	<xsl:template match="dri:div[@id='aspect.discovery.SimpleSearch.div.search']/dri:head">
		 <xsl:variable name="matchesCondition">
         	<xsl:value-of select="ex:matches(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='URI'], 'handle/.*/.*')"/>
	     </xsl:variable>

		 <xsl:if test="$matchesCondition = 'true'">
		    	<xsl:variable name="URL_mets">
          			<xsl:text>cocoon:/</xsl:text>
					<xsl:text>/metadata/handle/</xsl:text>
					<xsl:value-of select="substring-after(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='focus'][@qualifier='container'], 'hdl:')"/>
					<xsl:text>/mets.xml</xsl:text>
        		</xsl:variable>
        		<xsl:apply-templates select="document($URL_mets)" mode="label-collection"/>
		 </xsl:if>

		 <h1>
		 	<xsl:attribute name="class">
	 			<xsl:if test="$matchesCondition = 'true'">
	 				<xsl:text>search-head </xsl:text>
	 			</xsl:if>
		 		<xsl:text>ds-div-head</xsl:text>
		 	</xsl:attribute>
		 	<i18n:text>xmlui.ArtifactBrowser.SimpleSearch.head</i18n:text>
		 </h1> 
		 
	</xsl:template>
	
	
	
    <xsl:template match="/mets:METS" mode="label-collection">
 	  <h1 class="ds-div-head selectedCollection"><xsl:value-of select="mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='title']"/></h1>
	</xsl:template>
	

	
	
	<xsl:template match="dri:list" mode="local-list">
		<li><xsl:apply-templates select="." mode="nested"/></li>
	</xsl:template>

	
	<!-- Template de paginacion -->
    <xsl:template match="@pagination">
        <xsl:param name="position"/>
        <xsl:choose>
            <xsl:when test=". = 'simple'">
                <div class="pagination clearfix {$position}">
 
					<xsl:call-template name="pagination-info"/>
					
					<xsl:if test="parent::node()/@previousPage or parent::node()/@nextPage">
	                    <ul class="pagination-links">
	                        <xsl:if test="parent::node()/@previousPage">
		                        <li class="previous-page-link">
	                                <a>
	                                    <xsl:attribute name="href">
	                                        <xsl:value-of select="parent::node()/@previousPage"/>
	                                    </xsl:attribute>
	                                    <i18n:text>xmlui.dri2xhtml.structural.pagination-previous</i18n:text>
	                                </a>
		                        </li>
	                        </xsl:if>
	                        <xsl:if test="parent::node()/@nextPage">
	    	                    <li class="next-page-link">
	                                <a>
	                                    <xsl:attribute name="href">
	                                        <xsl:value-of select="parent::node()/@nextPage"/>
	                                    </xsl:attribute>
	                                    <i18n:text>xmlui.dri2xhtml.structural.pagination-next</i18n:text>
	                                </a>
	 	                       </li>
	                        </xsl:if>
	                    </ul>
                    </xsl:if>
                </div>
            </xsl:when>
            <xsl:when test=". = 'masked'">
                <div class="pagination-masked clearfix {$position}">
                	
                	<xsl:call-template name="pagination-info"/>
                	
                    <ul class="pagination-links">
                        <xsl:if test="not(parent::node()/@firstItemIndex = 0 or parent::node()/@firstItemIndex = 1)">
                            <li class="previous-page-link">
                                <a>
                                    <xsl:attribute name="href">
                                        <xsl:value-of select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
                                        <xsl:value-of select="parent::node()/@currentPage - 1"/>
                                        <xsl:value-of select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
                                    </xsl:attribute>
                                    <i18n:text>xmlui.dri2xhtml.structural.pagination-previous</i18n:text>
                                </a>
                            </li>
                        </xsl:if>
                        <xsl:if test="(parent::node()/@currentPage - 4) &gt; 0">
                            <li class="first-page-link">
                                <a>
                                    <xsl:attribute name="href">
                                        <xsl:value-of select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
                                        <xsl:text>1</xsl:text>
                                        <xsl:value-of select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
                                    </xsl:attribute>
                                    <xsl:text>1</xsl:text>
                                </a>
                            </li>
                        </xsl:if>
                        <xsl:if test="(parent::node()/@currentPage - 4) &gt; 1">
                            <li class="page-dots">
                            	<xsl:text> . . . </xsl:text>
                            </li>
                        </xsl:if>
                        <xsl:call-template name="offset-link">
                            <xsl:with-param name="pageOffset">-3</xsl:with-param>
                        </xsl:call-template>
                        <xsl:call-template name="offset-link">
                            <xsl:with-param name="pageOffset">-2</xsl:with-param>
                        </xsl:call-template>
                        <xsl:call-template name="offset-link">
                            <xsl:with-param name="pageOffset">-1</xsl:with-param>
                        </xsl:call-template>
                        <xsl:call-template name="offset-link">
                            <xsl:with-param name="pageOffset">0</xsl:with-param>
                        </xsl:call-template>
                        <xsl:call-template name="offset-link">
                            <xsl:with-param name="pageOffset">1</xsl:with-param>
                        </xsl:call-template>
                        <xsl:call-template name="offset-link">
                            <xsl:with-param name="pageOffset">2</xsl:with-param>
                        </xsl:call-template>
                        <xsl:call-template name="offset-link">
                            <xsl:with-param name="pageOffset">3</xsl:with-param>
                        </xsl:call-template>
                        <xsl:if test="(parent::node()/@currentPage + 4) &lt; ((parent::node()/@pagesTotal))">
                            <li class="page-dots">
                                <xsl:text>. . .</xsl:text>
                            </li>
                        </xsl:if>
                        <xsl:if test="(parent::node()/@currentPage + 4) &lt;= (parent::node()/@pagesTotal)">
                            <li class="last-page-link">
                                <a>
                                    <xsl:attribute name="href">
                                        <xsl:value-of select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
                                        <xsl:value-of select="parent::node()/@pagesTotal"/>
                                        <xsl:value-of select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
                                    </xsl:attribute>
                                    <xsl:value-of select="parent::node()/@pagesTotal"/>
                                </a>
                            </li>
                        </xsl:if>
                        <xsl:if test="not(parent::node()/@lastItemIndex = parent::node()/@itemsTotal)">
                            <li class="next-page-link">
                                <a>
                                    <xsl:attribute name="href">
                                        <xsl:value-of select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
                                        <xsl:value-of select="parent::node()/@currentPage + 1"/>
                                        <xsl:value-of select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
                                    </xsl:attribute>
                                    <i18n:text>xmlui.dri2xhtml.structural.pagination-next</i18n:text>
                                </a>
                            </li>
                        </xsl:if>
                    </ul>
                </div>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

	<xsl:template name="pagination-info">
	    <p class="pagination-info">
	        <i18n:translate>
	            <xsl:choose>
	                <xsl:when test="parent::node()/@itemsTotal = -1">
	                    <i18n:text>xmlui.dri2xhtml.structural.pagination-info.nototal</i18n:text>
	                </xsl:when>
	                <xsl:otherwise>
	                    <i18n:text>xmlui.dri2xhtml.structural.pagination-info</i18n:text>
	                </xsl:otherwise>
	            </xsl:choose>
	            <i18n:param><xsl:value-of select="parent::node()/@firstItemIndex"/></i18n:param>
	            <i18n:param><xsl:value-of select="parent::node()/@lastItemIndex"/></i18n:param>
	            <i18n:param><xsl:value-of select="parent::node()/@itemsTotal"/></i18n:param>
	        </i18n:translate>
	    </p>
	</xsl:template>
	
	
	<xsl:template name="filterHTMLTags">
		<!-- It is the node that will be processed. -->
   		<xsl:param name="targetNode"/>
    	<xsl:choose>
    		<!-- If there isn't a text node to process, then will proceed to copy the tree of nodes.  -->
    		<xsl:when test="$targetNode/*">
    			<xsl:copy-of select="$targetNode/*"/>
    		</xsl:when>
    		<!-- Else, there is a text node.-->
    		<xsl:when test="$targetNode/text()">
    			<xsl:call-template name="processTextNodes">
	    			<xsl:with-param name="textNode" select="$targetNode"/>
	    		</xsl:call-template>
    		</xsl:when>
    	</xsl:choose>
	</xsl:template>
	
	<xsl:template name="processTextNodes">
    	<!-- It is a text node that will be processed -->
    	<xsl:param name="textNode"/>
    	<xsl:if test="$textNode">
    		<!-- Method of a Java class that use a regular expression to replace the HTML tags of a text. -->
    		<xsl:copy-of select="java:ar.edu.unlp.sedici.xmlui.xsl.XslExtensions.replace($textNode,'&lt;/?(i|sub|sup)&gt;','')"/>
    	</xsl:if>
    </xsl:template>
    
    <!-- Encode the Discovery filters to generate a valid URL for its use by Discovery Simple-Search -->
    <xsl:template name="encodeDiscoveryFilter">
    	<xsl:param name="filterValue"/>
    	<xsl:if test="$filterValue">
    		<xsl:value-of select="ex:replace(ex:encodeURL($filterValue),'\+','\\+')"/>
    	</xsl:if>
    </xsl:template>
    
    <!-- Parsea los metadatos guardados en el metadato 'dc.description' de una comunidad/colección. Estos metadatos fueron guardados
    	utilizando el script metadataGenerator.js -->
    <xsl:template name="parseCommunityCollectionMetadata">
    	<xsl:param name="node"/>
    	<!-- Debajo del elemento 'dc.description' hay un string. Para manipularlo hay que convertirlo hay 'node-set' -->
		<xsl:variable name="html_nodes"><xsl:copy-of select="$node"/></xsl:variable>
        <xsl:for-each select="exslt:node-set($html_nodes)/*">
			<div>
				<xsl:attribute name="class"><xsl:value-of select="./@class"/></xsl:attribute>	
				<!-- Seguimos parseando el string y lo convertimos en otro conjunto de nodos -->
				<xsl:for-each select="exslt:node-set(.)/*">
					<xsl:choose>
	           			<xsl:when test="./@class='value'">
	           				<span>
	           					<xsl:attribute name="class"><xsl:value-of select="'value'"/></xsl:attribute>
                                <!-- Si el <span class="value"> tiene más de un hijo, entonces copiamos los nodos que no son text(), evitando asi perder elementos como el 
								<a> en un metadato del tipo link. Los nodos del tipo 'text' son afectados por el "disable-output-escaping" -->
           						<xsl:for-each select="./node()">
           							<xsl:choose>
           								<xsl:when test="self::text()">
           									<xsl:value-of select="." disable-output-escaping="yes"/>
           								</xsl:when>
           								<xsl:otherwise>
           									<xsl:copy-of select="."/>
           								</xsl:otherwise>
           							</xsl:choose>
           						</xsl:for-each>
	           				</span>
	           			</xsl:when>
	           			<xsl:otherwise>
	           				<xsl:copy-of select="."/>
	           			</xsl:otherwise>
	           		</xsl:choose>
				</xsl:for-each>
			</div>		
		</xsl:for-each>
    </xsl:template>
    
    <xsl:template name="build-anchor">
		<xsl:param name="a.href">/</xsl:param>
		<xsl:param name="a.value" select="$a.href"/>
		<xsl:param name="a.class"></xsl:param>
		<xsl:param name="img.src"></xsl:param>
		<xsl:param name="img.alt"></xsl:param>
		<xsl:param name="img.class"></xsl:param>
		<a>
			<xsl:attribute name="href">
				<xsl:if test="starts-with($a.href, 'http://') or starts-with($a.href, 'https://')">
					<xsl:value-of select="$a.href"/>
				</xsl:if>
				<xsl:if test="not(starts-with($a.href, 'http://') or starts-with($a.href, 'https://'))">
					<xsl:call-template name="print-path">
						<xsl:with-param name="path" select="$a.href"/>
					</xsl:call-template>
				</xsl:if>
			</xsl:attribute>
			<xsl:if test="$a.class">
				<xsl:attribute name="class">
					<xsl:copy-of select="$a.class"/>
				</xsl:attribute>
			</xsl:if>
			<xsl:if test="$img.src">
				<xsl:call-template name="build-img">	
					<xsl:with-param name="img.src" select="$img.src"/>
					<xsl:with-param name="img.alt" select="$img.alt"/>
					<xsl:with-param name="img.class" select="$img.class"/>
				</xsl:call-template>
			</xsl:if>
			<xsl:if test="not($img.src) or ($a.value != $a.href)">
				<xsl:copy-of select="$a.value"/>
			</xsl:if>
		</a>
	</xsl:template>
	
	<xsl:template name="build-img">
		<xsl:param name="img.src"></xsl:param>
		<xsl:param name="img.alt">image</xsl:param>
		<xsl:param name="img.class"></xsl:param>
		<img alt="{$img.alt}" class="{$img.class}">
			<xsl:attribute name="src">
				<xsl:choose>
					<xsl:when test="starts-with($img.src,'http') or starts-with($img.src,'https')">
						<xsl:value-of select="$img.src"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:call-template name="print-theme-path">
							<xsl:with-param name="path" select="$img.src"/>
						</xsl:call-template>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:attribute>
		</img>
	</xsl:template>
	
	<!-- Imprime la ruta absoluta al recurso indicado con el parámetro path -->
   	<xsl:template name="print-path">
   		<xsl:param name="path">/</xsl:param>
	
		<xsl:if test="not(starts-with($path, $context-path))">
			<!-- Imprimo el context path si la URL no es absolta -->
			<xsl:value-of select="$context-path" />
			
		</xsl:if>
		<xsl:if test="not(starts-with($path, '/'))">
				<xsl:text>/</xsl:text>
			</xsl:if>
		<xsl:value-of select="$path" />
	</xsl:template>
	
   <!-- Imprime la ruta absoluta al recurso indicado con el parámetro path -->   
   <xsl:template name="print-theme-path">
		<xsl:param name="path">/</xsl:param>
		<xsl:variable name="theme-path" select="$global-theme-path" />
		<xsl:variable name="context-path" select="$global-context-path" />
		
		<xsl:if test="not(starts-with($path, '/'))">
			<!-- Imprimo el context path si la URL no es absoluta -->
			<xsl:value-of select="$context-path" />
			<xsl:text>/themes/</xsl:text>
			<xsl:value-of select="$theme-path" />
			<xsl:if test="not(starts-with($path, '/'))">
				<xsl:text>/</xsl:text>
			</xsl:if>
		</xsl:if>
		<xsl:value-of select="$path" />
	</xsl:template>
    
   	<xsl:template name="renderDiscoveryField">
		<xsl:param name="href"/>
		<xsl:param name="value"/>
		<xsl:param name="classname"/>
		<span>
			<xsl:if test="$classname">
				<xsl:attribute name="class">
					<xsl:value-of select="$classname"/>
				</xsl:attribute>
			</xsl:if>
			<xsl:choose>
				<xsl:when test="$href">
					<xsl:call-template name="build-anchor">
						<xsl:with-param name="a.href" select="$href"/>
						<xsl:with-param name="a.value" select="$value"/>
					</xsl:call-template>
				</xsl:when>
				<xsl:when test="$value">
					<xsl:copy-of select="$value"/>
				</xsl:when>
				<xsl:otherwise>
					<i18n:text>xmlui.cicdigital.discoveryList.no-value</i18n:text>
				</xsl:otherwise>
			</xsl:choose>
		</span>
	</xsl:template>
	
</xsl:stylesheet>
