<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<!--
    Modifications to the rendering of elements in the administrative aspect.

    Author: art.lowel at atmire.com
    Author: lieven.droogmans at atmire.com
    Author: ben at atmire.com

   modified for LINDAT/CLARIN
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

    <!--Add the class 'tabbed' to the form for administrative pages containing tabs and wrap the tab content with a div with class 'pane',
    the rest is just a copy of the default interactive div template-->
    <xsl:template match="dri:div[contains(@rend, 'administrative') and dri:list[@rend = 'horizontal']]">
        <xsl:apply-templates select="dri:head"/>
        <xsl:apply-templates select="@pagination">
            <xsl:with-param name="position">top</xsl:with-param>
        </xsl:apply-templates>
        <form>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-interactive-div tabbed</xsl:with-param>
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
			<xsl:apply-templates select="dri:list[@rend = 'horizontal']"/>
            <div class="pane">
                <xsl:apply-templates select="*[not(name()='head' or @rend = 'horizontal')]"/>
            </div>

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


    <!--the tabs are floating, so give the ul the class 'clearfix' to ensure it has a height-->
    <xsl:template match="dri:div[contains(@rend, 'administrative')]/dri:list[@rend = 'horizontal']">
        <xsl:apply-templates select="dri:head"/>
        <ul>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">nav nav-tabs</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="*[not(name()='head')]" mode="nested"/>
        </ul>
    </xsl:template>

    <!--give the active tab the class 'active-tab'-->
    <xsl:template match="dri:div[contains(@rend, 'administrative')]/dri:list[@rend = 'horizontal']/dri:item[dri:hi[@rend = 'bold']]" mode="nested">
        <li class="active">
            <xsl:apply-templates select="dri:hi/node()" />
        </li>
    </xsl:template>
    
    <!--Template for the bitstream reordering-->
    <xsl:template match="dri:cell[starts-with(@id, 'aspect.administrative.item.EditItemBitstreamsForm.cell.bitstream_order_')]" priority="2">
        <td>
            <xsl:call-template name="standardAttributes"/>
            <xsl:apply-templates select="*[not(@type='button')]" />
            <!--A div that will indicate the old & the new order-->
            <div>
                <span>
                    <!--Give this one an ID so that the javascript can change his value-->
                    <xsl:attribute name="id">
                        <xsl:value-of select="dri:field/@id"/>
                        <xsl:text>_new</xsl:text>
                    </xsl:attribute>
                    <xsl:value-of select="dri:field/dri:value"/>
                </span>
                <xsl:text> (</xsl:text>
                <i18n:text>xmlui.administrative.item.EditItemBitstreamsForm.previous_order</i18n:text>
                <xsl:value-of select="dri:field/dri:value"/>
                <xsl:text>)</xsl:text>
            </div>
        </td>
        <td>
            <xsl:apply-templates select="dri:field[@type='button']"/>
        </td>
    </xsl:template>
    
    <xsl:template match="dri:div[@n='control-panel']//dri:list[not(@type)]" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <xsl:if test="count(dri:label)>0">
            <table>
                <xsl:call-template name="standardAttributes">
                    <xsl:with-param name="class">table</xsl:with-param>
                </xsl:call-template>
                <xsl:apply-templates select="dri:item" mode="labeled"/>
            </table>
        </xsl:if>
        <xsl:if test="count(dri:label)=0">
            <ul>
                <xsl:call-template name="standardAttributes">
                    <xsl:with-param name="class">ds-simple-list</xsl:with-param>
                </xsl:call-template>
                <xsl:apply-templates select="dri:item" mode="nested"/>
            </ul>
        </xsl:if>
    </xsl:template>
    
    
    <xsl:template match="dri:list[@n='replication_tabs']" priority="10">
    	<div role="tabpanel">
    		<ul class="nav nav-tabs" role="tablist">
				<xsl:for-each select="dri:item/dri:xref">
					<li role="presentation">
						<xsl:attribute name="class">
							<xsl:value-of select="../@n" />
						</xsl:attribute>
						<a>							
							<xsl:attribute name="href">
								<xsl:value-of select="@target" />
							</xsl:attribute>
		 					<xsl:value-of select="node()" />
						</a>
					</li>
		 		</xsl:for-each>
    		</ul>    		
    	</div>
    </xsl:template>
    
    <xsl:template match="dri:field[@id='aspect.administrative.ControlPanel.field.action']" priority="10">
        <input type="submit">
                <xsl:call-template name="standardAttributes" />
                <xsl:attribute name="name">
                        <xsl:value-of select="@n" />
                </xsl:attribute>
                <xsl:attribute name="value">
                        <xsl:value-of select="dri:value/node()" />
                </xsl:attribute>
        </input>
    </xsl:template>
    
</xsl:stylesheet>
