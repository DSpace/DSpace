<?xml version="1.0" encoding="UTF-8"?>
<!--  
    Author: Alexey Maslov
-->
<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:mets="http://www.loc.gov/METS/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim" xmlns:mods="http://www.loc.gov/mods/v3" xmlns:xlink="http://www.w3.org/TR/xlink/" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    
    <xsl:import href="../dri2xhtml.xsl"/>
    <xsl:output indent="yes"/>
    
    
    
    
    <xsl:template match="dri:document">
        <html>
            <!-- First of all, build the HTML head element -->
            <xsl:call-template name="buildHead"/>
            <!-- Then proceed to the body -->
            <body>
                
                <div id="ds-main">
                    <!-- 
                        The header div, complete with title, subtitle, trail and other junk. The trail is 
                        built by applying a template over pageMeta's trail children. -->
                    <xsl:call-template name="buildHeader"/>
                    
                    <div id="below-header">
                        
                        <xsl:choose>
                            <xsl:when test="/dri:document/dri:meta/dri:userMeta/@authenticated = 'yes'">
                                <div id="ds-user-box">
                                    <p>
                                        <a>
                                            <xsl:attribute name="href">
                                                <xsl:value-of select="/dri:document/dri:meta/dri:userMeta/                                                     dri:metadata[@element='identifier' and @qualifier='url']"/>
                                            </xsl:attribute>
                                            <i18n:text>xmlui.dri2xhtml.structural.profile</i18n:text>
                                            <xsl:value-of select="/dri:document/dri:meta/dri:userMeta/                                                 dri:metadata[@element='identifier' and @qualifier='firstName']"/>
                                            <xsl:text> </xsl:text>
                                            <xsl:value-of select="/dri:document/dri:meta/dri:userMeta/                                                 dri:metadata[@element='identifier' and @qualifier='lastName']"/>
                                        </a>
                                        <xsl:text> | </xsl:text>
                                        <a>
                                            <xsl:attribute name="href">
                                                <xsl:value-of select="/dri:document/dri:meta/dri:userMeta/                                                     dri:metadata[@element='identifier' and @qualifier='logoutURL']"/>
                                            </xsl:attribute>
                                            <i18n:text>xmlui.dri2xhtml.structural.logout</i18n:text>
                                        </a>
                                    </p>
                                </div>
                            </xsl:when>
                            <xsl:otherwise>
                                <div id="ds-user-box">
                                    <p>
                                        <a>
                                            <xsl:attribute name="href">
                                                <xsl:value-of select="/dri:document/dri:meta/dri:userMeta/                                                 dri:metadata[@element='identifier' and @qualifier='loginURL']"/>
                                            </xsl:attribute>
                                            <i18n:text>xmlui.dri2xhtml.structural.login</i18n:text>
                                        </a>
                                    </p>
                                </div>
                            </xsl:otherwise>
                        </xsl:choose>
                        
                        <div id="mainNav">
                            <ul id="ds-trail">
                                <xsl:apply-templates select="/dri:document/dri:meta/dri:pageMeta/dri:trail">
                                    <xsl:with-param name="number_of_trail_items" select="count(/dri:document/dri:meta/dri:pageMeta/dri:trail)"/>
                                </xsl:apply-templates>
                            </ul>
                        </div>
                        
                        <!-- 
                            Goes over the document tag's children elements: body, options, meta. The body template
                            generates the ds-body div that contains all the content. The options template generates
                            the ds-options div that contains the navigation and action options available to the 
                            user. The meta element is ignored since its contents are not processed directly, but 
                            instead referenced from the different points in the document. -->
                        <xsl:apply-templates/>
                        
                        
                        <div class="spacer"> </div>
                        
                        <!-- 
                            The footer div, dropping whatever extra information is needed on the page. It will
                            most likely be something similar in structure to the currently given example. -->
                        <xsl:call-template name="buildFooter"/>
                        
                    </div> 
                </div>
            </body>
        </html>
    </xsl:template>
    
    
    <xsl:template match="dri:body">
        <!-- test will determine if we are on the front page -->
        <xsl:variable name="front_page_searches" select="*[@n='front-page-search']"/>
        
        <div id="ds-body">
            <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='alert'][@qualifier='message']">
                <div id="ds-system-wide-alert">
                    <p>
                        <xsl:copy-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='alert'][@qualifier='message']/node()"/>
                    </p>
                </div>
            </xsl:if>
            
            <xsl:if test="count($front_page_searches) &gt;= 1">
                <div id="front-page-image-wrapper">
                    <img src="{$theme-path}/small_esl.jpg"/>
                </div>
            </xsl:if>
            
            <xsl:apply-templates/>

        </div>        
    </xsl:template>
     
       
    <xsl:template name="buildFooter">
        <div id="ds-footer">
            
            <div id="ds-footer-internal-wrapper">
                <a id="ds-footer-logo-anchor" href="http://www.example.org">
                    <!-- <span id="ds-footer-logo">&#130;</span> -->
                    <img id="ds-footer-logo" src="{$theme-path}/logo_footer-1.jpg"/>
                </a>
                <p>
                    <a href="mailto:webmaster@example.org" class="footer">Webmaster</a> |
                    
                    <br/>
                    
                    <a href="http://example.org/" target="_blank" class="footer">Example Organization</a> |
                </p>
                <div id="ds-footer-links">
                    <a>
                        <xsl:attribute name="href">
                            <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                            <xsl:text>/contact</xsl:text>
                        </xsl:attribute>
                        <i18n:text>xmlui.dri2xhtml.structural.contact-link</i18n:text>
                    </a>
                    <xsl:text> | </xsl:text>
                    <a onblur="alert('bob');">
                        <xsl:attribute name="href">
                            <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                            <xsl:text>/feedback</xsl:text>
                        </xsl:attribute>
                        <i18n:text>xmlui.dri2xhtml.structural.feedback-link</i18n:text>
                    </a>
                </div>
            </div>
            
            
            <div id="footerCap"/>
            
        </div>
        <!-- <div class="spacer"/> -->
        <!--                    
            <a href="http://di.tamu.edu">                            
            <div id="ds-footer-logo"></div>
            </a>
            <p>
            This website is using Manakin, a new front end for DSpace created by Texas A&amp;M University 
            Libraries. The interface can be extensively modified through Manakin Aspects and XSL based Themes. 
            For more information visit 
            <a href="http://di.tamu.edu">http://di.tamu.edu</a> and
            <a href="http://dspace.org">http://dspace.org</a>                            
            </p>
        -->
    </xsl:template>



 <xsl:template name="buildHeader">
     <div id="headerWrapper">
            <div id="ds-header">
                <a id="ds-header-logo-anchor">
                    <xsl:attribute name="href">
                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                    </xsl:attribute>
                    <!-- <span id="ds-header-logo">&#130;</span> -->
                    <img id="ds-header-logo" src="{$theme-path}/logo-1.jpg"/>
                </a>
                <em id="repository-label">Digital</em>
                <h1><xsl:copy-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='title']/node()"/></h1>
                <h2><i18n:text>xmlui.dri2xhtml.structural.head-subtitle</i18n:text></h2>
           
           
                <!-- The form, complete with a text box and a button, all built from attributes referenced
                    from under pageMeta. -->
                
                <div id="searchbar">
                    
                    <!-- <h3 id="ds-search-option-head" class="ds-option-set-head"><i18n:text>xmlui.dri2xhtml.structural.search</i18n:text></h3> -->
                    <h3 id="ds-search-option-head" class="ds-option-set-head">Search Repository</h3>
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
                            
                            
                            <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='focus'][@qualifier='container']">
                                <select id="ds-search-form-scope-container" style="width:168px;">
                                    <option selected="selected" value="/"><i18n:text>xmlui.dri2xhtml.structural.search</i18n:text>
                                    </option>
                                    
                                    <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='focus'][@qualifier='container']">
                                        <xsl:variable name="focus">
                                            <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='focus' and @qualifier='container']"/>
                                        </xsl:variable>
                                        <option>
                                            <xsl:attribute name="value">
                                                <xsl:value-of select="substring-after($focus,':')"/>
                                            </xsl:attribute>       
                                            <xsl:choose>
                                                <xsl:when test="/dri:document/dri:body//dri:reference[contains(@url, substring-after(//dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='focus'][@qualifier='container'], ':'))][@type='DSpace Community']">This Community</xsl:when>
                                                <xsl:when test="/dri:document/dri:body//dri:reference[contains(@url, substring-after(//dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='focus'][@qualifier='container'], ':'))][@type='DSpace Collection']">This Collection</xsl:when>
                                                <xsl:otherwise><!--ERROR--></xsl:otherwise>
                                            </xsl:choose>
                                            
                                        </option>
                                    </xsl:if>
                                </select>
                            </xsl:if>
                            
                            <input class="ds-button-field " name="submit" type="submit" i18n:attr="value" value="xmlui.general.go">
                                <xsl:attribute name="onclick">
                                    <xsl:text>
                                        var dropdown = document.getElementById("ds-search-form-scope-container");
                                        
                                        if (dropdown != undefined &amp;&amp; dropdown.value != "/")
                                        {
                                        var form = document.getElementById("ds-search-form");
                                        form.action=
                                    </xsl:text>
                                    <xsl:text>"</xsl:text>
                                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath']"/>
                                    <xsl:text>/handle/" + dropdown.value + "/search" ; </xsl:text>
                                    <xsl:text>
                                        }										
                                    </xsl:text>
                                </xsl:attribute>
                            </input>
                            
                        </fieldset>
                    </form>
                    
                    
                    <!-- The "Advanced search" link, to be perched underneath the search box -->
                    <a id="advanced-search-link">
                        <xsl:attribute name="href">
                            <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='advancedURL']"/>
                        </xsl:attribute>
                        <i18n:text>xmlui.dri2xhtml.structural.search-advanced</i18n:text>
                    </a> 
                    
                </div><!-- end #searchbar -->		
                
            </div>
         </div>
 </xsl:template>
    
    
    <!-- 
        The template to handle dri:options. Since it contains only dri:list tags (which carry the actual
        information), the only things than need to be done is creating the ds-options div and applying 
        the templates inside it. 
    -->    
    <xsl:template match="dri:options">
        <!-- changed the id of the following div from ds-options to navigation -->
        <div id="ds-options">			
            <xsl:apply-templates/>
        </div>
    </xsl:template>
    
    
    
    
    <!-- grabs only the summaryList found on the community/collection overview page -->
    <xsl:template match="dri:referenceSet[@type = 'summaryList'][@id='aspect.artifactbrowser.CommunityBrowser.referenceSet.community-browser']" priority="2">
        
        <xsl:apply-templates select="dri:head"/>
        <!-- Here we decide whether we have a hierarchical list or a flat one -->
        <xsl:choose>
            <xsl:when test="descendant-or-self::dri:referenceSet/@rend='hierarchy' or ancestor::dri:referenceSet/@rend='hierarchy'">
                <p id="expand_all_clicker" style="display: none">[Expand All]</p>
                <p id="collapse_all_clicker" style="display: none">[Collapse All]</p>
                <ul>
                    <xsl:apply-templates select="*[not(name()='head')]" mode="summaryList"/>
                </ul>
            </xsl:when>
            <xsl:otherwise>
                <ul class="ds-artifact-list">
                    <xsl:apply-templates select="*[not(name()='head')]" mode="summaryList"/>
                </ul>
            </xsl:otherwise>
        </xsl:choose>        
    </xsl:template>
    
    <!-- A community rendered in the summaryList pattern. Encountered on the community-list and on 
        on the front page. -->
    <xsl:template name="communitySummaryList-DIM">
        <xsl:variable name="data" select="./mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim"/>
        <p class="ListPlus" style="display:none">[+]</p>
        <p class="ListMinus">[-]</p>
        <span class="bold">
            <a href="{@OBJID}" class="communitySummaryListAnchorDIM">
                <xsl:choose>
                    <xsl:when test="string-length($data/dim:field[@element='title'][1]) &gt; 0">
                        <xsl:value-of select="$data/dim:field[@element='title'][1]"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                    </xsl:otherwise>
                </xsl:choose>
            </a>
        </span>
    </xsl:template>
    
    <!-- A community rendered in the summaryList pattern. Encountered on the community-list and on 
        on the front page. -->
    <xsl:template name="communitySummaryList-MODS">
        <xsl:variable name="data" select="./mets:dmdSec/mets:mdWrap/mets:xmlData/mods:mods"/>
        <p class="ListPlus" style="display:none">[+]</p>
        <p class="ListMinus">[-]</p>
        <span class="bold">
            <a href="{@OBJID}" class="communitySummaryListAnchorMODS">
                <xsl:value-of select="$data/mods:titleInfo/mods:title"/>
            </a>
        </span>
    </xsl:template>
    
    
    <!-- 
        
        The trail is built one link at a time. Each link is given the ds-trail-link class, with the first and
        the last links given an additional descriptor. 
        
        In case there are more than 5 nodes (which will show up as 3 here) show only 5.
        
        In that case,
        If node number 1
        then show it and an arrow and a dot dot dot
        Otherwise,
        If node number less than number_of_trail_items - 2 
        then do not show
        Otherwise,
        show it
    -->     
    <xsl:template match="dri:trail">
        <xsl:param name="number_of_trail_items"/>        
        
        <xsl:choose>
            <xsl:when test="$number_of_trail_items &gt; 3">
                <li>
                    <xsl:choose>                    
                        <xsl:when test="position()=1"> 
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
                            <span style="color: white; font-size: 110%;">→</span>
                            . . .
                        </xsl:when>                    
                        <xsl:otherwise>
                            <xsl:choose>                        
                                <xsl:when test="position() &lt; $number_of_trail_items - 2">
                                </xsl:when>
                                <xsl:otherwise>
                                    <span style="color: white; font-size: 110%;">→</span>
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
                                </xsl:otherwise>
                                
                            </xsl:choose>
                        </xsl:otherwise>
                    
                    </xsl:choose>
                </li>                
            </xsl:when>
            <xsl:otherwise>
                <li>
                    <!-- put in a little arrow if this is not the first item in the trail -->
                    <xsl:if test="not(position()=1)">
                        <span style="color: white; font-size: 110%;">→</span>
                    </xsl:if>
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
                    
                </li>
            </xsl:otherwise>
        </xsl:choose>
        
    </xsl:template>  
 
</xsl:stylesheet>