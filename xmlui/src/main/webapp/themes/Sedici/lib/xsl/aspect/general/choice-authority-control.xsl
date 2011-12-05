<!-- 

Redefinido desde el tema mirage, del xsl con el mismo nombre.
No redefino todos los templates, solo los que necesito redefinir. 

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
    
    <!-- Variable para el manejo de la propiedad -->
    <xsl:variable name="isAdministrator" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='administrator']"/>


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

    <!-- adds autocomplete fields and setup script to "normal" submit input -->
    <xsl:template name="addAuthorityAutocomplete">
      <xsl:param name="confidenceIndicatorID" select="''"/>
      <xsl:param name="confidenceName" select="''"/>
      <xsl:call-template name="addAuthorityAutocompleteWidgets"/>
      <xsl:call-template name="autocompleteSetup">
        <xsl:with-param name="formID"        select="translate(ancestor::dri:div[@interactive='yes']/@id,'.','_')"/>
        <xsl:with-param name="metadataField" select="@n"/>
        <xsl:with-param name="inputName"     select="@n"/>
        <xsl:with-param name="authorityControlled" select="dri:params/@authorityControlled"/>
        <xsl:with-param name="authorityName" select="concat(@n,'_authority')"/>
        <xsl:with-param name="containerID"   select="concat(translate(@id,'.','_'),'_container')"/>
        <xsl:with-param name="indicatorID"   select="concat(translate(@id,'.','_'),'_indicator')"/>
        <xsl:with-param name="isClosed"      select="dri:params/@choicesClosed"/>
        <xsl:with-param name="confidenceIndicatorID" select="$confidenceIndicatorID"/>
        <xsl:with-param name="confidenceName" select="concat(translate(@id,'.','_'),'_confidence')"/>
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
      <xsl:param name="isClosed" select="'No'"/>
      <xsl:param name="authorityControlled" select="'No'"/>
      <script type="text/javascript">
        <xsl:text>runAfterJSImports.add(function() {</xsl:text>
            <xsl:text>$(document).ready(function() {</xsl:text>
                <xsl:text>var gigo = DSpaceSetupAutocomplete('</xsl:text>
                    <xsl:value-of select="$formID"/>
                    <xsl:text>', { metadataField: '</xsl:text>
                    <xsl:value-of select="$metadataField"/>
                    <xsl:text>', isClosed: '</xsl:text>
                    <xsl:value-of select="$isClosed"/>
                    <xsl:text>', authorityControlled: '</xsl:text>
                    <xsl:value-of select="$authorityControlled"/>
                    <xsl:text>', inputName: '</xsl:text>
                    <xsl:value-of select="$inputName"/>
                    <xsl:text>', authorityName: '</xsl:text>
                    <xsl:value-of select="$authorityName"/>
                    <xsl:text>', authorityLabel: '</xsl:text>
                    <xsl:value-of select="$authorityName"/>
                    <xsl:text>_label', containerID: '</xsl:text>
                    <xsl:value-of select="$containerID"/>
                    <xsl:text>', indicatorID: '</xsl:text>
                    <xsl:value-of select="$indicatorID"/>
                    <xsl:text>', confidenceIndicatorID: '</xsl:text>
                    <xsl:value-of select="$confidenceIndicatorID"/>
                    <xsl:text>', confidenceName: '</xsl:text>
                    <xsl:value-of select="$confidenceName"/>
                    <xsl:text>', messageErrorConfidenceSingular: '</xsl:text>
                    <i18n:text>sedici.choiceAuthorityControl.messageErrorConfidenceSingular</i18n:text>
                    <xsl:text>', messageErrorConfidencePlural: '</xsl:text>
                    <i18n:text>sedici.choiceAuthorityControl.messageErrorConfidencePlural</i18n:text>
                    <xsl:text>', collection: </xsl:text>
                    <xsl:value-of select="$collectionID"/>
                    <xsl:text>, contextPath: '</xsl:text>
                    <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>'});</xsl:text>
            <xsl:text>});</xsl:text>
        <xsl:text>});</xsl:text>
      </script>
    </xsl:template>

    <!-- add the extra _authority{_n?} and _confidence input fields -->
    <xsl:template name="authorityInputFields">
      <xsl:param name="name" select="''"/>
      <xsl:param name="id" select="''"/>
      <xsl:param name="position" select="''"/>
      <xsl:param name="authValue" select="''"/>
      <xsl:param name="authLabel" select="''"/>
      <xsl:param name="confValue" select="''"/>
      <xsl:param name="confIndicatorID" select="''"/>
      <xsl:param name="unlockButton" select="''"/>
      <xsl:param name="unlockHelp" select="''"/>
      <xsl:variable name="authFieldID" select="concat(translate(@id,'.','_'),'_authority')"/>
      <xsl:variable name="authLabelID" select="concat(translate(@id,'.','_'),'_authority_label')"/>
      <xsl:variable name="confFieldID" select="concat(translate(@id,'.','_'),'_confidence')"/>

      <!-- 
      	The authority label 
      	Agregado por Nico.
      	Dependiendo si es administrador el usuario logueado se agrega un input onlyread
      	que muestra el label correspondiente al authority key.
      -->

      <xsl:if test="$isAdministrator = 'true'">
       <xsl:choose>
        <xsl:when test="$position = ''">
	      <input>
	        <xsl:attribute name="class">
	          <xsl:text>ds-authority-label</xsl:text>
	        </xsl:attribute>
	        <xsl:attribute name="type"><xsl:value-of select="$authorityInputType"/></xsl:attribute>
	        <xsl:attribute name="readonly"><xsl:text>readonly</xsl:text></xsl:attribute>
	        <xsl:attribute name="name">
	          <xsl:value-of select="concat($name,'_authority_label')"/>
	          <xsl:if test="$position">
	            <xsl:value-of select="concat('_', $position)"/>
	          </xsl:if>
	        </xsl:attribute>
	        <xsl:if test="$id">
	          <xsl:attribute name="id">
	            <xsl:value-of select="$authLabelID"/>
	          </xsl:attribute>
	        </xsl:if>
	        <xsl:attribute name="value">
	          <xsl:value-of select="substring-after($authValue, '#')"/>
	        </xsl:attribute>
	
	      </input>
		 </xsl:when>
         <xsl:otherwise>
              <!-- Aca va el valor real del authority -->
         </xsl:otherwise>
       </xsl:choose>
		
	  </xsl:if>
	  
	  
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
            <xsl:value-of select="substring-after($authValue, '#')"/>
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
        <input type="image" class="ds-authority-lock is-locked " value="">
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


</xsl:stylesheet>