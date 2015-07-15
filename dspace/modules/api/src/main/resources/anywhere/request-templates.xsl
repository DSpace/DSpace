<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.1" >
    <xsl:param name="username"/>
    <xsl:param name="password"/>
    <xsl:param name="customerId"/>
    <xsl:param name="date"/>

    <xsl:template match="/load-credit">
        <creditRequest>
            <integratorUsername><xsl:value-of select="$username"/></integratorUsername>
            <integratorPassword><xsl:value-of select="$password"/></integratorPassword>
            <custId><xsl:value-of select="$customerId"/></custId>
            <txTy>PREPAID</txTy>
        </creditRequest>
    </xsl:template>

    <xsl:template match="/customer-info">
        <custInfoRequest>
            <custId><xsl:value-of select="$customerId"/></custId>
            <integratorUsername><xsl:value-of select="$username"/></integratorUsername>
            <integratorPassword><xsl:value-of select="$password"/></integratorPassword>
            <details includeCodeValues="true">
                <roles include="true"/>
                <addresses include="true" includeBad="true"/>
                <phones include="true"/>
                <emails include="true" includeBad="true"/>
                <websites include="true" includeBad="true"/>
                <jobs include="true" includeInactive="true"/>
                <committeePositions include="true" includeInactive="true"/>
                <memberships include="true" includeInactive="true"/>
                <subscriptions include="true" includeExpired="true"/>
                <communicationPreferences include="true"/>
                <customerAttributes include="true" includeAll="true"></customerAttributes>
                <bio include="true"/>
            </details>
        </custInfoRequest>
    </xsl:template>

    <xsl:template match="/update-credit">
        <credit-request>
            <vendor-id><xsl:value-of select="$username"/></vendor-id>
            <vendor-password><xsl:value-of select="$password"/></vendor-password>
            <cust-id><xsl:value-of select="$customerId"/></cust-id>
            <trans-type>DEFERRED</trans-type>
            <trans-date><xsl:value-of select="$date"/></trans-date>
            <cred-accepted>-1</cred-accepted>
        </credit-request>
    </xsl:template>

</xsl:stylesheet>

