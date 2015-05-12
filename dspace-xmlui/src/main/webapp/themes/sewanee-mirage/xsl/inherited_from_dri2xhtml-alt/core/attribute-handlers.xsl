
<!--
    Templates to cover the attribute calls.

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

    <!-- The last thing in the structural elements section are the templates to cover the attribute calls.
        Although, by default, XSL only parses elements and text, an explicit call to apply the attributes
        of children tags can still be made. This, in turn, requires templates that handle specific attributes,
        like the kind you see below. The chief amongst them is the pagination attribute contained by divs,
        which creates a new div element to display pagination information. -->


    <!-- A quick helper function used by the @pagination template for repetitive tasks -->
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

    <!-- Add the HTML5 autofocus attribute to the input field -->
    <xsl:template match="@autofocus">
        <xsl:attribute name="autofocus"><xsl:value-of select="."/></xsl:attribute>
    </xsl:template>

    <!-- The general "catch-all" template for attributes matched, but not handled above -->
    <xsl:template match="@*"></xsl:template>

        <xsl:template match="dri:div[@n = 'masked-page-control']">
        <!--Do not render this division, this is handled by the xsl-->
    </xsl:template>

    <xsl:template match="dri:div[@n ='search-controls-gear']">
        <xsl:param name="position"/>
        <div>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class"><xsl:value-of select="$position"/></xsl:with-param>
            </xsl:call-template>

            <xsl:apply-templates/>
        </div>
    </xsl:template>

</xsl:stylesheet>
