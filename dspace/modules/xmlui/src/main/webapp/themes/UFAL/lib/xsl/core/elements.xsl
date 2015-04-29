<!--
	/* Created for LINDAT/CLARIN */
    Templates to cover the common dri elements.
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
    
    <!-- Non-interactive divs get turned into HTML div tags. The general process, which is found in many
        templates in this stylesheet, is to call the template for the head element (creating the HTML h tag),
        handle the attributes, and then apply the templates for the all children except the head. The id
        attribute is -->
        
	<xsl:template match="dri:div" priority="1">
		<div>
			<xsl:call-template name="standardAttributes" />
			<xsl:apply-templates select="dri:head" />
			<xsl:apply-templates select="@pagination" />
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
    </xsl:template>
    
    <xsl:template match="dri:div[@n='stack-trace']" priority="2">
    	<div class="alert alert-error ">
    		<div class="accordion-group">
				<div class="accordion-heading">
					<a class="accordion-toggle" data-toggle="collapse">
						<xsl:attribute name="href">
							<xsl:text>#</xsl:text><xsl:value-of select="translate(dri:list[@n='stack']/dri:list[@n='trace']/@id, '.', '_')" />
						</xsl:attribute>
						<span class="label label-important">
							<xsl:value-of select="dri:list[@n='stack']/dri:item/dri:xref/node()" />
						</span>
					</a>
				</div>
    		</div>
    		<div class="accordion-body collapse">
				<xsl:attribute name="id">
					<xsl:value-of select="translate(dri:list[@n='stack']/dri:list[@n='trace']/@id, '.', '_')" />
				</xsl:attribute>
				<div class="accordion-inner wordbreak">
					<xsl:apply-templates select="dri:list[@n='stack']/dri:list[@n='trace']" />
				</div>
    		</div>
    	</div>
    </xsl:template>
    
	<xsl:template match="dri:body/dri:div/dri:div" priority="2">		
		<div>
			<xsl:call-template name="standardAttributes">
				<xsl:with-param name="class">well well-light</xsl:with-param>
			</xsl:call-template>								
			<xsl:apply-templates select="dri:head" />			
			<xsl:apply-templates select="@pagination" />
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

            <xsl:choose>
                <xsl:when test="count(current()//dri:xref) &gt; 10">
                    <xsl:apply-templates select="@pagination" />
                </xsl:when>
            </xsl:choose>
		</div>
    </xsl:template>
    
    <!-- Interactive divs get turned into forms. The priority attribute on the template itself
        signifies that this template should be executed if both it and the one above match the
        same element (namely, the div element).

        Strictly speaking, XSL should be smart enough to realize that since one template is general
        and other more specific (matching for a tag and an attribute), it should apply the more
        specific once is it encounters a div with the matching attribute. However, the way this
        decision is made depends on the implementation of the XSL parser is not always consistent.
        For that reason explicit priorities are a safer, if perhaps redundant, alternative. -->
    <xsl:template match="dri:div[@interactive='yes']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <xsl:apply-templates select="@pagination" />
        <form>
            <xsl:call-template name="standardAttributes">
                
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
			<xsl:apply-templates select="*[not(name()='head')]"/>
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
        <xsl:apply-templates select="@pagination" />
    </xsl:template>
    
        <!-- Special case for divs tagged as "notice" -->
    <xsl:template match="dri:div[@n='general-message']" priority="3">
        <div>
            <xsl:call-template name="standardAttributes" />
            <xsl:apply-templates />
        </div>
    </xsl:template>    
    

    <!-- First, the table element, used for rendering data in tabular format. In DRI tables consist of
        an optional head element followed by a set of row tags. Each row in turn contains a set of cells.
        Rows and cells can have different roles, the most common ones being header and data (with the
        attribute omitted in the latter case). -->

    <xsl:template match="dri:table">
        <table>
            <xsl:call-template name="standardAttributes">
               	<xsl:with-param name="class">table table-bordered table-hover</xsl:with-param>
            </xsl:call-template>        
        	<caption>
        		<xsl:apply-templates select="dri:head/node()"/>
        	</caption>
            <xsl:apply-templates select="dri:row"/>
        </table>
    </xsl:template>

    <!-- Header row, most likely filled with header cells -->
    <xsl:template match="dri:row[@role='header']" priority="2">
        <tr class="info">
            <xsl:apply-templates />
        </tr>
    </xsl:template>

    <!-- Header cell, assumed to be one since it is contained in a header row -->
    <xsl:template match="dri:row[@role='header']/dri:cell | dri:cell[@role='header']" priority="2">
        <th>
            <xsl:call-template name="standardAttributes" />
            <xsl:if test="@rows">
                <xsl:attribute name="rowspan">
                    <xsl:value-of select="@rows"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:if test="@cols">
                <xsl:attribute name="colspan">
                    <xsl:value-of select="@cols"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:apply-templates />
        </th>
    </xsl:template>


    <!-- Normal row, most likely filled with data cells -->
    <xsl:template match="dri:row" priority="1">
        <tr>
            <xsl:call-template name="standardAttributes" />
            <xsl:apply-templates />
        </tr>
    </xsl:template>

    <!-- Just a plain old table cell -->
    <xsl:template match="dri:cell" priority="1">
        <td>
            <xsl:call-template name="standardAttributes" />
            <xsl:if test="@rows">
                <xsl:attribute name="rowspan">
                    <xsl:value-of select="@rows"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:if test="@cols">
                <xsl:attribute name="colspan">
                    <xsl:value-of select="@cols"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:apply-templates />
        </td>
    </xsl:template>
    
    <!-- Second, the p element, used for display of text. The p element is a rich text container, meaning it
        can contain text mixed with inline elements like hi, xref, figure and field. The cell element above
        and the item element under list are also rich text containers.
    -->
    <xsl:template match="dri:p">
        <p>
            <xsl:call-template name="standardAttributes" />
            <xsl:choose>
                <!--  does this element have any children -->
                <xsl:when test="child::node()">
					<xsl:apply-templates />
				</xsl:when>
				<!-- if no children are found we add a space to eliminate self closing tags -->
                <xsl:otherwise>
                        &#160;
                </xsl:otherwise>
            </xsl:choose>
        </p>
    </xsl:template>
    

    <!--Removed the automatic font sizing for headers, because while I liked the idea,
     in practice it's too unpredictable.
     Also made all head's follow the same rule: count the number of ancestors that have
     a head, that's the number after the 'h' in the tagname-->
    <xsl:template name="renderHead">
        <xsl:param name="class"/>
        <xsl:variable name="head_count" select="count(ancestor::dri:*[dri:head])+2"/>
        <xsl:element name="h{$head_count}">
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class" select="$class"/>
            </xsl:call-template>
            <xsl:apply-templates />
        </xsl:element>
    </xsl:template>

    <xsl:template match="dri:head" priority="1">
        <xsl:call-template name="renderHead">
            <xsl:with-param name="class">small</xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <!-- Progress list used primarily in forms that span several pages. There isn't a template for the nested
        version of this list, mostly because there isn't a use case for it. -->
    <xsl:template match="dri:list[@type='progress']" priority="3">
        <xsl:apply-templates select="dri:head"/>
        <div class="container-fluid">
	        <ul>
	            <xsl:call-template name="standardAttributes">
	                <xsl:with-param name="class">progressbar</xsl:with-param>
	            </xsl:call-template>
	            <xsl:apply-templates select="dri:item"/>
	        </ul>
        </div>
    </xsl:template>

    <xsl:template match="dri:list[@type='progress']/dri:item" priority="3">
        <li>
        	<xsl:attribute name="class">
        		<xsl:choose>
		        	<xsl:when test="@rend='current'">
		        		active	        	
		        	</xsl:when>
		        	<xsl:when test="@rend='disabled'">
		        	</xsl:when>		        	
		        	<xsl:otherwise>
		        		complete
		        	</xsl:otherwise>
	        	</xsl:choose>
        	</xsl:attribute>
        	<span class="step">
        		<xsl:if test="not(@rend)">
        			<xsl:attribute name="onclick">$('#<xsl:value-of select="translate(dri:field/@id,'.','_')" />').click();</xsl:attribute>
        		</xsl:if>.
        	</span>
        	<div class="hidden">
        	<xsl:apply-templates select="dri:field[@type='button']" />
        	</div>        	
        	<span class="title"><xsl:apply-templates select="dri:field/dri:value/node()" /></span>
        	
        </li>
    </xsl:template>

    <xsl:template match="dri:list[@n='jump-list']" priority="10">
        <xsl:apply-templates select="dri:head"/>
        <ul>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">nav nav-pills</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="*[not(name()='head')]" mode="nested"/>
        </ul>
    </xsl:template>        
    
    <!-- Finally, the generic header element template, given the lowest priority, is there for cases not
        covered above. It assumes nothing about the parent element and simply generates an HTML h3 tag -->
    <xsl:template match="dri:head" priority="1">
        <h3>
            <xsl:call-template name="standardAttributes" />
            <xsl:apply-templates />
        </h3>
    </xsl:template>
    
    
    <!-- Next come the components of rich text containers, namely: hi, xref, figure and, in case of interactive
        divs, field. All these can mix freely with text as well as contain text of their own. The templates for
        the first three elements are fairly straightforward, as they simply create HTML span, a, and img tags,
        respectively. -->

    <xsl:template match="dri:hi">
        <span>
            <xsl:attribute name="class">bold</xsl:attribute>
            <xsl:if test="@rend">
                <xsl:attribute name="class"><xsl:value-of select="@rend"/></xsl:attribute>
            </xsl:if>
            <xsl:apply-templates />
        </span>
    </xsl:template>
    
    
    <xsl:template match="dri:xref">
        <a>
            <xsl:if test="@target">
                <xsl:attribute name="href"><xsl:value-of select="@target"/></xsl:attribute>
            </xsl:if>

            <xsl:if test="@rend">
                <xsl:attribute name="class"><xsl:value-of select="@rend"/></xsl:attribute>
            </xsl:if>

            <xsl:if test="contains(@rend, 'target_blank')">
                <xsl:attribute name="target">_blank</xsl:attribute>
            </xsl:if>

            <xsl:if test="@n">
                <xsl:attribute name="name"><xsl:value-of select="@n"/></xsl:attribute>
            </xsl:if>

            <xsl:apply-templates />
        </a>
    </xsl:template>
    

    <xsl:template match="dri:figure">
        <xsl:if test="@target">
            <a>
                <xsl:attribute name="href"><xsl:value-of select="@target"/></xsl:attribute>
                <xsl:if test="@title">
                	<xsl:attribute name="title"><xsl:value-of select="@title"/></xsl:attribute>
                </xsl:if>
                <xsl:if test="@rend">
                	<xsl:attribute name="class"><xsl:value-of select="@rend"/></xsl:attribute>
                </xsl:if>
                <img>
                    <xsl:attribute name="src"><xsl:value-of select="@source"/></xsl:attribute>
                    <xsl:attribute name="alt"><xsl:apply-templates /></xsl:attribute>
                </img>
                <xsl:attribute name="border"><xsl:text>none</xsl:text></xsl:attribute>
            </a>
        </xsl:if>
        <xsl:if test="not(@target)">
            <img>
                <xsl:attribute name="src"><xsl:value-of select="@source"/></xsl:attribute>
                <xsl:attribute name="alt"><xsl:apply-templates /></xsl:attribute>
            </img>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
