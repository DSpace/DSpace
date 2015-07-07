<?xml version="1.0" encoding="UTF-8"?>

<!--
  template.xsl

  Version: $Revision: 1.0 $
 
  Date: $Date: 2008/01/25 22:54:52 $

-->

<!--
    Extensible stylesheet for the Kubrick theme.
    This xsl overrides and extends the dri2xhtml of Manakin, which takes the DRI XML and produces the XHTML for a nice interface with a DSpace repository.
    Some of the overridden templates here just provide new ids and classes on tags to make the css work.  Other overridden templates provide new
    functionality just for Kubrick.  New templates provide new functionality just for Kubrick.
    The purpose of each template is indicated in comments preceding and sometimes inside the template.
        
    Author: Alexey Maslov
    Author: James Creel
    
-->    

<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:mets="http://www.loc.gov/METS/" xmlns:xlink="http://www.w3.org/TR/xlink/"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim" xmlns:xhtml="http://www.w3.org/1999/xhtml" xmlns:mods="http://www.loc.gov/mods/v3"
    xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns="http://www.w3.org/1999/xhtml" exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc">

    <xsl:import href="../dri2xhtml.xsl"/>
    <xsl:import href="./lib/xsl/date.month-name.template.xsl"/>
    <xsl:import href="./lib/xsl/date.day-in-month.template.xsl"/>
    <xsl:import href="./lib/xsl/date.year.template.xsl"/>
    <xsl:output indent="yes"/>


    <!-- Overrides the normal document template to organize the structure around a particular div of class "page"-->
    <xsl:template match="dri:document">
        <html>
            <!-- First of all, build the HTML head element -->
            <xsl:call-template name="buildHead"/>
            <!-- Then proceed to the body -->
            <body>
                <!-- Here's where the specially classed div gets inserted -->
                <div id="page">
                    <!-- 
                        The header div, complete with title, subtitle, trail and other junk. The trail is 
                        built by applying a template over pageMeta's trail children. -->
                    <xsl:call-template name="buildHeader"/>

                    <!-- 
                        Goes over the document tag's children elements: body, options, meta. The body template
                        generates the ds-body div that contains all the content. The options template generates
                        the ds-options div that contains the navigation and action options available to the 
                        user. The meta element is ignored since its contents are not processed directly, but 
                        instead referenced from the different points in the document. -->
                    <xsl:apply-templates/>

                    <!-- 
                        The footer div, dropping whatever extra information is needed on the page. It will
                        most likely be something similar in structure to the currently given example. -->
                    <xsl:call-template name="buildFooter"/>

                </div>
            </body>
        </html>
    </xsl:template>





    <!-- Overrides the default footer to put in a div with id "footer" -->
    <!-- Like the header, the footer contains various miscellanious text, links, and image placeholders -->
    <xsl:template name="buildFooter">
        <div id="footer">
            <div id="ds-footer-links">
                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                        <xsl:text>/contact</xsl:text>
                    </xsl:attribute>
                    <i18n:text>xmlui.dri2xhtml.structural.contact-link</i18n:text>
                </a>
            </div>
        </div>
    </xsl:template>


    <!--  Overrides the default body template to create a content div of class narrowcolumn
    The template to handle the dri:body element. It simply creates the ds-body div and applies 
    templates of the body's child elements (which consists entirely of dri:div tags).
    -->
    <xsl:template match="dri:body">
        <div id="content" class="narrowcolumn">
            <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='alert'][@qualifier='message']">
                <div id="ds-system-wide-alert">
                    <p>
                        <xsl:copy-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='alert'][@qualifier='message']/node()"/>
                    </p>
                </div>
            </xsl:if>
            <xsl:apply-templates/>
        </div>
    </xsl:template>


    <!-- 
        The template to handle dri:options. Since it contains only dri:list tags (which carry the actual
        information), the only things than need to be done is creating the ds-options div and applying 
        the templates inside it. 
        
        In fact, the only bit of real work this template does is add the search box, which has to be 
        handled specially in that it is not actually included in the options div, and is instead built 
        from metadata available under pageMeta.
        
       This template override makes a div with id "sidebar".   
    -->
    <xsl:template match="dri:options">
        <div id="sidebar">
            <h3 id="ds-search-option-head" class="ds-option-set-head">
                <i18n:text>xmlui.dri2xhtml.structural.search</i18n:text>
            </h3>
            <div id="ds-search-option" class="ds-option-set">
                <!-- The form, complete with a text box and a button, all built from attributes referenced
                    from under pageMeta. -->
                <form id="ds-search-form" method="post">
                    <xsl:attribute name="action">
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='simpleURL']"/>
                    </xsl:attribute>
                    <fieldset>
                        <input class="ds-text-field " type="text">
                            <xsl:attribute name="name">
                                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='queryField']"/>
                            </xsl:attribute>
                        </input>
                        <input class="ds-button-field " name="submit" type="submit" i18n:attr="value" value="xmlui.general.go">
                            <xsl:attribute name="onclick">
                                <xsl:text>
                                    var radio = document.getElementById(&quot;ds-search-form-scope-container&quot;);
                                    if (radio != undefined &amp;&amp; radio.checked)
                                    {
                                    var form = document.getElementById(&quot;ds-search-form&quot;);
                                    form.action=
                                </xsl:text>
                                <xsl:text>&quot;</xsl:text>
                                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath']"/>
                                <xsl:text>/handle/&quot; + radio.value + &quot;/search&quot; ; </xsl:text>
                                <xsl:text>
                                    } 
                                </xsl:text>
                            </xsl:attribute>
                        </input>
                        <br/>
                        <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='focus'][@qualifier='container']">
                            <label>
                                <input id="ds-search-form-scope-all" type="radio" name="scope" value="" checked="checked"/>
                                <i18n:text>xmlui.dri2xhtml.structural.search</i18n:text>
                            </label>
                            <br/>
                            <label>
                                <input id="ds-search-form-scope-container" type="radio" name="scope">
                                    <xsl:attribute name="value">
                                        <xsl:value-of select="substring-after(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='focus'][@qualifier='container'],':')"/>
                                    </xsl:attribute>
                                </input>
                                <xsl:choose>
                                    <xsl:when
                                        test="/dri:document/dri:body//dri:reference[contains(@url, substring-after(//dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='focus'][@qualifier='container'], ':'))][@type='DSpace Community']"
                                        >This Community</xsl:when>
                                    <xsl:when
                                        test="/dri:document/dri:body//dri:reference[contains(@url, substring-after(//dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='focus'][@qualifier='container'], ':'))][@type='DSpace Collection']"
                                        >This Collection</xsl:when>
                                </xsl:choose>

                            </label>
                        </xsl:if>
                    </fieldset>
                </form>
                <!-- The "Advanced Search" link, to be perched underneath the search box -->
                <a>
                    <xsl:attribute name="id">ds-search-form-advanced-search-link</xsl:attribute>
                    <xsl:attribute name="href">
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='advancedURL']"/>
                    </xsl:attribute>
                    <i18n:text>xmlui.dri2xhtml.structural.search-advanced</i18n:text>
                </a>
            </div>

            <!-- Once the search box is built, the other parts of the options are added -->
            <xsl:apply-templates/>
        </div>
    </xsl:template>


    <!-- The last thing in the structural elements section are the templates to cover the attribute calls. 
        Although, by default, XSL only parses elements and text, an explicit call to apply the attributes
        of children tags can still be made. This, in turn, requires templates that handle specific attributes,
        like the kind you see below. The chief amongst them is the pagination attribute contained by divs, 
        which creates a new div element to display pagination information. 
    
        This override prevents the display of pagination when there is only one page of results. 
    -->
    <xsl:template match="@pagination">
        <xsl:param name="position"/>
        <!-- in case of only one page of results, we'll not give pagination -->
        <xsl:if test="not(parent::node()/@pagesTotal = 1)">
            <xsl:choose>
                <xsl:when test=". = 'simple'">
                    <div class="pagination {$position}">
                        <xsl:if test="parent::node()/@previousPage">
                            <a class="previous-page-link">
                                <xsl:attribute name="href">
                                    <xsl:value-of select="parent::node()/@previousPage"/>
                                </xsl:attribute> &#171; <i18n:text>xmlui.dri2xhtml.structural.pagination-previous</i18n:text>
                            </a>
                        </xsl:if>
                        <span class="pagination-info">
                            <xsl:if test="not(parent::node()/@previousPage)">
                                <!--xsl:attribute name="style">left: 218px;</xsl:attribute-->
                            </xsl:if>
                            <i18n:translate>
                                <i18n:text>xmlui.dri2xhtml.structural.pagination-info</i18n:text>
                                <i18n:param>
                                    <xsl:value-of select="parent::node()/@firstItemIndex"/>
                                </i18n:param>
                                <i18n:param>
                                    <xsl:value-of select="parent::node()/@lastItemIndex"/>
                                </i18n:param>
                                <i18n:param>
                                    <xsl:value-of select="parent::node()/@itemsTotal"/>
                                </i18n:param>
                            </i18n:translate>
                            <!--
                                <xsl:text>Now showing items </xsl:text>
                                <xsl:value-of select="parent::node()/@firstItemIndex"/>
                                <xsl:text>-</xsl:text>
                                <xsl:value-of select="parent::node()/@lastItemIndex"/>
                                <xsl:text> of </xsl:text>
                                <xsl:value-of select="parent::node()/@itemsTotal"/>
                            -->
                        </span>
                        <xsl:if test="parent::node()/@nextPage">
                            <a class="next-page-link">
                                <xsl:attribute name="href">
                                    <xsl:value-of select="parent::node()/@nextPage"/>
                                </xsl:attribute>
                                <i18n:text>xmlui.dri2xhtml.structural.pagination-next</i18n:text> &#187; </a>
                        </xsl:if>
                    </div>
                </xsl:when>
                <xsl:when test=". = 'masked'">
                    <div class="pagination-masked {$position}">
                        <xsl:if test="not(parent::node()/@firstItemIndex = 0 or parent::node()/@firstItemIndex = 1)">
                            <a class="previous-page-link">
                                <xsl:attribute name="href">
                                    <xsl:value-of select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
                                    <xsl:value-of select="parent::node()/@currentPage - 1"/>
                                    <xsl:value-of select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
                                </xsl:attribute>
                                <i18n:text>xmlui.dri2xhtml.structural.pagination-previous</i18n:text>
                            </a>
                        </xsl:if>
                        <p class="pagination-info">
                            <i18n:translate>
                                <i18n:text>xmlui.dri2xhtml.structural.pagination-info</i18n:text>
                                <i18n:param>
                                    <xsl:value-of select="parent::node()/@firstItemIndex"/>
                                </i18n:param>
                                <i18n:param>
                                    <xsl:value-of select="parent::node()/@lastItemIndex"/>
                                </i18n:param>
                                <i18n:param>
                                    <xsl:value-of select="parent::node()/@itemsTotal"/>
                                </i18n:param>
                            </i18n:translate>
                        </p>
                        <ul class="pagination-links">
                            <xsl:if test="parent::node()/@firstItemIndex = 0 or parent::node()/@firstItemIndex = 1">
                                <!--xsl:attribute name="style">left: 265px;</xsl:attribute-->
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
                            <xsl:if test="(parent::node()/@currentPage + 4) &lt;= (parent::node()/@pagesTotal)">
                                <li class="last-page-link">
                                    <xsl:text> . . . </xsl:text>
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
                        <xsl:if test="not(parent::node()/@lastItemIndex = parent::node()/@itemsTotal)">
                            <a class="next-page-link">
                                <xsl:attribute name="href">
                                    <xsl:value-of select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
                                    <xsl:value-of select="parent::node()/@currentPage + 1"/>
                                    <xsl:value-of select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
                                </xsl:attribute>
                                <i18n:text>xmlui.dri2xhtml.structural.pagination-next</i18n:text>
                            </a>
                        </xsl:if>
                    </div>
                </xsl:when>
            </xsl:choose>
        </xsl:if>
    </xsl:template>


    <!-- Generate the metadata slider (aka popup) text about the item from the metadata section -->
    <xsl:template match="dim:dim" mode="itemMetadataPopup-DIM">
        <table class="ds-includeSet-metadata-table">
            <!-- abstract -->
            <xsl:choose>
                <xsl:when test="dim:field[@element='description' and @qualifier='abstract']">
                    <tr class="ds-table-row even">
                        <td>
                            <span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-abstract</i18n:text>:</span>
                        </td>
                        <!-- we choose to truncate the abstract and add a "..." in case it is too long. -->
                        <!-- naah, we changed or mind. -->
                        <!--
                            <xsl:choose>
                            <xsl:when test="string-length(dim:field[@element='description' and @qualifier='abstract']/child::node()) > 201">
                            <td><xsl:copy-of select="substring(dim:field[@element='description' and @qualifier='abstract']/child::node(), 0, 200)"/>...</td>
                            </xsl:when>
                            <xsl:otherwise>
                            <td><xsl:copy-of select="dim:field[@element='description' and @qualifier='abstract']/child::node()"/></td>
                            </xsl:otherwise>
                            </xsl:choose>
                        -->
                        <td>
                            <xsl:copy-of select="dim:field[@element='description' and @qualifier='abstract']/child::node()"/>
                        </td>

                    </tr>
                </xsl:when>
                <xsl:otherwise> </xsl:otherwise>
            </xsl:choose>

            <!-- description -->
            <xsl:choose>
                <xsl:when test="dim:field[@element='description' and not(@qualifier)]">
                    <tr class="ds-table-row odd">
                        <td>
                            <span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-description</i18n:text>:</span>
                        </td>
                        <td>
                            <xsl:copy-of select="dim:field[@element='description' and not(@qualifier)]/child::node()"/>
                        </td>
                    </tr>
                </xsl:when>
                <xsl:otherwise> 
            		<tr><td> &#160;  </td><td> &#160; </td></tr>
            	</xsl:otherwise>
            </xsl:choose>

            <!-- URI 
            <xsl:choose>
                <xsl:when test="dim:field[@element='identifier' and @qualifier='uri']">
                    <tr class="ds-table-row even">
                        <td>
                            <span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-uri</i18n:text>:</span>
                        </td>
                        <td>
                            <a>
                                <xsl:attribute name="href">
                                    <xsl:copy-of select="dim:field[@element='identifier' and @qualifier='uri'][1]/child::node()"/>
                                </xsl:attribute>
                                <xsl:copy-of select="dim:field[@element='identifier' and @qualifier='uri'][1]/child::node()"/>
                            </a>
                        </td>
                    </tr>
                </xsl:when>
            </xsl:choose>
	-->
        </table>

        <xsl:variable name="context" select="ancestor::mets:METS"/>
        <!-- <xsl:variable name="context" select="."/> -->
        <!-- display bitstreams -->
        <xsl:variable name="data" select="./mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim"/>

        <xsl:apply-templates select="$data" mode="detailView"/>
        <!-- First, figure out if there is a primary bitstream -->
        <!-- <xsl:variable name="primary" select="$context/mets:METS/mets:structMap[@TYPE = 'LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID" /> -->
        <xsl:variable name="primary" select="$context/mets:structMap[@TYPE = 'LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:div[@TYPE='DSpace Content Bitstream']/mets:fptr/@FILEID"/>

        <xsl:variable name="bitstream-count" select="count($context/mets:structMap[@TYPE = 'LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:div[@TYPE='DSpace Content Bitstream'])"/>
        <!-- <xsl:variable name="bitstream-count" select="count(mets:METS/mets:structMap[@TYPE = 'LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:div[@TYPE='DSpace Content Bitstream'])" /> -->

        <xsl:choose>
            <!-- If one exists only display the primary bitstream -->
            <xsl:when test="/mets:METS//dim:field[@element='relation' and @qualifier='pdf']">
                <div style="padding: 2px 0 6px 5px;clear:both;">
                <strong>Link to File: </strong>
                <a class="ellipsis">
                	<xsl:attribute name="href">
                		<xsl:value-of select="/mets:METS//dim:field[@element='relation' and @qualifier='pdf']" />
                	</xsl:attribute>
                	<xsl:value-of select="/mets:METS//dim:field[@element='relation' and @qualifier='pdf']" />
		</a>
                </div>
	    </xsl:when>
            <xsl:when test="$context/mets:METS/mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file[@ID=$primary]">
        	<h2 class="slider-files-header"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-head</i18n:text>:&#160;<xsl:value-of select="$bitstream-count"/></h2>
                <xsl:if test="$bitstream-count&lt;2">
                    <xsl:call-template name="buildBitstreamOnePrimary">
                        <xsl:with-param name="context" select="$context"/>
                        <xsl:with-param name="file" select="$context/mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file[@ID=$primary]"/>
                    </xsl:call-template>
                </xsl:if>
                <xsl:if test="$bitstream-count&gt;1">
                    <xsl:call-template name="buildBitstreamSingle">
                        <xsl:with-param name="context" select="$context"/>
                        <xsl:with-param name="file" select="$context/mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file[@ID=$primary]"/>
                    </xsl:call-template>

                    <a class="slider-bitstream-count" href="{ancestor::dri:object/@url}"> (more files) </a>
                </xsl:if>
            </xsl:when>
            <!-- Otherwise, iterate over and display some (4) of them -->
            <xsl:otherwise>
       		<div class="slider-files-header" style="clear:both;padding-left: 6px">
                <xsl:if test="$bitstream-count&lt;2">
                	<strong>File: </strong>
		</xsl:if>
                <xsl:if test="$bitstream-count&gt;1">
                	<strong>Files: </strong>
		</xsl:if>
                <xsl:for-each select="$context/mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file">
                    <xsl:sort select="./mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>
                        <xsl:call-template name="buildBitstreamSingle">
                            <xsl:with-param name="context" select="$context"/>
                        </xsl:call-template>
                </xsl:for-each>
		</div>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <!-- Utility function used by the item's summary view to list each bitstream -->
    <xsl:template name="buildBitstreamSingle">
        <xsl:param name="context" select="."/>
        <xsl:param name="file" select="."/>
        <div class="slider-bitstreams">
            <span>
                <a class="bitstream-file">
                    <xsl:attribute name="href">
                        <xsl:value-of select="$file/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                    </xsl:attribute>
                    <xsl:attribute name="title">
                        <xsl:value-of select="$file/mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>
                    </xsl:attribute>
                    <xsl:choose>
                        <xsl:when test="string-length($file/mets:FLocat[@LOCTYPE='URL']/@xlink:title) > 50">
                            <xsl:variable name="title_length" select="string-length($file/mets:FLocat[@LOCTYPE='URL']/@xlink:title)"/>
                            <xsl:value-of select="substring($file/mets:FLocat[@LOCTYPE='URL']/@xlink:title,1,15)"/>
                            <xsl:text> ... </xsl:text>
                            <xsl:value-of select="substring($file/mets:FLocat[@LOCTYPE='URL']/@xlink:title,$title_length - 25,$title_length)"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="$file/mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </a>
            </span> &#160; <!-- File size always comes in bytes and thus needs conversion -->
            <span class="bitstream-filesize">(<xsl:choose>
                    <xsl:when test="$file/@SIZE &lt; 1000">
                        <xsl:value-of select="$file/@SIZE"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-bytes</i18n:text>
                    </xsl:when>
                    <xsl:when test="$file/@SIZE &lt; 1000000">
                        <xsl:value-of select="substring(string($file/@SIZE div 1000),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-kilobytes</i18n:text>
                    </xsl:when>
                    <xsl:when test="$file/@SIZE &lt; 1000000000">
                        <xsl:value-of select="substring(string($file/@SIZE div 1000000),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-megabytes</i18n:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="substring(string($file/@SIZE div 1000000000),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-gigabytes</i18n:text>
                    </xsl:otherwise>
                </xsl:choose>) </span>
        </div>
    </xsl:template>


    <!-- Utility function used by the item's summary view the only and primary bitstream of an item -->
    <xsl:template name="buildBitstreamOnePrimary">
        <xsl:param name="context" select="."/>
        <xsl:param name="file" select="."/>
        <div class="slider-bitstreams">
            <span>
                <a class="bitstream-file">
                    <xsl:attribute name="href">
                        <xsl:value-of select="$file/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                    </xsl:attribute>
                    <xsl:attribute name="title">
                        <xsl:value-of select="$file/mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>
                    </xsl:attribute>
                    <xsl:choose>
                        <xsl:when test="string-length($file/mets:FLocat[@LOCTYPE='URL']/@xlink:title) > 50">
                            <xsl:variable name="title_length" select="string-length($file/mets:FLocat[@LOCTYPE='URL']/@xlink:title)"/>
                            <xsl:value-of select="substring($file/mets:FLocat[@LOCTYPE='URL']/@xlink:title,1,15)"/>
                            <xsl:text> ... </xsl:text>
                            <xsl:value-of select="substring($file/mets:FLocat[@LOCTYPE='URL']/@xlink:title,$title_length - 25,$title_length)"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="$file/mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </a>
            </span> &#160; <!-- File size always comes in bytes and thus needs conversion -->
            <span class="bitstream-filesize">(<xsl:choose>
                    <xsl:when test="$file/@SIZE &lt; 1000">
                        <xsl:value-of select="$file/@SIZE"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-bytes</i18n:text>
                    </xsl:when>
                    <xsl:when test="$file/@SIZE &lt; 1000000">
                        <xsl:value-of select="substring(string($file/@SIZE div 1000),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-kilobytes</i18n:text>
                    </xsl:when>
                    <xsl:when test="$file/@SIZE &lt; 1000000000">
                        <xsl:value-of select="substring(string($file/@SIZE div 1000000),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-megabytes</i18n:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="substring(string($file/@SIZE div 1000000000),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-gigabytes</i18n:text>
                    </xsl:otherwise>
                </xsl:choose>) </span>
        </div>
    </xsl:template>


    <!-- We resolve the reference tag to an external mets object -->
    <xsl:template match="dri:reference" mode="summaryList">
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!-- Since this is a summary only grab the descriptive metadata, and the thumbnails -->
            <!-- actually, we want the details for the slider/popup metadata -->
            <!-- <xsl:text>?sections=dmdSec,fileSec&amp;fileGrpTypes=THUMBNAIL</xsl:text> -->
        </xsl:variable>

        <li>
            <div>
                <xsl:attribute name="class">
                    <xsl:text>ds-artifact-item </xsl:text>
                    <xsl:choose>
                        <xsl:when test="position() mod 2 = 0">even</xsl:when>
                        <xsl:otherwise>odd</xsl:otherwise>
                    </xsl:choose>
                </xsl:attribute>

                <xsl:apply-templates select="document($externalMetadataURL)" mode="summaryList"/>
                <xsl:apply-templates/>

            </div>
        </li>
    </xsl:template>


    <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']]" mode="metadataPopup">
        <xsl:choose>
            <xsl:when test="@LABEL='DSpace Item'">
                <xsl:call-template name="itemMetadataPopup-DIM"/>
            </xsl:when>
            <!-- The following calls are to templates not implemented yet (if ever - what need for sliders/popups on containers?)
                <xsl:when test="@LABEL='DSpace Collection'">
                <xsl:call-template name="collectionMetadataPopup-DIM"/>
                </xsl:when>
                <xsl:when test="@LABEL='DSpace Community'">
                <xsl:call-template name="communityMetadataPopup-DIM"/>
                </xsl:when>
            -->
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <!-- A metadata popup for an item rendered in the summaryList pattern.  -->
    <xsl:template name="itemMetadataPopup-DIM">
        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim" mode="itemMetadataPopup-DIM"/>
        <!-- Generate the thunbnail, if present, from the file section -->
        <!-- <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']" mode="itemMetadataPopup-DIM"/> -->
    </xsl:template>


    <!-- Generate the thunbnail, if present, from the file section -->
    <xsl:template match="mets:fileGrp[@USE='THUMBNAIL']" mode="itemMetadataPopup-DIM">
        <div class="popup-artifact-preview">
            <!-- manakin-voss version: <a href="{ancestor::mets:METS/@OBJID}"> -->
            <a href="{ancestor::dri:object/@url}">
                <img alt="Thumbnail">
                    <xsl:attribute name="src">
                        <xsl:value-of select="mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                    </xsl:attribute>
                </img>
            </a>
        </div>
    </xsl:template>


    <!-- Generate the thunbnail, if present, from the file section -->
    <xsl:template match="mets:fileGrp[@USE='THUMBNAIL']" mode="itemMetadataPopup-DIM">
        <div class="popup-artifact-preview">
            <a href="{ancestor::mets:METS/@OBJID}">
                <img alt="Thumbnail">
                    <xsl:attribute name="src">
                        <xsl:value-of select="mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                    </xsl:attribute>
                </img>
            </a>
        </div>
    </xsl:template>






    <!-- 
        The trail is built one link at a time. Each link is given the ds-trail-link class, with the first and
        the last links given an additional descriptor. 
        
        This overriding template is here to add a class to the links in the breadcrumb trail, 
        and to add a little arrow after non-ending (not the last) items in the trail.
    -->
    <xsl:template match="dri:trail">
        <li>
            <xsl:attribute name="class">
                <xsl:text>ds-trail-link </xsl:text>
                <xsl:if test="position()=1">
                    <xsl:text>first-link</xsl:text>
                </xsl:if>
                <xsl:if test="position()=last()">
                    <xsl:text>last-link</xsl:text>
                </xsl:if>
            </xsl:attribute>
            <!-- Determine whether we are dealing with a link or plain text trail link -->
            <xsl:choose>
                <xsl:when test="./@target">
                    <a class="trail_anchor">
                        <xsl:attribute name="href">
                            <xsl:value-of select="./@target"/>
                        </xsl:attribute>
                        <xsl:apply-templates/>
                    </a>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates/>
                </xsl:otherwise>
            </xsl:choose>
            <!-- put in a little arrow '>' if this is not the last item in the trail -->
            <xsl:if test="not(position()=last())">
                <span style="color: white; font-size: 110%;">&#8594;</span>
            </xsl:if>
        </li>
    </xsl:template>





    <!-- We resolve the reference tag to an external mets object, and depending on the label of that object we will put in a metadata slider.-->
    <xsl:template name="itemSummaryList-DIM">

        <div>
            <xsl:choose>
                <xsl:when test="@LABEL='DSpace Item'">
                    <xsl:attribute name="class">
                        <xsl:text>ds-artifact-item-with-popup </xsl:text>
                    </xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="class">
                        <xsl:text>ds-artifact-item </xsl:text>
                    </xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>

            <!-- Generate the info about the item from the metadata section -->
            <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim" mode="itemSummaryList-DIM"/>
            <!-- Generate the thunbnail, if present, from the file section -->
            <xsl:apply-templates select="./mets:fileSec" mode="artifact-preview"/>


            <xsl:if test="@LABEL='DSpace Item'">
               <xsl:apply-templates select="." mode="metadataPopup"/>
            </xsl:if>

        </div>
    </xsl:template>









    <!-- Generate the info about the item from the metadata section (overrides a template in DIM-Handler in order to better format the date) -->
    <xsl:template match="dim:dim" mode="itemSummaryList-DIM">
        <xsl:param name="longsight-month">
            <xsl:call-template name="month-name">
                <xsl:with-param name="date-time" select="dim:field[@element='date' and @qualifier='issued']/node()"/>
            </xsl:call-template>
        </xsl:param>
        <xsl:param name="longsight-day-in-month">
            <xsl:call-template name="day-in-month">
                <xsl:with-param name="date-time" select="dim:field[@element='date' and @qualifier='issued']/node()"/>
            </xsl:call-template>
        </xsl:param>
        <xsl:param name="longsight-year">
            <xsl:call-template name="year">
                <xsl:with-param name="date-time" select="dim:field[@element='date' and @qualifier='issued']/node()"/>
            </xsl:call-template>
        </xsl:param>

        <div class="artifact-description">
            <div class="artifact-title">
                <a href="{ancestor::mets:METS/@OBJID}">
                    <xsl:choose>
                        <xsl:when test="dim:field[@element='title']">
                            <xsl:value-of select="dim:field[@element='title'][1]/node()"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </a>
            </div>
            <div class="artifact-info">
                <span class="author">
                    <xsl:choose>
                        <xsl:when test="dim:field[@element='audience']">
                            <xsl:for-each select="dim:field[@element='audience']">
                                <xsl:copy-of select="node()"/>
                                <xsl:if test="count(following-sibling::dim:field[@element='audience']) != 0">
                                    <xsl:text>; </xsl:text>
                                </xsl:if>
                            </xsl:for-each>
                        </xsl:when>
                        <xsl:when test="dim:field[@element='contributor'][@qualifier='author']">
                            <xsl:for-each select="dim:field[@element='contributor'][@qualifier='author']">
                                <xsl:copy-of select="./node()"/>
                                <xsl:if test="count(following-sibling::dim:field[@element='contributor'][@qualifier='author']) != 0">
                                    <xsl:text>; </xsl:text>
                                </xsl:if>
                            </xsl:for-each>
                        </xsl:when>
                        <xsl:when test="dim:field[@element='contributor']">
                            <xsl:for-each select="dim:field[@element='contributor']">
                                <xsl:copy-of select="node()"/>
                                <xsl:if test="count(following-sibling::dim:field[@element='contributor']) != 0">
                                    <xsl:text>; </xsl:text>
                                </xsl:if>
                            </xsl:for-each>
                        </xsl:when>
                        <xsl:otherwise>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-author</i18n:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </span>
                <xsl:text> </xsl:text>
                <span class="publisher-date">
                    <xsl:text>(</xsl:text>
                    <xsl:if test="dim:field[@element='publisher']">
                        <span class="publisher">
                            <xsl:copy-of select="dim:field[@element='publisher']/node()"/>
                        </span>
                        <xsl:text>, </xsl:text>
                    </xsl:if>
                    <span class="date">
                        <!--
		    	<xsl:value-of select="substring(dim:field[@element='date' and @qualifier='issued']/node(),1,10)"/>
                        -->
                        <xsl:if test="$longsight-month != 'NaN' ">
                            <xsl:value-of select="$longsight-month"/>
                            <xsl:if test="$longsight-day-in-month != 'NaN'">
                                <xsl:text> </xsl:text>
                                </xsl:if>
                        </xsl:if>
                        <xsl:if test="$longsight-day-in-month != 'NaN'">
                            <xsl:value-of select="$longsight-day-in-month"/>

                        </xsl:if>
                        <xsl:if test="$longsight-month != ''">
                            <xsl:text>, </xsl:text>
                        </xsl:if>
                        <xsl:if test="$longsight-year != 'NaN'">
                            <xsl:value-of select="$longsight-year"/>
                        </xsl:if>

                    </span>
                    <xsl:text>)</xsl:text>
                </span>
            </div>
        </div>

    </xsl:template>

    <xsl:template name="itemSummaryView-DIM">
        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
        mode="itemSummaryView-DIM"/>
        
        <!-- Generate the bitstream information from the file section -->
        <xsl:choose>
            <xsl:when test="/mets:METS//dim:field[@element='relation' and @qualifier='pdf']">
                <h2>Link to File</h2>
                <div style="margin: 30px">
                <a>
                	<xsl:attribute name="href">
                		<xsl:value-of select="/mets:METS//dim:field[@element='relation' and @qualifier='pdf']" />
                	</xsl:attribute>
                	<xsl:value-of select="/mets:METS//dim:field[@element='relation' and @qualifier='pdf']" />
		</a>
		</div>

	    </xsl:when>
            <xsl:when test="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']">
                <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']">
                    <xsl:with-param name="context" select="."/>
                    <xsl:with-param name="primaryBitstream" select="./mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"/>
                </xsl:apply-templates>
            </xsl:when>
            <!-- Special case for handling ORE resource maps stored as DSpace bitstreams -->
            <xsl:when test="./mets:fileSec/mets:fileGrp[@USE='ORE']">
                <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='ORE']"/>
            </xsl:when>
            <xsl:otherwise>
                <h2><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-head</i18n:text></h2> 
                <table class="ds-table file-list">
                    <tr class="ds-table-header-row">
                        <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-file</i18n:text></th>
                        <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-size</i18n:text></th>
                        <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-format</i18n:text></th>
                        <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-view</i18n:text></th>
                    </tr>
                    <tr>
                        <td colspan="4">
                            <p><i18n:text>xmlui.dri2xhtml.METS-1.0.item-no-files</i18n:text></p>
                        </td>
                    </tr>
                </table>
            </xsl:otherwise>
        </xsl:choose>

        <!-- Generate the Creative Commons license information from the file section (DSpace deposit license hidden by default)-->
        <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CC-LICENSE' or @USE='LICENSE']"/>

    </xsl:template>

    <!-- Generate the info about the item from the metadata section -->
    <xsl:template match="dim:dim" mode="itemSummaryView-DIM">
        <table class="ds-includeSet-table">
            <!--
            <tr class="ds-table-row odd">
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-preview</i18n:text>:</span></td>
                <td>
                    <xsl:choose>
                        <xsl:when test="mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']">
                            <a class="image-link">
                                <xsl:attribute name="href"><xsl:value-of select="@OBJID"/></xsl:attribute>
                                <img alt="Thumbnail">
                                    <xsl:attribute name="src">
                                        <xsl:value-of select="mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/
                                            mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                                    </xsl:attribute>
                                </img>
                            </a>
                        </xsl:when>
                        <xsl:otherwise>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-preview</i18n:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </td>
            </tr>-->
            <tr class="ds-table-row even">
                <td>
                    <span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-title</i18n:text>: </span>
                </td>
                <td>
                    <xsl:choose>
                        <xsl:when test="count(dim:field[@element='title'][not(@qualifier)]) &gt; 1">
                            <xsl:for-each select="dim:field[@element='title'][not(@qualifier)]">
                                <xsl:value-of select="./node()"/>
                                <xsl:if test="count(following-sibling::dim:field[@element='title'][not(@qualifier)]) != 0">
                                    <xsl:text>; </xsl:text>
                                    <br/>
                                </xsl:if>
                            </xsl:for-each>
                        </xsl:when>
                        <xsl:when test="count(dim:field[@element='title'][not(@qualifier)]) = 1">
                            <xsl:value-of select="dim:field[@element='title'][not(@qualifier)][1]/node()"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </td>
            </tr>
            <xsl:if test="dim:field[@element='contributor'][@qualifier='author'] or dim:field[@element='creator'] or dim:field[@element='contributor']">
                <tr class="ds-table-row odd">
                    <td>
                        <span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-author</i18n:text>:</span>
                    </td>
                    <td>
                        <xsl:choose>
                            <xsl:when test="dim:field[@element='contributor'][@qualifier='author']">
                                <xsl:for-each select="dim:field[@element='contributor'][@qualifier='author']">
                                    <xsl:copy-of select="node()"/>
                                    <xsl:if test="count(following-sibling::dim:field[@element='contributor'][@qualifier='author']) != 0">
                                        <xsl:text>; </xsl:text>
                                    </xsl:if>
                                </xsl:for-each>
                            </xsl:when>
                            <xsl:when test="dim:field[@element='creator']">
                                <xsl:for-each select="dim:field[@element='creator']">
                                    <xsl:copy-of select="node()"/>
                                    <xsl:if test="count(following-sibling::dim:field[@element='creator']) != 0">
                                        <xsl:text>; </xsl:text>
                                    </xsl:if>
                                </xsl:for-each>
                            </xsl:when>
                            <xsl:when test="dim:field[@element='contributor']">
                                <xsl:for-each select="dim:field[@element='contributor']">
                                    <xsl:copy-of select="node()"/>
                                    <xsl:if test="count(following-sibling::dim:field[@element='contributor']) != 0">
                                        <xsl:text>; </xsl:text>
                                    </xsl:if>
                                </xsl:for-each>
                            </xsl:when>
                            <xsl:otherwise>
                                <i18n:text>xmlui.dri2xhtml.METS-1.0.no-author</i18n:text>
                            </xsl:otherwise>
                        </xsl:choose>
                    </td>
                </tr>
            </xsl:if>
            <xsl:if test="dim:field[@element='description' and @qualifier='abstract']">
                <tr class="ds-table-row even">
                    <td>
                        <span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-abstract</i18n:text>:</span>
                    </td>
                    <td>
                        <xsl:if test="count(dim:field[@element='description' and @qualifier='abstract']) &gt; 1">
                            <hr class="metadata-seperator"/>
                        </xsl:if>
                        <xsl:for-each select="dim:field[@element='description' and @qualifier='abstract']">
                            <xsl:copy-of select="./node()"/>
                            <xsl:if test="count(following-sibling::dim:field[@element='description' and @qualifier='abstract']) != 0">
                                <hr class="metadata-seperator"/>
                            </xsl:if>
                        </xsl:for-each>
                        <xsl:if test="count(dim:field[@element='description' and @qualifier='abstract']) &gt; 1">
                            <hr class="metadata-seperator"/>
                        </xsl:if>
                    </td>
                </tr>
            </xsl:if>
            <xsl:if test="dim:field[@element='description' and not(@qualifier)]">
                <tr class="ds-table-row odd">
                    <td>
                        <span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-description</i18n:text>:</span>
                    </td>
                    <td>
                        <xsl:if test="count(dim:field[@element='description' and not(@qualifier)]) &gt; 1 and not(count(dim:field[@element='description' and @qualifier='abstract']) &gt; 1)">
                            <hr class="metadata-seperator"/>
                        </xsl:if>
                        <xsl:for-each select="dim:field[@element='description' and not(@qualifier)]">
                            <xsl:copy-of select="./node()"/>
                            <xsl:if test="count(following-sibling::dim:field[@element='description' and not(@qualifier)]) != 0">
                                <hr class="metadata-seperator"/>
                            </xsl:if>
                        </xsl:for-each>
                        <xsl:if test="count(dim:field[@element='description' and not(@qualifier)]) &gt; 1">
                            <hr class="metadata-seperator"/>
                        </xsl:if>
                    </td>
                </tr>
            </xsl:if>
            <xsl:if test="dim:field[@element='identifier' and @qualifier='uri']">
                <tr class="ds-table-row even">
                    <td>
                        <span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-uri</i18n:text>:</span>
                    </td>
                    <td>
                        <xsl:for-each select="dim:field[@element='identifier' and @qualifier='uri']">
                            <a>
                                <xsl:attribute name="href">
                                    <xsl:copy-of select="./node()"/>
                                </xsl:attribute>
                                <xsl:copy-of select="./node()"/>
                            </a>
                            <xsl:if test="count(following-sibling::dim:field[@element='identifier' and @qualifier='uri']) != 0">
                                <br/>
                            </xsl:if>
                        </xsl:for-each>
                    </td>
                </tr>
            </xsl:if>
            <xsl:if test="dim:field[@element='date' and @qualifier='issued']">
                <tr class="ds-table-row odd">
                    <td>
                        <span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-date</i18n:text>:</span>
                    </td>
                    <td>
                        <xsl:for-each select="dim:field[@element='date' and @qualifier='issued']">
                            <xsl:copy-of select="substring(./node(),1,10)"/>
                            <xsl:if test="count(following-sibling::dim:field[@element='date' and @qualifier='issued']) != 0">
                                <br/>
                            </xsl:if>
                        </xsl:for-each>
                    </td>
                </tr>
            </xsl:if>
            <xsl:if test="dim:field[@element='audience' and not(@qualifier)]">
                <tr class="ds-table-row even">
                    <td>
                        <span class="bold">Audience:</span>
                    </td>
                    <td>
                        <xsl:for-each select="dim:field[@element='audience' and not(@qualifier)]">
                            <xsl:copy-of select="./node()"/>
                            <xsl:if test="count(following-sibling::dim:field[@element='audience' and not(@qualifier)]) != 0">
                                <br/>
                            </xsl:if>
                        </xsl:for-each>
                    </td>
                </tr>
            </xsl:if>
        </table>
    </xsl:template>

    <!-- Build a single row in the bitsreams table of the item view page -->
    <xsl:template match="mets:file">
        <xsl:param name="context" select="."/>
        <tr>
            <xsl:attribute name="class">
                <xsl:text>ds-table-row </xsl:text>
                <xsl:if test="(position() mod 2 = 0)">even </xsl:if>
                <xsl:if test="(position() mod 2 = 1)">odd </xsl:if>
            </xsl:attribute>
            <td>
                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                    </xsl:attribute>
                    <xsl:attribute name="title">
                        <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>
                    </xsl:attribute>
                    <xsl:choose>
                        <xsl:when test="string-length(mets:FLocat[@LOCTYPE='URL']/@xlink:title) > 50">
                            <xsl:variable name="title_length" select="string-length(mets:FLocat[@LOCTYPE='URL']/@xlink:title)"/>
                            <xsl:value-of select="substring(mets:FLocat[@LOCTYPE='URL']/@xlink:title,1,15)"/>
                            <xsl:text> ... </xsl:text>
                            <xsl:value-of select="substring(mets:FLocat[@LOCTYPE='URL']/@xlink:title,$title_length - 25,$title_length)"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </a>
            </td>
            <!-- File size always comes in bytes and thus needs conversion --> 
            <td>
                <xsl:choose>
                    <xsl:when test="@SIZE &lt; 1024">
                        <xsl:value-of select="@SIZE"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-bytes</i18n:text>
                    </xsl:when>
                    <xsl:when test="@SIZE &lt; 1024 * 1024">
                        <xsl:value-of select="substring(string(@SIZE div 1024),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-kilobytes</i18n:text>
                    </xsl:when>
                    <xsl:when test="@SIZE &lt; 1024 * 1024 * 1024">
                        <xsl:value-of select="substring(string(@SIZE div (1024 * 1024)),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-megabytes</i18n:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="substring(string(@SIZE div (1024 * 1024 * 1024)),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-gigabytes</i18n:text>
                    </xsl:otherwise>
                </xsl:choose>
            </td>
            <!-- Lookup File Type description in local messages.xml based on MIME Type.
                In the original DSpace, this would get resolved to an application via
                the Bitstream Registry, but we are constrained by the capabilities of METS
                and can't really pass that info through. -->
            <td>
              <xsl:call-template name="getFileTypeDesc">
                <xsl:with-param name="mimetype">
                  <xsl:value-of select="substring-before(@MIMETYPE,'/')"/>
                  <xsl:text>/</xsl:text>
                  <xsl:value-of select="substring-after(@MIMETYPE,'/')"/>
                </xsl:with-param>
              </xsl:call-template>
            </td>
            <td>
                <xsl:choose>
                    <xsl:when test="$context/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/
                        mets:file[@GROUPID=current()/@GROUPID]">
                        <a class="image-link cloud-zoom" rel="adjustX: -10, adjustY:-100, zoomWidth: 500, zoomHeight: 500, position: 'left', tint: '#ff0000', tintOpacity: 0.5">
                            <xsl:attribute name="href">
                                <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                            </xsl:attribute>
                            <img alt="Thumbnail">
                                <xsl:attribute name="src">
                                    <xsl:value-of select="$context/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/
                                        mets:file[@GROUPID=current()/@GROUPID]/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                                </xsl:attribute>
                            </img>
                        </a>
                    </xsl:when>
                    <xsl:otherwise>
                        <a>
                            <xsl:attribute name="href">
                                <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                            </xsl:attribute>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-viewOpen</i18n:text>
                        </a>
                    </xsl:otherwise>
                </xsl:choose>                        
            </td>
	    <!-- Display the contents of 'Description' as long as at least one bitstream contains a description -->
	    <xsl:if test="$context/mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file/mets:FLocat/@xlink:label != ''">
	        <td>
	            <xsl:value-of select="mets:FLocat[@LOCTYPE='URL']/@xlink:label"/>
	        </td>
	    </xsl:if>

        </tr>
    </xsl:template>

</xsl:stylesheet>
