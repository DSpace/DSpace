
<!--
    Templates to cover the common dri elements.

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
	xmlns="http://www.w3.org/1999/xhtml"
	exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc">

    <xsl:output indent="yes"/>

    <xsl:template match="dri:div[@interactive='yes']" priority="2">
        <xsl:apply-templates select="@pagination">
            <xsl:with-param name="position">top</xsl:with-param>
        </xsl:apply-templates>
        <form>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-interactive-div</xsl:with-param>
            </xsl:call-template>
            <xsl:attribute name="action"><xsl:value-of select="@action"/></xsl:attribute>
            <xsl:attribute name="method"><xsl:value-of select="@method"/></xsl:attribute>
            <xsl:if test="@autocomplete">
                <xsl:attribute name="autocomplete"><xsl:value-of select="@autocomplete"/></xsl:attribute>
            </xsl:if>
            <xsl:if test="@method='multipart'">
                <xsl:attribute name="method">post</xsl:attribute>
                <xsl:attribute name="enctype">multipart/form-data</xsl:attribute>
            </xsl:if>
            <xsl:attribute name="onsubmit">javascript:tSubmit(this);</xsl:attribute>
            <!--For Item Submission process, disable ability to submit a form by pressing 'Enter'-->
            <xsl:if test="starts-with(@n,'submit')">
                <xsl:attribute name="onkeydown">javascript:return disableEnterKey(event);</xsl:attribute>
            </xsl:if>

            <xsl:apply-templates select="dri:head"/>
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
        <xsl:apply-templates select="@pagination">
            <xsl:with-param name="position">bottom</xsl:with-param>
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template match="dri:head" mode="panel-heading">
        <div>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">panel-heading</xsl:with-param>
            </xsl:call-template>
            <h3 class="panel-title">
                <xsl:apply-templates/>
            </h3>

        </div>
    </xsl:template>

    <xsl:template name="renderHead">
        <xsl:param name="class"/>
        <xsl:variable name="head_count" select="count(ancestor::dri:*[dri:head])"/>
        <xsl:variable name="is_first_head_on_page" select="(//dri:head)[1] = ."/>
            <xsl:element name="h{$head_count+1}">
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">
                    <xsl:value-of select="$class"/>
                    <xsl:if test="$head_count = 1 and not($class='ds-option-set-head')">
                        <xsl:text> page-header</xsl:text>
                    </xsl:if>
                    <xsl:if test="$is_first_head_on_page">
                        <xsl:text> first-page-header</xsl:text>
                    </xsl:if>
                </xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </xsl:element>
    </xsl:template>


    <xsl:template match="dri:div/dri:head" priority="3">
        <xsl:call-template name="renderHead">
            <xsl:with-param name="class">ds-div-head</xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <!-- The second case is the header on tables, which always creates an HTML h3 element -->
    <xsl:template match="dri:table/dri:head" priority="2">
        <xsl:call-template name="renderHead">
            <xsl:with-param name="class">ds-table-head</xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <!-- The third case is the header on lists, which creates an HTML h3 element for top level lists and
        and h4 elements for all sublists. -->
    <xsl:template match="dri:list/dri:head" priority="2" mode="nested">
        <xsl:call-template name="renderHead">
            <xsl:with-param name="class">ds-list-head</xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="dri:list/dri:list/dri:head" priority="3" mode="nested">
        <xsl:call-template name="renderHead">
            <xsl:with-param name="class">ds-sublist-head</xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="dri:referenceSet/dri:head" priority="2">
        <xsl:call-template name="renderHead">
            <xsl:with-param name="class">ds-list-head</xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="dri:head" priority="1">
        <xsl:call-template name="renderHead">
            <xsl:with-param name="class">ds-head</xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <!-- Progress list used primarily in forms that span several pages. There isn't a template for the nested
        version of this list, mostly because there isn't a use case for it. -->
    <xsl:template match="dri:list[@type='progress']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <ul>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-progress-list list-inline</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="dri:item"/>
        </ul>
    </xsl:template>

    <xsl:template match="dri:list[@type='progress']/dri:item" priority="2">
        <li>
            <xsl:attribute name="class">
                <xsl:value-of select="@rend"/>
                <xsl:if test="position()=1">
                    <xsl:text> first</xsl:text>
                </xsl:if>
                <xsl:if test="descendant::dri:field[@type='button']">
                    <xsl:text> button</xsl:text>
                </xsl:if>
                <xsl:if test="position()=last()">
                    <xsl:text> last</xsl:text>
                </xsl:if>
            </xsl:attribute>
            <span>
                <xsl:attribute name="class">
                    <xsl:text>label</xsl:text>
                    <xsl:choose>
                        <xsl:when test="contains(@rend, 'current')">
                            <xsl:text> label-success</xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text> label-default</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>

                </xsl:attribute>
                <xsl:apply-templates/>
            </span>

        </li>
        <xsl:if test="not(position()=last())">
            <li class="arrow">
                <xsl:text>&#8594;</xsl:text>
            </li>
        </xsl:if>
    </xsl:template>

    <xsl:template match="dri:table">
        <xsl:apply-templates select="dri:head"/>
        <div class="table-responsive">
        <table>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-table table table-striped table-hover</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="dri:row"/>
        </table>
        </div>
    </xsl:template>


    <xsl:template match="dri:list[@n='selectlist']" priority="1">
        <!--<xsl:apply-templates select="dri:head"/>-->
        <!--<ul>-->
            <!--<xsl:call-template name="standardAttributes">-->
                <!--<xsl:with-param name="class">ds-simple-list</xsl:with-param>-->
            <!--</xsl:call-template>-->
            <!--<xsl:apply-templates select="*[not(name()='head')]" mode="nested"/>-->
        <!--</ul>-->

        <fieldset>
            <xsl:call-template name="standardAttributes"/>
            <label><xsl:apply-templates select="dri:item"/></label>
            <!--<xsl:apply-templates select="dri:list/dri:item/dri:field"/>-->
            <xsl:apply-templates select="following-sibling::dri:list[@n='sublist'][1]" mode="selectlist"/>
        </fieldset>

    </xsl:template>


    <xsl:template match="dri:list[@n='sublist']" priority="1"/>

    <xsl:template match="dri:list[@n='sublist']" mode="selectlist" priority="1">
        <xsl:apply-templates select="dri:item/dri:field"/>
    </xsl:template>


</xsl:stylesheet>
