<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<!--
    Rendering specific to the navigation (options)

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

    <!--
        The template to handle dri:options. Since it contains only dri:list tags (which carry the actual
        information), the only things than need to be done is creating the ds-options div and applying
        the templates inside it.

        In fact, the only bit of real work this template does is add the search box, which has to be
        handled specially in that it is not actually included in the options div, and is instead built
        from metadata available under pageMeta.
    -->
    <!-- TODO: figure out why i18n tags break the go button -->
    <xsl:template match="dri:options">
        <div id="ds-options-wrapper">
            <div id="ds-options">
                <xsl:if test="not(contains(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='URI'], 'discover'))">
                    <h1 id="ds-search-option-head" class="ds-option-set-head">
                        <i18n:text>xmlui.dri2xhtml.structural.search</i18n:text>
                    </h1>
                    <div id="ds-search-option" class="ds-option-set">
                        <!-- The form, complete with a text box and a button, all built from attributes referenced
                     from under pageMeta. -->
                        <form id="ds-search-form" method="post">
                            <xsl:attribute name="action">
                                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath']"/>
                                <xsl:value-of
                                        select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='simpleURL']"/>
                            </xsl:attribute>
                            <fieldset>
                                <input class="ds-text-field " type="text">
                                    <xsl:attribute name="name">
                                        <xsl:value-of
                                                select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='queryField']"/>
                                    </xsl:attribute>
                                </input>
                                <input class="ds-button-field " name="submit" type="submit" i18n:attr="value"
                                       value="xmlui.general.go">
                                    <xsl:attribute name="onclick">
                                    <xsl:text>
                                        var radio = document.getElementById(&quot;ds-search-form-scope-container&quot;);
                                        if (radio != undefined &amp;&amp; radio.checked)
                                        {
                                        var form = document.getElementById(&quot;ds-search-form&quot;);
                                        form.action=
                                    </xsl:text>
                                        <xsl:text>&quot;</xsl:text>
                                        <xsl:value-of
                                                select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath']"/>
                                        <xsl:text>/handle/&quot; + radio.value + &quot;</xsl:text>
                                        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='simpleURL']"/>
                                        <xsl:text>&quot; ; </xsl:text>
                                    <xsl:text>
                                        }
                                    </xsl:text>
                                    </xsl:attribute>
                                </input>
                                <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='focus'][@qualifier='container']">
                                    <label>
                                        <input id="ds-search-form-scope-all" type="radio" name="scope" value=""
                                               checked="checked"/>
                                        <i18n:text>xmlui.dri2xhtml.structural.search</i18n:text>
                                    </label>
                                    <br/>
                                    <label>
                                        <input id="ds-search-form-scope-container" type="radio" name="scope">
                                            <xsl:attribute name="value">
                                                <xsl:value-of
                                                        select="substring-after(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='focus'][@qualifier='container'],':')"/>
                                            </xsl:attribute>
                                        </input>
                                        <xsl:choose>
                                            <xsl:when
                                                    test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='focus'][@qualifier='containerType']/text() = 'type:community'">
                                                <i18n:text>xmlui.dri2xhtml.structural.search-in-community</i18n:text>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <i18n:text>xmlui.dri2xhtml.structural.search-in-collection</i18n:text>
                                            </xsl:otherwise>

                                        </xsl:choose>
                                    </label>
                                </xsl:if>
                            </fieldset>
                        </form>
                        <!--Only add if the advanced search url is different from the simple search-->
                        <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='advancedURL'] != /dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='simpleURL']">
                            <!-- The "Advanced search" link, to be perched underneath the search box -->
                            <a>
                                <xsl:attribute name="href">
                                    <xsl:value-of
                                            select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search'][@qualifier='advancedURL']"/>
                                </xsl:attribute>
                                <i18n:text>xmlui.dri2xhtml.structural.search-advanced</i18n:text>
                            </a>
                        </xsl:if>
                    </div>

                </xsl:if>
                <!-- Once the search box is built, the other parts of the options are added -->
                <xsl:apply-templates/>

                <!-- DS-984 Add RSS Links to Options Box -->
                <xsl:if test="count(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='feed']) != 0">
                    <h1 id="ds-feed-option-head" class="ds-option-set-head">
                        <i18n:text>xmlui.feed.header</i18n:text>
                    </h1>
                    <div id="ds-feed-option" class="ds-option-set">
                        <ul>
                            <xsl:call-template name="addRSSLinks"/>
                        </ul>
                    </div>
                </xsl:if>


            </div>
        </div>
    </xsl:template>

    <!-- Add each RSS feed from meta to a list -->
    <xsl:template name="addRSSLinks">
        <xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='feed']">
            <li>
                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="."/>
                    </xsl:attribute>

                    <xsl:attribute name="style">
                        <xsl:text>background: url(</xsl:text>
                        <xsl:value-of select="$context-path"/>
                        <xsl:text>/static/icons/feed.png) no-repeat</xsl:text>
                    </xsl:attribute>

                    <xsl:choose>
                        <xsl:when test="contains(., 'rss_1.0')">
                            <xsl:text>RSS 1.0</xsl:text>
                        </xsl:when>
                        <xsl:when test="contains(., 'rss_2.0')">
                            <xsl:text>RSS 2.0</xsl:text>
                        </xsl:when>
                        <xsl:when test="contains(., 'atom_1.0')">
                            <xsl:text>Atom</xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="@qualifier"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </a>
            </li>
        </xsl:for-each>
    </xsl:template>

    <!--give nested navigation list the class sublist-->
    <xsl:template match="dri:options/dri:list/dri:list" priority="3" mode="nested">
        <li>
            <xsl:apply-templates select="dri:head" mode="nested"/>
            <ul class="ds-simple-list sublist">
                <xsl:apply-templates select="dri:item" mode="nested"/>
            </ul>
        </li>
    </xsl:template>

    <!-- Quick patch to remove empty lists from options -->
    <xsl:template match="dri:options//dri:list[count(child::*)=0]" priority="5" mode="nested">
    </xsl:template>
    <xsl:template match="dri:options//dri:list[count(child::*)=0]" priority="5">
    </xsl:template>

</xsl:stylesheet>
