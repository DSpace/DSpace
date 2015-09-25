<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:doc="http://www.lyncode.com/xoai"
	version="1.0">
	<xsl:output method="text" />

<!-- Change this to whatever is appropriate for your export -->
<xsl:variable name='school'>Technische Universität Hamburg-Harburg</xsl:variable>
<xsl:variable name='address'>Hamburg</xsl:variable>


<!-- Stop changing at this point unless you really know what you are doing -->
<xsl:variable name='newline'><xsl:text>
</xsl:text></xsl:variable>
<xsl:variable name='tab'><xsl:text>   </xsl:text></xsl:variable>
	<xsl:template match="/">
		<xsl:choose>
			<xsl:when test="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value']/text() = 'article'">
				<xsl:text>@article</xsl:text>
			</xsl:when>
			<xsl:when test="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value']/text() = 'Thesis'">
                            <xsl:choose>
                                <xsl:when test="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element[@name='thesis']/doc:element/doc:field[@name='value']/text() = 'doctoralThesis' or doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element[@name='thesis']/doc:element/doc:field[@name='value']/text() = 'habilitation'">
                                    <xsl:text>@phdthesis</xsl:text>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:text>@mastersthesis</xsl:text>
                                </xsl:otherwise>
                            </xsl:choose>
			</xsl:when>
			<xsl:when test="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value']/text() = 'Proceedings'">
				<xsl:text>@proceedings</xsl:text>
			</xsl:when>
			<xsl:when test="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value']/text() = 'inProceedings'">
				<xsl:text>@inproceedings</xsl:text>
			</xsl:when>
			<xsl:when test="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value']/text() = 'book'">
				<xsl:text>@book</xsl:text>
			</xsl:when>
			<xsl:when test="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value']/text() = 'bookPart'">
				<xsl:text>@inbook</xsl:text>
			</xsl:when>
			<xsl:when test="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value']/text() = 'report'">
                            <xsl:choose>
                                <xsl:when test="doc:metadata/doc:element[@name='tuhh']/doc:element[@name='type']/doc:element[@name='opus']/doc:element/doc:field[@name='value']/text() = 'Report (Bericht)'">
                                    <xsl:text>@techreport</xsl:text>
                                </xsl:when>
                                <xsl:when test="doc:metadata/doc:element[@name='tuhh']/doc:element[@name='type']/doc:element[@name='opus']/doc:element/doc:field[@name='value']/text() = 'Anleitung (Manual)'">
                                    <xsl:text>@manual</xsl:text>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:text>@misc</xsl:text>
                                </xsl:otherwise>
                            </xsl:choose>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>@misc</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:text> { </xsl:text>
			<xsl:value-of select="translate(doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text(), '/', '_')"></xsl:value-of>
			<xsl:text>,</xsl:text>
			<xsl:value-of select="$newline"></xsl:value-of>
			<xsl:value-of select="$tab"></xsl:value-of>
			<xsl:text>title = {</xsl:text>
			<xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']/text()"></xsl:value-of>
			<xsl:text>},</xsl:text>
			<xsl:value-of select="$newline"></xsl:value-of>
			<xsl:value-of select="$tab"></xsl:value-of>
			<xsl:text>author = {</xsl:text>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value']">
				<xsl:if test="position() > 1"><xsl:text> AND </xsl:text></xsl:if>
				<xsl:value-of select="."></xsl:value-of>
			</xsl:for-each>
			<xsl:text>},</xsl:text>
                        <xsl:if test="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value']/text() = 'Thesis'">
                            <xsl:value-of select="$newline"></xsl:value-of>
                            <xsl:value-of select="$tab"></xsl:value-of>
                            <xsl:text>school = {</xsl:text>
                            <xsl:value-of select="$school" />
                            <xsl:text>},</xsl:text>
                        </xsl:if>
                        <xsl:if test="doc:metadata/doc:element[@name='tuhh']/doc:element[@name='publication']/doc:element[@name='institute']/">
                            <xsl:value-of select="$newline"></xsl:value-of>
                            <xsl:value-of select="$tab"></xsl:value-of>
                            <xsl:text>institute = {</xsl:text>
                            <xsl:value-of select="$school" />
                            <xsl:text>, </xsl:text>
                            <xsl:value-of select="doc:metadata/doc:element[@name='tuhh']/doc:element[@name='publication']/doc:element[@name='institute']/doc:element/doc:field[@name='value']/text()"></xsl:value-of><xsl:text>},</xsl:text>
                        </xsl:if>
			<xsl:if test="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']">
				<xsl:value-of select="$newline"></xsl:value-of>
				<xsl:value-of select="$tab"></xsl:value-of>
				<xsl:text>year = {</xsl:text>
				<xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']/text()"></xsl:value-of>
				<xsl:text>},</xsl:text>
			</xsl:if>
                        <xsl:if test="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value']/text() = 'Thesis'">
                            <xsl:value-of select="$newline"></xsl:value-of>
                            <xsl:value-of select="$tab"></xsl:value-of>
                            <xsl:text>address = {</xsl:text>
                            <xsl:value-of select="$address" />
                            <xsl:text>},</xsl:text>
                        </xsl:if>
                        <xsl:if test="doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='ispartofseries']/">
                            <xsl:value-of select="$newline"></xsl:value-of>
                            <xsl:value-of select="$tab"></xsl:value-of>
                            <xsl:text>series = {</xsl:text><xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='ispartofseries']/doc:element/doc:field[@name='value']/text()"></xsl:value-of><xsl:text>},</xsl:text>
                        </xsl:if>
                        <xsl:if test="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value']/text() = 'Thesis'">
                            <xsl:value-of select="$newline"></xsl:value-of>
                            <xsl:value-of select="$tab"></xsl:value-of>
                            <xsl:text>type = {</xsl:text>
                            <xsl:choose>
                                <xsl:when test="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element[@name='thesis']/doc:element/doc:field[@name='value']/text() = 'doctoralThesis'">
                                    <xsl:text>Dissertation</xsl:text>
                                </xsl:when>
                                <xsl:when test="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element[@name='thesis']/doc:element/doc:field[@name='value']/text() = 'habilitation'">
                                    <xsl:text>Habilitation</xsl:text>
                                </xsl:when>
                                <xsl:when test="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element[@name='thesis']/doc:element/doc:field[@name='value']/text() = 'masterThesis'">
                                    <xsl:text>Master Thesis</xsl:text>
                                </xsl:when>
                                <xsl:when test="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element[@name='thesis']/doc:element/doc:field[@name='value']/text() = 'diplomaThesis'">
                                    <xsl:text>Diplomarbeit</xsl:text>
                                </xsl:when>
                                <xsl:when test="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element[@name='thesis']/doc:element/doc:field[@name='value']/text() = 'bachelorThesis'">
                                    <xsl:text>Bachelor Thesis</xsl:text>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:text>Other</xsl:text>
                                </xsl:otherwise>
                            </xsl:choose>
                            <xsl:text>},</xsl:text>
                        </xsl:if>
<!--
			<xsl:if test="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='citation']">
				<xsl:value-of select="$newline"></xsl:value-of>
				<xsl:value-of select="$tab"></xsl:value-of>
				<xsl:text>journal = {</xsl:text>
				<xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='citation']/doc:element/doc:field[@name='value']/text()"></xsl:value-of>
				<xsl:text>},</xsl:text>
			</xsl:if>
-->
                    <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']">
                        <xsl:value-of select="$newline"></xsl:value-of>
                        <xsl:value-of select="$tab"></xsl:value-of>
                        <xsl:text>abstract = {</xsl:text>
                        <xsl:value-of select="./doc:element/doc:element/doc:field[@name='value']/text()" />
                        <xsl:text>},</xsl:text>
                    </xsl:for-each>

			<xsl:if test="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='isbn']">
				<xsl:value-of select="$newline"></xsl:value-of>
				<xsl:value-of select="$tab"></xsl:value-of>
				<xsl:text>isbn = {</xsl:text>
				<xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='isbn']/doc:element/doc:field[@name='value']/text()"></xsl:value-of>
				<xsl:text>},</xsl:text>
			</xsl:if>
			<xsl:if test="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='doi']">
				<xsl:value-of select="$newline"></xsl:value-of>
				<xsl:value-of select="$tab"></xsl:value-of>
				<xsl:text>doi = {</xsl:text>
				<xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='doi']/doc:element/doc:field[@name='value']/text()"></xsl:value-of>
				<xsl:text>},</xsl:text>
			</xsl:if>
			<xsl:if test="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']">
				<xsl:value-of select="$newline"></xsl:value-of>
				<xsl:value-of select="$tab"></xsl:value-of>
				<xsl:text>url = {</xsl:text>
				<xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field[@name='value']/text()"></xsl:value-of>
				<xsl:text>}</xsl:text>
			</xsl:if>
			<xsl:value-of select="$newline"></xsl:value-of>
		<xsl:text>}</xsl:text>
		<xsl:value-of select="$newline"></xsl:value-of>
	</xsl:template>
</xsl:stylesheet>
<!--
        // Optional fields
        foreach ($metadata->getSequence() as $sequence) {
            $output .= $this->field('series', $sequence->getSequenceName());
        }
        $output .= $this->field('publisher',
                $metadata->getPublisherUniversity());

        $output .= $this->field('booktitle', $metadata->getSourceTitle());
        $output .= $this->field('school', $metadata->getPublisherUniversity());
        $output .= $this->field('address', $metadata->getInstitutionAddress());
        $output .= $this->field('institution', $metadata->getInstitutionName());
-->