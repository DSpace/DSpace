<xsl:stylesheet
        xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
        xmlns:dri="http://di.tamu.edu/DRI/1.0/"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:mets="http://www.loc.gov/METS/"
        xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"  version="1.0">
    <xsl:output indent="yes"/>

    <!-- Capturamos las tablas que queremos presentar en columnas -->
    <!--<xsl:template match="dri:div[@id='aspect.artifactbrowser.CommunityViewer.div.community-view']">-->
    <!--<xsl:apply-templates select="." mode="multiple_column_browse_community"/>-->
    <!--</xsl:template>-->



    <!-- Generamos las columnas -->
    <xsl:template match="dri:referenceSet" mode="multiple_column_browse_community">

        <!-- Recupero la cantidad de filas -->
        <xsl:variable name="itemsTotal">
            <xsl:value-of select="count(dri:reference)"/>
        </xsl:variable>

        <xsl:variable name="cantItemsXCol">
            <xsl:choose>
                <xsl:when test="$itemsTotal>10">
                    <xsl:value-of select="ceiling($itemsTotal div 2)"/>
                </xsl:when>
                <xsl:otherwise>
                    10
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <!-- encabezado de la seccion -->
        <xsl:call-template name="header_browse_columns_community">
            <xsl:with-param name="header"><xsl:value-of select="dri:head"/></xsl:with-param>
        </xsl:call-template>

        <!-- Si hay mas de un elemento debo mostrar por lo menos el primer contenedor -->
        <div class="column_container">

            <xsl:if test="$itemsTotal>=1">
                <xsl:call-template name="div_column_browse_community">
                    <xsl:with-param name="contador">0</xsl:with-param>
                    <xsl:with-param name="cantItemsXCol" select="$cantItemsXCol"/>
                </xsl:call-template>
            </xsl:if>

            <!-- Si hay mas de la cantidad permitida por columna, genero el segundo contenedor -->
            <xsl:if test="$itemsTotal > $cantItemsXCol">
                <xsl:call-template name="div_column_browse_community">
                    <xsl:with-param name="contador">1</xsl:with-param>
                    <xsl:with-param name="cantItemsXCol" select="$cantItemsXCol"/>
                </xsl:call-template>
            </xsl:if>
        </div>

    </xsl:template>

    <!-- No hago nada porque lo controlo desde el otro lado -->
    <xsl:template name="div_column_browse_community">
        <xsl:param name="contador"/>
        <xsl:param name="cantItemsXCol"/>
        <div class="browse_column">
            <ul>
                <xsl:apply-templates select="dri:reference[floor((position()-1) div ($cantItemsXCol)) = $contador]"  mode="li_browse_columns"/>
            </ul>
        </div>

    </xsl:template>

    <!-- Imprime el header de la tabla -->
    <xsl:template name="header_browse_columns_community">
        <xsl:param name="header"/>
        <xsl:if test="$header">
            <h2>
                <xsl:call-template name="standardAttributes">
                    <xsl:with-param name="class">ds-list-head</xsl:with-param>
                </xsl:call-template>
                <i18n:text><xsl:value-of select="$header"/></i18n:text>

            </h2>
        </xsl:if>
    </xsl:template>

    <!-- Es el mismo que el di:row normal, solo que necesito por el MODE -->
    <xsl:template match="dri:reference" mode="li_browse_columns">
        <li>
            <xsl:variable name="externalMetadataURL">
                <xsl:text>cocoon:/</xsl:text>
                <xsl:value-of select="@url"/>
                <xsl:text>?sections=dmdSec,fileSec&amp;fileGrpTypes=THUMBNAIL</xsl:text>
            </xsl:variable>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-artifact-item collection
                    <xsl:if test="(position() mod 2 = 0)">even</xsl:if>
                    <xsl:if test="(position() mod 2 = 1)">odd</xsl:if>
                </xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="document($externalMetadataURL)" mode="nameCollection"/>
            <xsl:apply-templates />
        </li>
    </xsl:template>

    <xsl:template match="mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim" mode="nameCollection">
        <div class="artifact-description">
            <div class="artifact-title">
                <a>
                    <xsl:attribute name="href"><xsl:value-of select="/mets:METS/@OBJID"/></xsl:attribute>
                    <xsl:if test="dim:field[@element='description' and @qualifier='abstract']">
                        <xsl:attribute name="title"><xsl:value-of select="dim:field[@element='description' and @qualifier='abstract']"/></xsl:attribute>
                    </xsl:if>
                    <span class="Z3988"> <xsl:value-of select="dim:field[@element='title']"/></span>
                </a>

                <!--<xsl:if test="dim:field[@element='description' and @qualifier='abstract']">-->
                <!--<div class="artifact-info">-->
                <!--<span class="short-description"> <xsl:value-of select="dim:field[@element='description' and @qualifier='abstract']"/></span>-->
                <!--</div>-->
                <!--</xsl:if>-->

            </div>
        </div>



    </xsl:template>


</xsl:stylesheet>