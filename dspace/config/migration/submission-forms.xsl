<?xml version="1.0"?>
<!-- This XSLT is used by `./dspace submission-forms-migrate` to transform a DSpace 6.x (or below) input-forms.xml
configuration file into a DSpace 7.x (or above) submission-forms.xml -->
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" doctype-system="submission-forms.dtd"/>

    <xsl:template match="/">
        <input-forms>

            <form-definitions>
                <xsl:apply-templates select="input-forms/form-definitions/form/page"/>
                <xsl:call-template name="predefined-forms"/>
            </form-definitions>

            <xsl:apply-templates select="input-forms/form-value-pairs"/>

        </input-forms>
    </xsl:template>

    <xsl:template match="page">
        <form>
            <xsl:attribute name="name">
                <xsl:value-of select="../@name"/>
                <xsl:text>page</xsl:text>
                <xsl:value-of select="@number"/>
            </xsl:attribute>
            <xsl:apply-templates select="field"/>
        </form>
    </xsl:template>

    <xsl:template match="field">
        <xsl:choose>
            <!-- transform input-type twobox into onebox -->
            <xsl:when test="input-type/text() = 'twobox'">
                <row>
                    <field>
                        <!-- copy each child tag of field where input-type is twobox, except input-type (order children enforced by submission-forms.dtd) -->
                        <xsl:for-each select="*">
                            <xsl:choose>
                                <xsl:when test="(self::input-type)">
                                    <input-type>onebox</input-type>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:copy-of select="."/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:for-each>
                    </field>
                </row>
            </xsl:when>
            <xsl:otherwise>
                <row>
                    <xsl:copy-of select="."/>
                </row>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="form-value-pairs">
            <xsl:copy-of select="."/>
    </xsl:template>

    <xsl:template name="predefined-forms">
        <form name="bitstream-metadata">
            <row>
                <field>
                    <dc-schema>dc</dc-schema>
                    <dc-element>title</dc-element>
                    <dc-qualifier></dc-qualifier>
                    <repeatable>false</repeatable>
                    <label>Title</label>
                    <input-type>onebox</input-type>
                    <hint>Enter the name of the file.</hint>
                    <required>You must enter a main title for this item.</required>
                </field>
            </row>
            <row>
                <field>
                    <dc-schema>dc</dc-schema>
                    <dc-element>description</dc-element>
                    <repeatable>true</repeatable>
                    <label>Description</label>
                    <input-type>textarea</input-type>
                    <hint>Enter a description for the file</hint>
                    <required></required>
                </field>
            </row>
        </form>
    </xsl:template>

</xsl:stylesheet>
