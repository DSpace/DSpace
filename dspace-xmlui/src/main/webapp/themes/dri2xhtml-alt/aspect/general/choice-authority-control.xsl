<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<!--
    Rendering of the authority control related pages.

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

    <!-- =============================================================== -->
    <!-- - - - - - New templates for Choice/Authority control - - - - -  -->

    <!-- choose 'hidden' for invisible auth, 'text' lets CSS control it. -->
    <xsl:variable name="authorityInputType" select="'text'"/>

    <!-- add button to invoke Choices lookup popup.. assume
      -  that the context is a dri:field, where dri:params/@choices is true.
     -->
    <xsl:template name="addLookupButton">
      <xsl:param name="isName" select="'missing value'"/>
      <!-- optional param if you want to send authority value to diff field -->
      <xsl:param name="authorityInput" select="concat(@n,'_authority')"/>
      <!-- optional param for confidence indicator ID -->
      <xsl:param name="confIndicator" select="''"/>
      <input type="button" name="{concat('lookup_',@n)}" class="ds-button-field ds-add-button" >
        <xsl:attribute name="value">
          <xsl:text>Lookup</xsl:text>
          <xsl:if test="contains(dri:params/@operations,'add')">
            <xsl:text> &amp; Add</xsl:text>
          </xsl:if>
        </xsl:attribute>
        <xsl:attribute name="onClick">
          <xsl:text>javascript:DSpaceChoiceLookup('</xsl:text>
          <!-- URL -->
          <xsl:value-of select="concat($context-path,'/admin/lookup')"/>
          <xsl:text>', '</xsl:text>
          <!-- field -->
          <xsl:value-of select="dri:params/@choices"/>
          <xsl:text>', '</xsl:text>
          <!-- formID -->
          <xsl:value-of select="translate(ancestor::dri:div[@interactive='yes']/@id,'.','_')"/>
          <xsl:text>', '</xsl:text>
          <!-- valueInput -->
          <xsl:value-of select="@n"/>
          <xsl:text>', '</xsl:text>
          <!-- authorityInput, name of field to get authority -->
          <xsl:value-of select="$authorityInput"/>
          <xsl:text>', '</xsl:text>
          <!-- Confidence Indicator's ID so lookup can frob it -->
          <xsl:value-of select="$confIndicator"/>
          <xsl:text>', </xsl:text>
          <!-- Collection ID for context -->
          <xsl:choose>
            <xsl:when test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='choice'][@qualifier='collection']">
              <xsl:text>'</xsl:text>
              <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='choice'][@qualifier='collection']"/>
              <xsl:text>'</xsl:text>
            </xsl:when>
            <xsl:otherwise>
              <xsl:text>-1</xsl:text>
            </xsl:otherwise>
          </xsl:choose>
          <xsl:text>, </xsl:text>
          <!-- isName -->
          <xsl:value-of select="$isName"/>
          <xsl:text>, </xsl:text>
          <!-- isRepating -->
          <xsl:value-of select="boolean(contains(dri:params/@operations,'add'))"/>
          <xsl:text>);</xsl:text>
        </xsl:attribute>
      </input>
    </xsl:template>

    <!-- Fragment to display an authority confidence icon.
       -  Insert an invisible 1x1 image which gets "covered" by background
       -  image as dictated by the CSS, so icons are easily adjusted in CSS.
       -  "confidence" param is confidence _value_, i.e. symbolic name
      -->
    <xsl:template name="authorityConfidenceIcon">
      <!-- default confidence value won't show any image. -->
      <xsl:param name="confidence" select="'blank'"/>
      <xsl:param name="id" select="''"/>
      <xsl:variable name="lcConfidence" select="translate($confidence,'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz')"/>
      <img i18n:attr="title">
        <xsl:if test="string-length($id) > 0">
          <xsl:attribute name="id">
             <xsl:value-of select="$id"/>
          </xsl:attribute>
        </xsl:if>
        <xsl:attribute name="src">
           <xsl:value-of select="concat($theme-path,'/images/authority_control/invisible.gif')"/>
        </xsl:attribute>
        <xsl:attribute name="class">
          <xsl:text>ds-authority-confidence </xsl:text>
          <xsl:choose>
            <xsl:when test="string-length($lcConfidence) > 0">
              <xsl:value-of select="concat('cf-',$lcConfidence,' ')"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:text>cf-blank </xsl:text>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
        <xsl:attribute name="title">
          <xsl:text>xmlui.authority.confidence.description.cf_</xsl:text>
          <xsl:value-of select="$lcConfidence"/>
        </xsl:attribute>
      </img>
    </xsl:template>

    <!-- Fragment to include an authority confidence hidden input
       - assumes @n is the name of the field.
       -  param is confidence _value_, i.e. integer 0-6
      -->
    <xsl:template name="authorityConfidenceInput">
      <xsl:param name="confidence"/>
      <xsl:param name="name"/>
      <input class="ds-authority-confidence-input" type="hidden">
        <xsl:attribute name="name">
          <xsl:value-of select="$name"/>
        </xsl:attribute>
        <xsl:attribute name="value">
          <xsl:value-of select="$confidence"/>
        </xsl:attribute>
      </input>
    </xsl:template>


    <!-- insert fields needed by Scriptaculous autocomplete -->
    <xsl:template name="addAuthorityAutocompleteWidgets">
      <!-- "spinner" indicator to signal "loading", managed by autocompleter -->
      <!--  put it next to input field -->
      <span style="display:none;">
        <xsl:attribute name="id">
         <xsl:value-of select="concat(translate(@id,'.','_'),'_indicator')"/>
        </xsl:attribute>
        <img alt="Loading...">
          <xsl:attribute name="src">
           <xsl:value-of select="concat($theme-path,'/images/authority_control/suggest-indicator.gif')"/>
          </xsl:attribute>
        </img>
      </span>
      <!-- This is the anchor for autocomplete popup, div id="..._container" -->
      <!--  put it below input field, give ID to autocomplete below -->
      <div class="autocomplete">
        <xsl:attribute name="id">
         <xsl:value-of select="concat(translate(@id,'.','_'),'_container')"/>
        </xsl:attribute>
        <xsl:text> </xsl:text>
      </div>
    </xsl:template>

    <!-- adds autocomplete fields and setup script to "normal" submit input -->
    <xsl:template name="addAuthorityAutocomplete">
      <xsl:param name="confidenceIndicatorID" select="''"/>
      <xsl:param name="confidenceName" select="''"/>
      <xsl:call-template name="addAuthorityAutocompleteWidgets"/>
      <xsl:call-template name="autocompleteSetup">
        <xsl:with-param name="formID"        select="translate(ancestor::dri:div[@interactive='yes']/@id,'.','_')"/>
        <xsl:with-param name="metadataField" select="@n"/>
        <xsl:with-param name="inputName"     select="@n"/>
        <xsl:with-param name="authorityName" select="concat(@n,'_authority')"/>
        <xsl:with-param name="containerID"   select="concat(translate(@id,'.','_'),'_container')"/>
        <xsl:with-param name="indicatorID"   select="concat(translate(@id,'.','_'),'_indicator')"/>
        <xsl:with-param name="isClosed"      select="contains(dri:params/@choicesClosed,'true')"/>
        <xsl:with-param name="confidenceIndicatorID" select="$confidenceIndicatorID"/>
        <xsl:with-param name="confidenceName" select="$confidenceName"/>
        <xsl:with-param name="collectionID">
          <xsl:choose>
            <xsl:when test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='choice'][@qualifier='collection']">
              <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='choice'][@qualifier='collection']"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:text>-1</xsl:text>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:with-param>
      </xsl:call-template>
    </xsl:template>

    <!-- generate the script that sets up autocomplete feature on input field -->
    <!-- ..it has lots of params -->
    <xsl:template name="autocompleteSetup">
      <xsl:param name="formID" select="'missing value'"/>
      <xsl:param name="metadataField" select="'missing value'"/>
      <xsl:param name="inputName" select="'missing value'"/>
      <xsl:param name="authorityName" select="''"/>
      <xsl:param name="containerID" select="'missing value'"/>
      <xsl:param name="collectionID" select="'-1'"/>
      <xsl:param name="indicatorID" select="'missing value'"/>
      <xsl:param name="confidenceIndicatorID" select="''"/>
      <xsl:param name="confidenceName" select="''"/>
      <xsl:param name="isClosed" select="'false'"/>
      <script type="text/javascript">
        <xsl:text>var gigo = DSpaceSetupAutocomplete('</xsl:text>
        <xsl:value-of select="$formID"/>
        <xsl:text>', { metadataField: '</xsl:text>
        <xsl:value-of select="$metadataField"/>
        <xsl:text>', isClosed: '</xsl:text>
        <xsl:value-of select="$isClosed"/>
        <xsl:text>', inputName: '</xsl:text>
        <xsl:value-of select="$inputName"/>
        <xsl:text>', authorityName: '</xsl:text>
        <xsl:value-of select="$authorityName"/>
        <xsl:text>', containerID: '</xsl:text>
        <xsl:value-of select="$containerID"/>
        <xsl:text>', indicatorID: '</xsl:text>
        <xsl:value-of select="$indicatorID"/>
        <xsl:text>', confidenceIndicatorID: '</xsl:text>
        <xsl:value-of select="$confidenceIndicatorID"/>
        <xsl:text>', confidenceName: '</xsl:text>
        <xsl:value-of select="$confidenceName"/>
        <xsl:text>', collection: </xsl:text>
        <xsl:value-of select="$collectionID"/>
        <xsl:text>, contextPath: '</xsl:text>
        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
        <xsl:text>'});</xsl:text>
      </script>
    </xsl:template>

    <!-- add the extra _authority{_n?} and _confidence input fields -->
    <xsl:template name="authorityInputFields">
      <xsl:param name="name" select="''"/>
      <xsl:param name="id" select="''"/>
      <xsl:param name="position" select="''"/>
      <xsl:param name="authValue" select="''"/>
      <xsl:param name="confValue" select="''"/>
      <xsl:param name="confIndicatorID" select="''"/>
      <xsl:param name="unlockButton" select="''"/>
      <xsl:param name="unlockHelp" select="''"/>
      <xsl:variable name="authFieldID" select="concat(translate(@id,'.','_'),'_authority')"/>
      <xsl:variable name="confFieldID" select="concat(translate(@id,'.','_'),'_confidence')"/>
      <!-- the authority key value -->
      <input>
        <xsl:attribute name="class">
          <xsl:text>ds-authority-value </xsl:text>
          <xsl:if test="$unlockButton">
            <xsl:text>ds-authority-visible </xsl:text>
          </xsl:if>
        </xsl:attribute>
        <xsl:attribute name="type"><xsl:value-of select="$authorityInputType"/></xsl:attribute>
        <xsl:attribute name="readonly"><xsl:text>readonly</xsl:text></xsl:attribute>
        <xsl:attribute name="name">
          <xsl:value-of select="concat($name,'_authority')"/>
          <xsl:if test="$position">
            <xsl:value-of select="concat('_', $position)"/>
          </xsl:if>
        </xsl:attribute>
        <xsl:if test="$id">
          <xsl:attribute name="id">
            <xsl:value-of select="$authFieldID"/>
          </xsl:attribute>
        </xsl:if>
        <xsl:attribute name="value">
          <xsl:value-of select="$authValue"/>
        </xsl:attribute>
        <!-- this updates confidence after a manual change to authority value -->
        <xsl:attribute name="onChange">
          <xsl:text>javascript: return DSpaceAuthorityOnChange(this, '</xsl:text>
          <xsl:value-of select="$confFieldID"/>
          <xsl:text>','</xsl:text>
          <xsl:value-of select="$confIndicatorID"/>
          <xsl:text>');</xsl:text>
        </xsl:attribute>
      </input>
      <!-- optional "unlock" button on (visible) authority value field -->
      <xsl:if test="$unlockButton">
        <input type="image" class="ds-authority-lock is-locked ">
          <xsl:attribute name="onClick">
            <xsl:text>javascript: return DSpaceToggleAuthorityLock(this, '</xsl:text>
            <xsl:value-of select="$authFieldID"/>
            <xsl:text>');</xsl:text>
          </xsl:attribute>
          <xsl:attribute name="src">
             <xsl:value-of select="concat($theme-path,'/images/authority_control/invisible.gif')"/>
          </xsl:attribute>
          <xsl:attribute name="i18n:attr">title</xsl:attribute>
          <xsl:attribute name="title">
            <xsl:value-of select="$unlockHelp"/>
          </xsl:attribute>
        </input>
      </xsl:if>
      <input class="ds-authority-confidence-input" type="hidden">
        <xsl:attribute name="name">
          <xsl:value-of select="concat($name,'_confidence')"/>
          <xsl:if test="$position">
            <xsl:value-of select="concat('_', $position)"/>
          </xsl:if>
        </xsl:attribute>
        <xsl:if test="$id">
          <xsl:attribute name="id">
            <xsl:value-of select="$confFieldID"/>
          </xsl:attribute>
        </xsl:if>
        <xsl:attribute name="value">
          <xsl:value-of select="$confValue"/>
        </xsl:attribute>
      </input>
    </xsl:template>

    <!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  -->
    <!-- Special Transformations for Choice Authority lookup popup page -->

    <!-- indicator spinner -->
    <xsl:template match="dri:item[@id='aspect.general.ChoiceLookupTransformer.item.select']/dri:figure">
      <img id="lookup_indicator_id" alt="Loading..." style="display:none;">
        <xsl:attribute name="src">
         <xsl:value-of select="concat($theme-path,'/images/authority_control/lookup-indicator.gif')"/>
        </xsl:attribute>
      </img>
    </xsl:template>

    <!-- This inline JS must be added to the popup page for choice lookups -->
    <xsl:template name="choiceLookupPopUpSetup">
      <script type="text/javascript">
        var form = document.getElementById('aspect_general_ChoiceLookupTransformer_div_lookup');
        DSpaceChoicesSetup(form);
      </script>
    </xsl:template>

    <!-- Special select widget for lookup popup -->
    <xsl:template match="dri:field[@id='aspect.general.ChoiceLookupTransformer.field.chooser']">
      <div>
        <select onChange="javascript:DSpaceChoicesSelectOnChange();">
          <xsl:call-template name="fieldAttributes"/>
          <xsl:apply-templates/>
          <xsl:comment>space filler because "unclosed" select annoys browsers</xsl:comment>
        </select>
        <img class="choices-lookup" id="lookup_indicator_id" alt="Loading..." style="display:none;">
          <xsl:attribute name="src">
           <xsl:value-of select="concat($theme-path,'/images/authority_control/lookup-indicator.gif')"/>
          </xsl:attribute>
        </img>
      </div>
    </xsl:template>

    <!-- Generate buttons with onClick attribute, since it is the easiest
       - way to set a single event handler in a browser-independent manner.
      -->

    <!-- choice popup "accept" button -->
    <xsl:template match="dri:field[@id='aspect.general.ChoiceLookupTransformer.field.accept']">
      <xsl:call-template name="choiceLookupButton">
        <xsl:with-param name="onClick" select="'javascript:DSpaceChoicesAcceptOnClick();'"/>
      </xsl:call-template>
    </xsl:template>

    <!-- choice popup "more" button -->
    <xsl:template match="dri:field[@id='aspect.general.ChoiceLookupTransformer.field.more']">
      <xsl:call-template name="choiceLookupButton">
        <xsl:with-param name="onClick" select="'javascript:DSpaceChoicesMoreOnClick();'"/>
      </xsl:call-template>
    </xsl:template>

    <!-- choice popup "cancel" button -->
    <xsl:template match="dri:field[@id='aspect.general.ChoiceLookupTransformer.field.cancel']">
      <xsl:call-template name="choiceLookupButton">
        <xsl:with-param name="onClick" select="'javascript:DSpaceChoicesCancelOnClick();'"/>
      </xsl:call-template>
    </xsl:template>

    <!-- button markup: special handling needed because these must not be <input type=submit> -->
    <xsl:template name="choiceLookupButton">
      <xsl:param name="onClick"/>
      <input type="button" onClick="{$onClick}">
        <xsl:call-template name="fieldAttributes"/>
        <xsl:attribute name="value">
            <xsl:choose>
                <xsl:when test="./dri:value[@type='raw']">
                    <xsl:value-of select="./dri:value[@type='raw']"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="./dri:value[@type='default']"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:attribute>
        <xsl:if test="dri:value/i18n:text">
            <xsl:attribute name="i18n:attr">value</xsl:attribute>
        </xsl:if>
        <xsl:apply-templates />
      </input>
    </xsl:template>

</xsl:stylesheet>
