<!--
	/* Created for LINDAT/CLARIN */
    Templates to cover the attribute calls.
    Author: Amir Kamran
-->

<xsl:stylesheet version="1.0"
	xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/"
	xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns="http://www.w3.org/1999/xhtml"
	exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc">

    <xsl:output indent="yes" />

    <xsl:template match="@pagination" priority="10">
		<div class="navbar" style="margin: 0px; box-shadow: none; background: none;">
			<div class="navbar-inner">
		        <xsl:choose>
		            <xsl:when test=". = 'simple'">
		                <ul class="pager">
	                        <li>
	                        	<xsl:choose>
	                        		<xsl:when test="not(parent::node()/@previousPage)">
	                        			<xsl:attribute name="class">
	                        				previous disabled
	                        			</xsl:attribute>
	                        			<a href="#">
	                        				<i class="fa fa-angle-left">&#160;</i>
	                                    	<i18n:text>xmlui.dri2xhtml.structural.pagination-previous</i18n:text>
	                        			</a>	                        			
	                        		</xsl:when>	                        	
		                            <xsl:otherwise>
	                        			<xsl:attribute name="class">
	                        				previous
	                        			</xsl:attribute>		                            
		                                <a>
		                                    <xsl:attribute name="href">
		                                        <xsl:value-of select="parent::node()/@previousPage"/>
		                                    </xsl:attribute>
		                                    <i class="fa fa-angle-left">&#160;</i>
		                                    <i18n:text>xmlui.dri2xhtml.structural.pagination-previous</i18n:text>
		                                </a>
		                            </xsl:otherwise>
	                            </xsl:choose>
	                        </li>
	                        <li>
	                        <span style="border: none; background-color: inherit;">
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
	                        </span>	                        
	                        </li>	                        
	                        <li>
	                        	<xsl:choose>
	                        		<xsl:when test="not(parent::node()/@nextPage)">
	                        			<xsl:attribute name="class">
	                        				next disabled
	                        			</xsl:attribute>
	                        			<a href="#">
	                                    	<i18n:text>xmlui.dri2xhtml.structural.pagination-next</i18n:text>
	                        				<i class="fa fa-angle-right">&#160;</i>	                                    	
	                        			</a>	                        			
	                        		</xsl:when>	                        	
		                            <xsl:otherwise>
	                        			<xsl:attribute name="class">
	                        				next
	                        			</xsl:attribute>		                            
		                                <a>
		                                    <xsl:attribute name="href">
		                                        <xsl:value-of select="parent::node()/@nextPage"/>
		                                    </xsl:attribute>
		                                    <i18n:text>xmlui.dri2xhtml.structural.pagination-next</i18n:text>
		                                    <i class="fa fa-angle-right">&#160;</i>		                                    
		                                </a>
		                            </xsl:otherwise>
	                            </xsl:choose>
	                        </li>
	                    </ul>                	
					</xsl:when>
		            <xsl:when test=". = 'masked'">
		                <div style="float: left;">
		                    <ul class="pagination">
		                        <xsl:if test="(parent::node()/@currentPage - 3) &gt; 0">
		                            <li class="page-link">
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
		                    
		                        <xsl:if test="not(parent::node()/@firstItemIndex = 0 or parent::node()/@firstItemIndex = 1)">
		                            <li>
		                                <a>
		                                    <xsl:attribute name="href">
		                                        <xsl:value-of
		                                                select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
		                                        <xsl:value-of select="parent::node()/@currentPage - 1"/>
		                                        <xsl:value-of
		                                                select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
		                                    </xsl:attribute>
		                                    <!-- i18n:text>xmlui.dri2xhtml.structural.pagination-previous</i18n:text-->
		                                    <i class="fa fa-angle-left">&#160;</i>
		                                </a>
		                            </li>
		                        </xsl:if>
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
		                        
		                        
		                        <xsl:if test="not(parent::node()/@lastItemIndex = parent::node()/@itemsTotal)">
		                            <li>
		                                <a>
		                                    <xsl:attribute name="href">
		                                        <xsl:value-of
		                                                select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
		                                        <xsl:value-of select="parent::node()/@currentPage + 1"/>
		                                        <xsl:value-of
		                                                select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
		                                    </xsl:attribute>
		                                    <!-- i18n:text>xmlui.dri2xhtml.structural.pagination-next</i18n:text-->
		                                    <i class="fa fa-angle-right">&#160;</i>
		                                </a>
		                            </li>
		                        </xsl:if>
		                        <xsl:if test="(parent::node()/@currentPage + 3) &lt;= (parent::node()/@pagesTotal)">
		                            <li class="page-link">
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
		                    </ul>
		                </div>
		            </xsl:when>			
				</xsl:choose>
				
				<xsl:if test="../..//dri:list[@n='sort-options']">
					<ul class="nav pull-right">
                      <li class="dropdown">
                        <a href="#" class="dropdown-toggle" data-toggle="dropdown"><i class="fa fa-gear">&#160;</i> <b class="caret">&#160;</b></a>
                        <xsl:apply-templates select="../..//dri:list[@n='sort-options']" />
                      </li>
                    </ul>				
				</xsl:if>
				
			</div>
		</div>
    </xsl:template>
    
    <xsl:template name="offset-link" priority="10">
        <xsl:param name="pageOffset"/>
        <xsl:if test="((parent::node()/@currentPage + $pageOffset) &gt; 0) and
            ((parent::node()/@currentPage + $pageOffset) &lt;= (parent::node()/@pagesTotal))">
            <li class="page-link">
            	<xsl:choose>
                <xsl:when test="$pageOffset = 0">
                    <xsl:attribute name="class">page-link active</xsl:attribute>
                    <a href="#" onclick="return false;">
                    	<xsl:value-of select="parent::node()/@currentPage + $pageOffset"/>
                    </a>
                </xsl:when>
                <xsl:otherwise>
	                <a>
	                    <xsl:attribute name="href">
	                        <xsl:value-of select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
	                        <xsl:value-of select="parent::node()/@currentPage + $pageOffset"/>
	                        <xsl:value-of select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
	                    </xsl:attribute>
	                    <xsl:value-of select="parent::node()/@currentPage + $pageOffset"/>
	                </a>
                </xsl:otherwise>
                </xsl:choose>
            </li>
        </xsl:if>
    </xsl:template>
    
	<xsl:template match="dri:div[@n='masked-page-control']" priority="10" />

	<xsl:template match="dri:list[@n='sort-options']" priority="10" >
		<ul class="dropdown-menu">
			<xsl:for-each select="dri:item">
				<xsl:if test="position()!=1">
					<li class="divider">&#160;</li>
				</xsl:if>
				<li>
					<h6 style="padding: 0 0 0 10px; margin: 0">
						<xsl:apply-templates select="./node()" />
					</h6>
				</li>
				<xsl:if test="following-sibling::*[1][self::dri:list]">
					<xsl:for-each select="following-sibling::*[1]/dri:item">
						<li>
							<xsl:if test="dri:xref[@rend='gear-option gear-option-selected'] or self::dri:item[@rend='gear-option gear-option-selected']">
								<xsl:attribute name="class">
									<xsl:text>disabled</xsl:text>
								</xsl:attribute>
							</xsl:if>
							<a>
								<xsl:if test="dri:xref[@rend='gear-option gear-option-selected'] or self::dri:item[@rend='gear-option gear-option-selected']">
									<i class="fa fa-check">&#160;</i>
								</xsl:if>
								<xsl:attribute name="href">
									<xsl:value-of select="dri:xref/@target" />
								</xsl:attribute>
								<xsl:apply-templates select="dri:xref/node()" />
							</a>
						</li>
					</xsl:for-each>
				</xsl:if>
			</xsl:for-each>
		</ul>
	</xsl:template>    


    <!-- checkbox and radio fields type uses this attribute -->
    <xsl:template match="@returnValue">
        <xsl:attribute name="value"><xsl:value-of select="."/></xsl:attribute>
    </xsl:template>

    <!-- used for image buttons -->
    <xsl:template match="@source">
        <xsl:attribute name="src"><xsl:value-of select="."/></xsl:attribute>
    </xsl:template>

    <!-- size and maxlength used by text, password, and textarea inputs -->
    <xsl:template match="@size">
        <xsl:attribute name="size"><xsl:value-of select="."/></xsl:attribute>
    </xsl:template>

    <!-- used by select element -->
    <xsl:template match="@evtbehavior">
        <xsl:param name="behavior" select="."/>
        <xsl:if test="normalize-space($behavior)='submitOnChange'">
            <xsl:attribute name="onchange">this.form.submit();</xsl:attribute>
                </xsl:if>
    </xsl:template>

    <xsl:template match="@maxlength">
        <xsl:attribute name="maxlength"><xsl:value-of select="."/></xsl:attribute>
    </xsl:template>

    <!-- "multiple" attribute is used by the <select> input method -->
    <xsl:template match="@multiple[.='yes']">
        <xsl:attribute name="multiple">multiple</xsl:attribute>
    </xsl:template>

    <!-- rows and cols attributes are used by textarea input -->
    <xsl:template match="@rows">
        <xsl:attribute name="rows"><xsl:value-of select="."/></xsl:attribute>
    </xsl:template>

    <xsl:template match="@cols">
        <xsl:attribute name="cols"><xsl:value-of select="."/></xsl:attribute>
    </xsl:template>

    <!-- The general "catch-all" template for attributes matched, but not handled above -->
    <xsl:template match="@*"></xsl:template>
    
</xsl:stylesheet>
