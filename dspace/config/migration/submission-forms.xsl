<?xml version="1.0"?>

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" doctype-system="submission-forms.dtd"/>

    <xsl:template match="/">
<!--        <!DOCTYPE input-forms SYSTEM "submission-forms.dtd">-->
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
        <row>
            <xsl:copy-of select="."/>
        </row>
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
