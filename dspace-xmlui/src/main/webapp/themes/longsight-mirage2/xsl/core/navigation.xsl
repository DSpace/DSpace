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
        <div id="ds-options" class="word-break">
            <xsl:if test="not(contains(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='URI'], 'discover'))">
                <div id="ds-search-option" class="ds-option-set">
                    <!-- The form, complete with a text box and a button, all built from attributes referenced
                 from under pageMeta. -->
                                   </div>
            </xsl:if>
            <xsl:apply-templates/>
            <!-- DS-984 Add RSS Links to Options Box -->
            <xsl:if test="count(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='feed']) != 0">
                <div>
                    <h6 class="ds-option-set-head">
                        <i18n:text>xmlui.feed.header</i18n:text>
                    </h6>
                    <div id="ds-feed-option" class="ds-option-set list-group">
                        <xsl:call-template name="addRSSLinks"/>
                    </div>
                </div>

            </xsl:if>
        </div>
    </xsl:template>

    <!-- Add each RSS feed from meta to a list -->
    <xsl:template name="addRSSLinks">
        <xsl:for-each select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='feed']">
            <a class="list-group-item">
                <xsl:attribute name="href">
                    <xsl:value-of select="."/>
                </xsl:attribute>

                <img src="{concat($context-path, '/static/icons/feed.png')}" alt="feed" class="btn-xs"/>

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
        </xsl:for-each>
    </xsl:template>

    <xsl:template match="dri:options//dri:list">
        <xsl:apply-templates select="dri:head"/>
        <xsl:apply-templates select="dri:item"/>
        <xsl:apply-templates select="dri:list"/>
    </xsl:template>

    <xsl:template match="dri:options/dri:list" priority="3">
        <xsl:apply-templates select="dri:head"/>
        <div>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">list-group</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="dri:item"/>
            <xsl:apply-templates select="dri:list"/>
        </div>
    </xsl:template>


    <xsl:template match="dri:options//dri:item">
        <div>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">list-group-item ds-option</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </div>
    </xsl:template>

    <xsl:template match="dri:options//dri:item[dri:xref]">
        <a href="{dri:xref/@target}">
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">list-group-item ds-option</xsl:with-param>
            </xsl:call-template>
            <xsl:choose>
                <xsl:when test="dri:xref/node()">
                    <xsl:apply-templates select="dri:xref/node()"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="dri:xref"/>
                </xsl:otherwise>
            </xsl:choose>

        </a>
    </xsl:template>

    <xsl:template match="dri:options/dri:list/dri:head" priority="3">
        <xsl:call-template name="renderHead">
            <xsl:with-param name="class">ds-option-set-head</xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="dri:options/dri:list//dri:list/dri:head" priority="3">
        <a class="list-group-item active">
            <span>
                <xsl:call-template name="standardAttributes">
                    <xsl:with-param name="class">
                        <xsl:value-of select="@rend"/>
                        <xsl:text> list-group-item-heading</xsl:text>
                    </xsl:with-param>
                </xsl:call-template>
                <xsl:apply-templates/>
            </span>
        </a>
    </xsl:template>

    <xsl:template match="dri:list[count(child::*)=0]"/>

</xsl:stylesheet>
