<!--
	/* Created for LINDAT/CLARIN */
    Rendering of a list of items (e.g. in a search or
    browse results page)

    Author: Amir Kamran
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
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    xmlns:xalan="http://xml.apache.org/xalan"
    xmlns:encoder="xalan://java.net.URLEncoder"
    xmlns:solrClientUtils="org.apache.solr.client.solrj.util.ClientUtils"
    xmlns:util="org.dspace.app.xmlui.utils.XSLUtils"
    xmlns:confman="org.dspace.core.ConfigurationManager"
    exclude-result-prefixes="xalan encoder solrClientUtils i18n dri mets dim xlink xsl util xhtml confman">

    <xsl:import href="../../core/elements.xsl"/>

    <xsl:output indent="yes" />

	<xsl:template match="dri:body/dri:div/dri:div[@id='aspect.discovery.SimpleSearch.div.search-results']" priority="3">
	
		<div class="clearfix well well-light">
		<xsl:if test="/dri:document/dri:options/dri:list[@n='discovery']/dri:list[count(dri:item/dri:xref)>1]">
		<div class="col-md-4 accordion" id="search-filters">
			<h4>
				Limit your search
			</h4>
			<xsl:for-each select="/dri:document/dri:options/dri:list[@n='discovery']/dri:list[count(dri:item/dri:xref)>1]">
				<div class="well well-sm well-white accordion-group" style="margin-bottom: 5px;">
					<div class="accordion-heading">
						<a class="accordion-toggle" data-toggle="collapse" data-parent="#search-filters">
							<xsl:attribute name="href">#<xsl:copy-of select="translate(@id, '.', '_')" /></xsl:attribute>
							<div>
							<strong>
								<xsl:apply-templates select="dri:head" />
							</strong>
							<b xmlns:i18n="http://apache.org/cocoon/i18n/2.1" class="arrow fa fa-caret-down pull-right">&#160;</b>
							</div>
						</a>
					</div>
					<div class="accordion-body collapse">
						<xsl:attribute name="id"><xsl:copy-of select="translate(@id, '.', '_')" /></xsl:attribute>
						<div class="accordion-inner no-padding">
						<ul class="nav nav-list">
						<li style="border-bottom: none;">
						<ul class="sublist">
							<xsl:for-each select="dri:item/dri:xref">
								<li>
									<a>										
										<xsl:attribute name="href"><xsl:value-of select="@target" /></xsl:attribute>
										<span class="wordbreak">
												<xsl:apply-templates select="./node()"/>
										</span>
									</a>
								</li>
							</xsl:for-each>
						</ul>
						</li>
						</ul>
						</div>
					</div>
				</div>
			</xsl:for-each>
		</div>
		</xsl:if>
		
		<div>
			<xsl:attribute name="class">
				<xsl:if test="/dri:document/dri:options/dri:list[@n='discovery']/dri:list[count(dri:item/dri:xref)>1]">
					<xsl:text> col-md-8</xsl:text>
				</xsl:if>
			</xsl:attribute>
			<xsl:apply-templates select="dri:head" />
			<xsl:apply-templates select="@pagination" />
			<hr/>
			<xsl:call-template name="standardAttributes" />
			<xsl:choose>
				<!-- does this element have any children -->
				<xsl:when test="child::node()">
					<xsl:apply-templates select="*[not(name()='head')]" />
				</xsl:when>
				<!-- if no children are found we add a space to eliminate self closing 
					tags -->
				<xsl:otherwise>
					&#160;
				</xsl:otherwise>
			</xsl:choose>
			<xsl:variable name="itemDivision">
				<xsl:value-of select="@n" />
			</xsl:variable>
			<xsl:variable name="xrefTarget">
				<xsl:value-of select="./dri:p/dri:xref/@target" />
			</xsl:variable>
					
			<xsl:apply-templates select="@pagination" />
						
		</div>
		</div>
    </xsl:template>


    <xsl:template match="dri:div[@n='search-results']/dri:head" priority="10">
        <h4>
            <xsl:apply-templates select="./node()" />
        </h4>
    </xsl:template>
    
    <!-- XXX hide dspace5 item-list instead use referenceSet and mets md
	 This effectively disables highlighting I believe
     -->
    <xsl:template match="dri:list[@n='search-results-repository']" priority="10">
    </xsl:template>
    <!-- An item rendered in the summaryList pattern. Commonly encountered in various browse-by pages
        and search results. -->
    <xsl:template name="itemSummaryList-DIM">
        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim" />
        <!-- Generate the thunbnail, if present, from the file section -->
        <xsl:apply-templates select="./mets:fileSec" mode="artifact-preview"/>
    </xsl:template>    

    <xsl:template match="mets:METS[@LABEL='DSpace Item'][mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']]" mode="summaryList" priority="10">
        <li class="item-box">
            <xsl:variable name="itemWithdrawn" select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/@withdrawn" />
    
            <xsl:variable name="href">
                <xsl:choose>
                    <xsl:when test="$itemWithdrawn">
                        <xsl:value-of select="@OBJEDIT"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="@OBJID"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>

            <xsl:choose>
                <xsl:when test="@LABEL='DSpace Item'">
                    <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim">
                        <xsl:with-param name="href" select="$href"/>
                    </xsl:apply-templates>
                    </xsl:when>
            </xsl:choose>
            <div class="label label-info" style="margin-bottom: 20px;">
                <xsl:variable name="file-size"
                    select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='local' and @element='files' and @qualifier='size']/node()" />
                <xsl:variable name="formatted-file-size">
                    <xsl:call-template name="format-size">                   
                        <xsl:with-param name="size" select="$file-size" />
                    </xsl:call-template>
                </xsl:variable>
                <xsl:variable name="file-count"
                    select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='local' and @element='files' and @qualifier='count']/node()" />
                <i class="fa fa-paperclip">&#160;</i>
                <i18n:translate>
                    <xsl:choose>
                        <xsl:when test="$file-count = 1">
                            <i18n:text>xmlui.UFAL.artifactbrowser.item-contains-one-file</i18n:text>
                        </xsl:when>
                        <xsl:when test="$file-count &gt; 1">
                            <i18n:text>xmlui.UFAL.artifactbrowser.item-contains-many-files</i18n:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <i18n:text>xmlui.UFAL.artifactbrowser.item-contains-no-files</i18n:text>
                        </xsl:otherwise>
                     </xsl:choose>
                     <i18n:param><xsl:value-of select="$file-count"/></i18n:param>
                     <i18n:param><xsl:copy-of select="$formatted-file-size"/></i18n:param>
                </i18n:translate>
            </div>
            <xsl:if test="mets:amdSec/mets:rightsMD/mets:mdWrap/mets:xmlData/license">
            	<div class="visible-xs" style="height: 20px;">&#160;</div>
                <div class="item-label {mets:amdSec/mets:rightsMD/mets:mdWrap/mets:xmlData/license/@label}" >
                    <span title="{mets:amdSec/mets:rightsMD/mets:mdWrap/mets:xmlData/license/@label_title}">
                        <xsl:value-of
                            select="mets:amdSec/mets:rightsMD/mets:mdWrap/mets:xmlData/license/@label_title" />
                    </span>
                    <xsl:for-each
                        select="mets:amdSec/mets:rightsMD/mets:mdWrap/mets:xmlData/license/labels/label">
                        <img class="" style="width: 16px"
                            src="{$theme-path}/images/licenses/{translate(@label, $uppercase, $smallcase)}.png" 
                            alt="{@label_title}" title="{@label_title}" />
                    </xsl:for-each>
                </div>
            </xsl:if>
        </li>
        <li style="list-style: none;">
        	<hr/>
        </li>
    </xsl:template>

    <xsl:template match="dim:dim">
        <xsl:param name="href"/>
        
        <xsl:if test="dim:field[@element='type']">
            <div class="item-type">
                    <xsl:value-of select="dim:field[@element='type'][1]/node()"/>
            </div>
        </xsl:if>
        
        <xsl:if test="dim:field[@mdschema='local' and @element='branding']">
        				<div class="item-branding label">
					<a>
					<xsl:attribute name="href">
						<xsl:copy-of select="$context-path"/>
						<xsl:value-of select="concat('/discover?filtertype=branding&amp;filter_relational_operator=equals&amp;filter=',encoder:encode(dim:field[@mdschema='local' and @element='branding'][1]/node()))"/>
					</xsl:attribute>
					<xsl:value-of select="dim:field[@mdschema='local' and @element='branding'][1]/node()"/>
					</a>
					</div>
        </xsl:if>
        
        <img class="artifact-icon pull-right" alt="{dim:field[@element='type'][1]/node()}" onerror="this.src='{$theme-path}/images/mime/application-x-zerosize.png'">
            <xsl:attribute name="src">
                <xsl:value-of select="$context-path" />
                <xsl:text>/themes/UFALHome/lib/images/</xsl:text>
                <xsl:value-of select="dim:field[@element='type'][1]/node()" />
                <xsl:text>.png</xsl:text>
            </xsl:attribute>
        </img>

        <div class="artifact-title">
            <xsl:element name="a">
                <xsl:attribute name="href">
                        <xsl:value-of select="$href" />
                    </xsl:attribute>
                <xsl:choose>
                    <xsl:when test="dim:field[@element='title']">
                        <xsl:value-of select="dim:field[@element='title'][1]/node()" />
                    </xsl:when>
                    <xsl:otherwise>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:element>
        </div>
        <xsl:if test="dim:field[@element='date' and @qualifier='issued'] or dim:field[@element='publisher']">
         <div class="publisher-date">
             <xsl:text>(</xsl:text>
             <xsl:if test="dim:field[@element='publisher']">
             	<xsl:for-each select="dim:field[@element='publisher']">
                    <a class="publisher">
                        <xsl:attribute name="href"><xsl:copy-of select="$context-path"/>/browse?value=<xsl:copy-of select="./node()"/>&amp;type=publisher</xsl:attribute>
                        <xsl:copy-of select="./node()"/>
                    </a>
					<xsl:if
                        test="count(following-sibling::dim:field[@element='publisher']) != 0">
                        <xsl:text>; </xsl:text>
                    </xsl:if>	                
	             </xsl:for-each>
	             <xsl:text> / </xsl:text>
             </xsl:if>
             <span class="date">	             				
				<xsl:value-of select="substring(dim:field[@element='date' and @qualifier='issued']/node(),1,10)"/>						            
			</span>
             <xsl:text>)</xsl:text>
         </div>
        </xsl:if>
        <div class="artifact-info">
            <span class="Z3988 hidden">
                <xsl:attribute name="title">
                    <xsl:call-template name="renderCOinS"/>
                </xsl:attribute>
                &#xFEFF; <!-- non-breaking space to force separating the end tag -->
            </span>        
            <div class="author-head">
                Author(s):
            </div>
            <div class="author">
                <xsl:choose>
                    <xsl:when test="dim:field[@element='contributor'][@qualifier='author' or @qualifier='other']">
                        <xsl:for-each
                            select="dim:field[@element='contributor'][@qualifier='author' or @qualifier='other']">
                            <span>
                                <xsl:if test="@authority">
                                    <xsl:attribute name="class"><xsl:text>ds-dc_contributor_author-authority</xsl:text></xsl:attribute>
                                </xsl:if>
                                <a>
									<xsl:attribute name="href"><xsl:copy-of select="$context-path"/>/browse?value=<xsl:copy-of select="node()" />&amp;type=author</xsl:attribute>
									<xsl:copy-of select="node()" />
								</a>                                
                            </span>
                            <xsl:if
                                test="count(following-sibling::dim:field[@element='contributor'][@qualifier='author' or @qualifier='other']) != 0">
                                <xsl:text>; </xsl:text>
                            </xsl:if>
                        </xsl:for-each>
                    </xsl:when>
                    <xsl:when test="dim:field[@element='creator']">
                        <xsl:for-each select="dim:field[@element='creator']">
                        	<a>
								<xsl:attribute name="href"><xsl:copy-of select="$context-path"/>/browse?value=<xsl:copy-of select="node()" />&amp;type=author</xsl:attribute>
								<xsl:copy-of select="node()" />
							</a>                            
                            <xsl:if
                                test="count(following-sibling::dim:field[@element='creator']) != 0">
                                <xsl:text>; </xsl:text>
                            </xsl:if>
                        </xsl:for-each>
                    </xsl:when>
                    <xsl:otherwise>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.no-author</i18n:text>
                    </xsl:otherwise>
                </xsl:choose>
            </div>
        </div>                
        <!-- xsl:choose>
            <xsl:when
                test="dim:field[@element = 'description' and @qualifier='abstract']">
                <xsl:variable name="abstract"
                    select="dim:field[@element = 'description' and @qualifier='abstract']/node()" />
                <div class="artifact-abstract-head">
                    Description:
                </div>
                <div class="artifact-abstract">
                    <xsl:value-of select="util:shortenString($abstract, 220, 10)" />
                </div>
            </xsl:when>
            <xsl:when test="dim:field[@element = 'description' and not(@qualifier)]">
                <xsl:variable name="description"
                    select="dim:field[@element = 'description' and not(@qualifier)]/node()" />
                <div class="artifact-abstract-head">
                    Description:
                </div>
                <div class="artifact-abstract">
                    <xsl:value-of select="util:shortenString($description, 220, 10)" />
                </div>
            </xsl:when>
        </xsl:choose-->
    </xsl:template>
    
    <xsl:template match="dri:list[@n='primary-search']" priority="10">
        <div class="accordion" id="filters">
            <div class="accordion-group">
                <div class="row" style="margin-top: 20px;"> 
                    <div class="input-group input-group-lg col-sm-10 col-sm-offset-1">
                        <span class="input-group-addon"><i class="fa fa-search fa-lg" style="color: #7479B8;">&#160;</i></span>
                        <input class="form-control" type="text">
                            <xsl:attribute name="name">
                                <xsl:value-of select="dri:item/dri:field[@n='query']/@n" />
                            </xsl:attribute>
                            <xsl:attribute name="id">
                                <xsl:value-of select="dri:item/dri:field[@n='query']/@id" />
                            </xsl:attribute>
                            <xsl:attribute name="value">
                                <xsl:value-of select="dri:item/dri:field[@n='query']/dri:value/node()" />
                            </xsl:attribute>                                                            
                        </input>
						<span class="input-group-btn">
							<input class="btn btn-large btn-repository" name="submit" type="submit" i18n:attr="value" value="xmlui.general.search" />
						</span>
                    </div>            
                </div>
                <xsl:if test="/dri:document//dri:div[@n='general-query']/dri:p[@n='hidden-fields']/dri:field[starts-with(@n,'filtertype')]">
                	<xsl:call-template name="selected-filters" />
                </xsl:if>
                <div class="bold accordion-heading">
                    <a class="accordion-toggle" data-toggle="collapse" data-parent="#filters" href="#add-filters">
                        <i18n:text>xmlui.UFAL.artifactbrowser.filters.add_filters</i18n:text>
                    </a>
                </div>
                <div id="add-filters">
                	<xsl:attribute name="class">
	                	<xsl:choose>
	                		<xsl:when test="contains($query-string, 'advance')">
	                			<xsl:text>accordion-body filters in</xsl:text>
	                		</xsl:when>
	                		<xsl:otherwise>
	                			<xsl:text>accordion-body collapse filters</xsl:text>
	                		</xsl:otherwise>
	                	</xsl:choose>
                	</xsl:attribute>
                    <xsl:apply-templates select="/dri:document//dri:div[@id='aspect.discovery.SimpleSearch.div.search-filters']/*" />                    
                </div>
            </div>
        </div>
        
    </xsl:template>
        
    <xsl:template match="dri:div[@id='aspect.discovery.SimpleSearch.div.search-filters']" />
    
    <xsl:template match="/dri:document//dri:div[@n='general-query']/dri:p[@n='hidden-fields']/dri:field" />
        
    <xsl:template match="dri:div[@n='browse-navigation']/dri:p" priority="10">
        <div>
            <xsl:attribute name="class">
                container-fluid inline <xsl:value-of select="@rend" />
            </xsl:attribute>
            <xsl:apply-templates />
        </div>
    </xsl:template>
    
    <xsl:template name="selected-filters">
        <div class="filters well well-light">
        	<h5>
                <i18n:text>xmlui.UFAL.artifactbrowser.filters.selected</i18n:text>
			</h5>
	        <xsl:for-each select="/dri:document//dri:div[@n='general-query']/dri:p[@n='hidden-fields']/dri:field[starts-with(@n,'filtertype')]">
	        	<xsl:variable name="filter_number" select="substring-after(@n, 'filtertype_')" />
	        	<xsl:variable name="filtertype" select="dri:value/node()" />
	        	<xsl:variable name="filteroperator" select="../dri:field[@n=concat('filter_relational_operator_', $filter_number)]/dri:value/node()" />
	        	<xsl:variable name="filtervalue" select="../dri:field[@n=concat('filter_', $filter_number)]/dri:value/node()" />
				<span style="padding: 5px 20px 5px 10px; margin: 2px; position: relative;">
					<xsl:attribute name="class">
						<xsl:text>badge</xsl:text>
	                        <xsl:choose>
	                                <xsl:when test="$filteroperator='notequals' or $filteroperator='notcontains' or $filteroperator='notavailable'">
	                                        <xsl:text> badge-important</xsl:text>
	                                </xsl:when>
	                                <xsl:otherwise>
	                                        <xsl:text> badge-info</xsl:text>
	                                </xsl:otherwise>
	                        </xsl:choose>
						</xsl:attribute>
					<xsl:choose>
						<xsl:when test="$filteroperator='notequals' or $filteroperator='notcontains'">
							<i class="fa fa-search-minus fa-lg">&#160;</i>
						</xsl:when>
						<xsl:when test="$filteroperator='notavailable'">
							<i class="fa fa-ban fa-lg">&#160;</i>
						</xsl:when>						
						<xsl:otherwise>
							<i class="fa fa-search-plus fa-lg">&#160;</i>
						</xsl:otherwise>
					</xsl:choose>
					<i18n:text>xmlui.ArtifactBrowser.SimpleSearch.filter.<xsl:value-of select="$filtertype"/></i18n:text>					
					<xsl:choose>
						<xsl:when test="$filteroperator='notavailable'">
							&#160;							
						</xsl:when>
						<xsl:otherwise>
							: <xsl:value-of select="$filtervalue" />
						</xsl:otherwise>
					</xsl:choose>
					&#160;
					<i class="selected-filter-close-icon fa fa-times-circle"
						style="cursor: pointer; top: 2px; position: absolute; right: 2px;">
						<xsl:attribute name="filter_number"><xsl:value-of select="$filter_number" /></xsl:attribute>
						<span style="display:none;">&#160;</span>
					</i>
				</span>
			</xsl:for-each>
            <span class="badge selected-filter-clearall" style="padding: 5px 10px; margin: 2px; cursor: pointer">
                <i18n:text>xmlui.UFAL.artifactbrowser.filters.clear</i18n:text>
            </span>
        </div>
    </xsl:template>
    
    <xsl:template match="dri:item[dri:field[@id='aspect.discovery.SimpleSearch.field.submit_update_filters']]">
    	<div class="hidden">
    		<xsl:apply-templates select="dri:field" />
    	</div>
    </xsl:template>
    
    <xsl:template match="dri:item[@id='aspect.discovery.SimpleSearch.item.has_files']" />
    
    <xsl:template match="dri:field[@n='search-filter-controls']">
    	
    </xsl:template>
    
    <xsl:template match="dri:p[dri:field[@id='aspect.artifactbrowser.ConfigurableBrowse.field.submit']]">
        <div class="alert">
                Using the jump menu or typing a value will move the pointer to the best matched point in the following list.<BR/>
                e.g. If you are typing a year 2000 and there is no record with this year, the pointer will be moved to the next possible record after the year 2000.
        </div>
        <xsl:apply-templates select="dri:field" />
    </xsl:template>    
            
</xsl:stylesheet>



