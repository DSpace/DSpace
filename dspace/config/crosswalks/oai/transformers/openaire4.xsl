<?xml version="1.0" encoding="UTF-8"?>
<!-- Following OpenAIRE Guidelines 4 -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:doc="http://www.lyncode.com/xoai">
    <xsl:output indent="yes" method="xml" omit-xml-declaration="yes"/>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- 
        Formatting dc.date.issued
        based on what OpenAIRE4 specifies for issued dates 
        https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_publicationdate.html
    -->
    <xsl:template
        match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field/text()">
        <xsl:call-template name="formatdate">
            <xsl:with-param name="datestr" select="."/>
        </xsl:call-template>
    </xsl:template>

    <!-- 
        Modifying and normalizing dc.language
        to ISO 639-3 (from ISO 639-1) for each language available at the submission form
     -->
    <xsl:template
        match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element[@name='iso']/doc:element/doc:field/text()">

        <xsl:variable name="lc_value">
            <xsl:call-template name="lowercase">
                <xsl:with-param name="value" select="."/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$lc_value = 'en_US'">
                <xsl:text>eng</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_value = 'en'">
                <xsl:text>eng</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_value = 'es'">
                <xsl:text>spa</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_value = 'de'">
                <xsl:text>deu</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_value = 'fr'">
                <xsl:text>fra</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_value = 'it'">
                <xsl:text>ita</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_value = 'ja'">
                <xsl:text>jpn</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_value = 'zh'">
                <xsl:text>zho</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_value = 'pt'">
                <xsl:text>por</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_value = 'tr'">
                <xsl:text>tur</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="."/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Modifying dc.rights -->
    <!-- Removing unwanted -->
    <xsl:template
        match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element/doc:element"/>

    <!-- 
        Normalizing dc.rights according to COAR Controlled Vocabulary for
        Access Rights (Version 1.0) (http://vocabularies.coar-repositories.org/documentation/access_rights/)
        available at
        https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_accessrights.html#definition-and-usage-instruction
    -->
    <xsl:template
        match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element/doc:field/text()">
        <xsl:variable name="value" select="."/>
        <xsl:variable name="lc_value">
            <xsl:call-template name="lowercase">
                <xsl:with-param name="value" select="."/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:choose>
            <xsl:when
                test="$lc_value = 'open access' or $lc_value = 'openaccess' or $value = 'http://purl.org/coar/access_right/c_abf2'">
                <xsl:text>open access</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_value = 'embargoed access' or $lc_value = 'embargoedaccess' or $value = 'http://purl.org/coar/access_right/c_f1cf'">
                <xsl:text>embargoed access</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_value = 'restricted access' or $lc_value = 'restrictedaccess' or $value = 'http://purl.org/coar/access_right/c_16ec'">
                <xsl:text>restricted access</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_value = 'metadata only access' or $lc_value = 'closedaccess' or $value = 'http://purl.org/coar/access_right/c_14cb'">
                <xsl:text>metadata only access</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$value"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Modifying and normalizing dc.type according to COAR Controlled Vocabulary for Resource Type 
        Genres (Version 2.0) (http://vocabularies.coar-repositories.org/documentation/resource_types/)
        available at 
        https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/v4.0.0/field_publicationtype.html#attribute-uri-m
    -->
    <xsl:template
        match="/doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field/text()">
        <xsl:variable name="dc_type" select="."/>
        <xsl:variable name="lc_dc_type">
            <xsl:call-template name="lowercase">
                <xsl:with-param name="value" select="."/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:choose>
            <xsl:when
                test="$lc_dc_type = 'annotation' or $dc_type = 'http://purl.org/coar/resource_type/c_1162'">
                <xsl:text>annotation</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'journal'">
                <xsl:text>journal</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'journal article' or $lc_dc_type = 'article' or $lc_dc_type = 'journalarticle' or $dc_type = 'http://purl.org/coar/resource_type/c_6501'">
                <xsl:text>journal article</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'editorial' or $dc_type = 'http://purl.org/coar/resource_type/c_b239'">
                <xsl:text>editorial</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'bachelor thesis' or $lc_dc_type = 'bachelorthesis' or $dc_type = 'http://purl.org/coar/resource_type/c_7a1f'">
                <xsl:text>bachelor thesis</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'bibliography' or $dc_type = 'http://purl.org/coar/resource_type/c_86bc'">
                <xsl:text>bibliography</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'book' or $dc_type = 'http://purl.org/coar/resource_type/c_2f33'">
                <xsl:text>book</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'book part' or $lc_dc_type = 'bookpart' or $dc_type = 'http://purl.org/coar/resource_type/c_3248'">
                <xsl:text>book part</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'book review' or $lc_dc_type = 'bookreview' or $dc_type = 'http://purl.org/coar/resource_type/c_ba08'">
                <xsl:text>book review</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'website' or $dc_type = 'http://purl.org/coar/resource_type/c_7ad9'">
                <xsl:text>website</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'interactive resource' or $lc_dc_type = 'interactiveresource' or $dc_type = 'http://purl.org/coar/resource_type/c_e9a0'">
                <xsl:text>interactive resource</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'conference proceedings' or $lc_dc_type = 'conferenceproceedings' or $dc_type = 'http://purl.org/coar/resource_type/c_f744'">
                <xsl:text>conference proceedings</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'conference object' or $lc_dc_type = 'conferenceobject' or $dc_type = 'http://purl.org/coar/resource_type/c_c94f'">
                <xsl:text>conference object</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'conference paper' or $lc_dc_type = 'conferencepaper' or $dc_type = 'http://purl.org/coar/resource_type/c_5794'">
                <xsl:text>conference paper</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'conference poster' or $lc_dc_type = 'conferenceposter' or $dc_type = 'http://purl.org/coar/resource_type/c_6670'">
                <xsl:text>conference poster</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'contribution to journal' or $lc_dc_type = 'contributiontojournal' or $dc_type = 'http://purl.org/coar/resource_type/c_3e5a'">
                <xsl:text>contribution to journal</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'datapaper' or $dc_type = 'http://purl.org/coar/resource_type/c_beb9'">
                <xsl:text>data paper</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'dataset' or $dc_type = 'http://purl.org/coar/resource_type/c_ddb1'">
                <xsl:text>dataset</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'doctoral thesis' or $lc_dc_type = 'doctoralthesis' or $dc_type = 'http://purl.org/coar/resource_type/c_db06'">
                <xsl:text>doctoral thesis</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'image' or $dc_type = 'http://purl.org/coar/resource_type/c_c513'">
                <xsl:text>image</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'lecture' or $dc_type = 'http://purl.org/coar/resource_type/c_8544'">
                <xsl:text>lecture</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'letter' or $dc_type = 'http://purl.org/coar/resource_type/c_0857'">
                <xsl:text>letter</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'master thesis' or $lc_dc_type = 'masterthesis' or $dc_type = 'http://purl.org/coar/resource_type/c_bdcc'">
                <xsl:text>master thesis</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'moving image' or $lc_dc_type = 'movingimage' or $dc_type = 'http://purl.org/coar/resource_type/c_8a7e'">
                <xsl:text>moving image</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'periodical' or $dc_type = 'http://purl.org/coar/resource_type/c_2659'">
                <xsl:text>periodical</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'letter to the editor' or $lc_dc_type = 'lettertotheeditor' or $dc_type = 'http://purl.org/coar/resource_type/c_545b'">
                <xsl:text>letter to the editor</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'patent' or $dc_type = 'http://purl.org/coar/resource_type/c_15cd'">
                <xsl:text>patent</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'preprint' or $dc_type = 'http://purl.org/coar/resource_type/c_816b'">
                <xsl:text>preprint</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'report' or $dc_type = 'http://purl.org/coar/resource_type/c_93fc'">
                <xsl:text>report</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'report part' or $lc_dc_type = 'reportpart' or $dc_type = 'http://purl.org/coar/resource_type/c_ba1f'">
                <xsl:text>report part</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'research proposal' or $lc_dc_type = 'researchproposal' or $dc_type = 'http://purl.org/coar/resource_type/c_baaf'">
                <xsl:text>research proposal</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'review' or $dc_type = 'http://purl.org/coar/resource_type/c_efa0'">
                <xsl:text>review</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'software' or $dc_type = 'http://purl.org/coar/resource_type/c_5ce6'">
                <xsl:text>software</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'still image' or $lc_dc_type = 'stillimage' or $dc_type = 'http://purl.org/coar/resource_type/c_ecc8'">
                <xsl:text>still image</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'technical documentation' or $lc_dc_type = 'technicaldocumentation' or $dc_type = 'http://purl.org/coar/resource_type/c_71bd'">
                <xsl:text>technical documentation</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'workflow' or $dc_type = 'http://purl.org/coar/resource_type/c_393c'">
                <xsl:text>workflow</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'working paper' or $lc_dc_type = 'workingpaper' or $dc_type = 'http://purl.org/coar/resource_type/c_8042'">
                <xsl:text>working paper</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'thesis' or $dc_type = 'http://purl.org/coar/resource_type/c_46ec'">
                <xsl:text>thesis</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'cartographic material' or $lc_dc_type = 'cartographicmaterial' or $dc_type = 'http://purl.org/coar/resource_type/c_12cc'">
                <xsl:text>cartographic material</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'map' or $dc_type = 'http://purl.org/coar/resource_type/c_12cd'">
                <xsl:text>map</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'video' or $dc_type = 'http://purl.org/coar/resource_type/c_12ce'">
                <xsl:text>video</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'sound' or $dc_type = 'http://purl.org/coar/resource_type/c_18cc'">
                <xsl:text>sound</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'musical composition' or $lc_dc_type = 'musicalcomposition' or $dc_type = 'http://purl.org/coar/resource_type/c_18cd'">
                <xsl:text>musical composition</xsl:text>
            </xsl:when>
            <xsl:when test="$lc_dc_type = 'text' or $dc_type = 'http://purl.org/coar/resource_type/c_18cf'">
                <xsl:text>text</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'conference paper not in proceedings' or $lc_dc_type = 'conferencepapernotinproceedings' or $dc_type = 'http://purl.org/coar/resource_type/c_18cp'">
                <xsl:text>conference paper not in proceedings</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'conference poster not in proceedings' or $lc_dc_type = 'conferenceposternotinproceedings' or $dc_type = 'http://purl.org/coar/resource_type/c_18co'">
                <xsl:text>conference poster not in proceedings</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'musical notation' or $dc_type = 'http://purl.org/coar/resource_type/c_18cw'">
                <xsl:text>musical notation</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'internal report' or $lc_dc_type = 'internalreport' or $dc_type = 'http://purl.org/coar/resource_type/c_18ww'">
                <xsl:text>internal report</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'memorandum' or $dc_type = 'http://purl.org/coar/resource_type/c_18wz'">
                <xsl:text>memorandum</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'other type of report'  or $lc_dc_type = 'othertypeofreport' or $dc_type = 'http://purl.org/coar/resource_type/c_18wq'">
                <xsl:text>other type of report</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'policy report' or $lc_dc_type = 'policyreport'  or $dc_type = 'http://purl.org/coar/resource_type/c_186u'">
                <xsl:text>policy report</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'project deliverable' or $lc_dc_type = 'projectdeliverable' or $dc_type = 'http://purl.org/coar/resource_type/c_18op'">
                <xsl:text>project deliverable</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'report to funding agency' or $lc_dc_type = 'reporttofundingagency' or $dc_type = 'http://purl.org/coar/resource_type/c_18hj'">
                <xsl:text>report to funding agency</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'research report' or $lc_dc_type = 'researchreport' or $dc_type = 'http://purl.org/coar/resource_type/c_18ws'">
                <xsl:text>research report</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'technical report' or $lc_dc_type = 'technicalreport' or $dc_type = 'http://purl.org/coar/resource_type/c_18gh'">
                <xsl:text>technical report</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'review article' or $lc_dc_type = 'reviewarticle' or $dc_type = 'http://purl.org/coar/resource_type/c_dcae04bc'">
                <xsl:text>review article</xsl:text>
            </xsl:when>
            <xsl:when
                test="$lc_dc_type = 'research article' or $lc_dc_type = 'researcharticle' or $dc_type = 'http://purl.org/coar/resource_type/c_2df8fbb1'">
                <xsl:text>research article</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>other</xsl:text>
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>



    <!-- AUXILIARY TEMPLATES -->


    <!--  
        Date format
        This template is discarding the " 16:53:24.556" part from a date and time 
        like "2019-04-30 16:53:24.556" to support the YYYY-MM-DD format of 
        ISO 8601 [W3CDTF]
    -->
    <xsl:template name="formatdate">
        <xsl:param name="datestr"/>
        <xsl:variable name="sub">
            <xsl:value-of select="substring($datestr,1,10)"/>
        </xsl:variable>
        <xsl:value-of select="$sub"/>
    </xsl:template>

   <!--  -->
   <!-- Other Auxiliary templates -->
   <!--  -->
    <xsl:param name="smallcase" select="'abcdefghijklmnopqrstuvwxyzàèìòùáéíóúýâêîôûãñõäëïöüÿåæœçðø'"/>
    <xsl:param name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZÀÈÌÒÙÁÉÍÓÚÝÂÊÎÔÛÃÑÕÄËÏÖÜŸÅÆŒÇÐØ'"/>    

   <!-- to retrieve a string in uppercase -->
    <xsl:template name="uppercase">
        <xsl:param name="value"/>
        <xsl:value-of select="translate($value, $smallcase, $uppercase)"/>
    </xsl:template>

   <!-- to retrieve a string in lowercase -->
    <xsl:template name="lowercase">
        <xsl:param name="value"/>
        <xsl:value-of select="translate($value, $uppercase, $smallcase)"/>
    </xsl:template>

    <!-- to retrieve a string which the first letter is in uppercase -->
    <xsl:template name="ucfirst">
        <xsl:param name="value"/>
        <xsl:call-template name="uppercase">
            <xsl:with-param name="value" select="substring($value, 1, 1)"/>
        </xsl:call-template>
        <xsl:call-template name="lowercase">
            <xsl:with-param name="value" select="substring($value, 2)"/>
        </xsl:call-template>
    </xsl:template>
</xsl:stylesheet>
