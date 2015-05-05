
<!--
    Templates to cover the forms and forms fields.

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

    <xsl:template match="dri:list[@type='form']" priority="3">
        <xsl:choose>
            <xsl:when test="ancestor::dri:list[@type='form']">
                <div>
                    <xsl:call-template name="standardAttributes">
                        <xsl:with-param name="class">
                            <!-- Provision for the sub list -->
                            <xsl:text>ds-form-</xsl:text>
                            <xsl:if test="ancestor::dri:list[@type='form']">
                                <xsl:text>sub</xsl:text>
                            </xsl:if>
                            <xsl:text>list panel panel-default </xsl:text>
                            <xsl:if test="count(dri:item) > 3">
                                <xsl:text>thick </xsl:text>
                            </xsl:if>
                        </xsl:with-param>
                    </xsl:call-template>
                    <xsl:apply-templates select="dri:head" mode="panel-heading"/>
                    <div class="panel-body">
                        <xsl:apply-templates select="*[not(name()='label' or name()='head')]"/>
                    </div>

                </div>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates select="dri:head"/>
                <fieldset>
                    <xsl:call-template name="standardAttributes">
                        <xsl:with-param name="class">
                            <!-- Provision for the sub list -->
                            <xsl:text>col ds-form-</xsl:text>
                            <xsl:if test="ancestor::dri:list[@type='form']">
                                <xsl:text>sub</xsl:text>
                            </xsl:if>
                            <xsl:text>list </xsl:text>
                            <xsl:if test="count(dri:item) > 3">
                                <xsl:text>thick </xsl:text>
                            </xsl:if>
                        </xsl:with-param>
                    </xsl:call-template>
                    <xsl:apply-templates select="*[not(name()='label' or name()='head')]" />
                </fieldset>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="dri:list[@type='form']//dri:item" priority="3">
        <div>
            <xsl:choose>
                <xsl:when test="dri:field[@type='composite']">
                    <xsl:call-template name="standardAttributes">
                        <xsl:with-param name="class">
                            <xsl:text>ds-form-item row </xsl:text>
                            <xsl:if test="contains('aspect.submission.StepTransformer',@id)">
                                <xsl:text>table </xsl:text>
                            </xsl:if>
                        </xsl:with-param>
                    </xsl:call-template>
                    <div>
                        <xsl:attribute name="class">
                            <xsl:text>control-group col-sm-12</xsl:text>
                            <xsl:if test="dri:field/dri:error">
                                <xsl:text> has-error</xsl:text>
                            </xsl:if>
                        </xsl:attribute>
                        <xsl:apply-templates mode="formComposite"/>
                    </div>
                </xsl:when>
                <xsl:when test="dri:list[@type='form']">
                    <xsl:call-template name="standardAttributes">
                        <xsl:with-param name="class">
                            <xsl:text>ds-form-item row </xsl:text>
                            <xsl:if test="contains('aspect.submission.StepTransformer',@id)">
                                <xsl:text>table </xsl:text>
                            </xsl:if>
                        </xsl:with-param>
                    </xsl:call-template>
                    <xsl:apply-templates />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template name="standardAttributes">
                        <xsl:with-param name="class">
                            <xsl:text>ds-form-item row </xsl:text>
                            <xsl:if test="contains('aspect.submission.StepTransformer',@id)">
                                <xsl:text>table </xsl:text>
                            </xsl:if>
                        </xsl:with-param>
                    </xsl:call-template>
                    <div>
                        <xsl:attribute name="class">
                            <xsl:text>control-group col-sm-12</xsl:text>
                            <xsl:if test="dri:field/dri:error">
                                <xsl:text> has-error</xsl:text>
                            </xsl:if>
                        </xsl:attribute>
                        <xsl:call-template name="pick-label"/>
                        <xsl:apply-templates />
                        <!-- special name used in submission UI review page -->
                        <xsl:if test="@n = 'submit-review-field-with-authority'">
                            <xsl:call-template name="authorityConfidenceIcon">
                                <xsl:with-param name="confidence" select="substring-after(./@rend, 'cf-')"/>
                            </xsl:call-template>
                        </xsl:if>
                    </div>
                </xsl:otherwise>
            </xsl:choose>
        </div>
    </xsl:template>

    <xsl:template match="dri:field" mode="normalField">
        <xsl:variable name="confidenceIndicatorID" select="concat(translate(@id,'.','_'),'_confidence_indicator')"/>
        <xsl:choose>
            <!-- TODO: this has changed dramatically (see form3.xml) -->
            <xsl:when test="@type= 'select'">
                <!--<div class="col-sm-10">-->
                    <select>
                        <xsl:call-template name="fieldAttributes"/>
                        <xsl:apply-templates/>
                    </select>
                <!--</div>-->
            </xsl:when>
            <xsl:when test="@type= 'textarea'">
                <!--<div class="col-sm-10">-->
                    <textarea>
                        <xsl:call-template name="fieldAttributes"/>
                        <xsl:attribute name="onkeydown">event.cancelBubble=true;</xsl:attribute>

                        <!--
                            if the cols and rows attributes are not defined we need to call
                            the templates for them since they are required attributes in strict xhtml
                         -->
                        <xsl:choose>
                            <xsl:when test="not(./dri:params[@cols])">
                                <xsl:call-template name="textAreaCols"/>
                            </xsl:when>
                        </xsl:choose>
                        <xsl:choose>
                            <xsl:when test="not(./dri:params[@rows])">
                                <xsl:call-template name="textAreaRows"/>
                            </xsl:when>
                        </xsl:choose>

                        <xsl:apply-templates />
                        <xsl:choose>
                            <xsl:when test="./dri:value[@type='raw']">
                                <xsl:copy-of select="./dri:value[@type='raw']/node()"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:copy-of select="./dri:value[@type='default']/node()"/>
                            </xsl:otherwise>
                        </xsl:choose>
                        <xsl:if  test="string-length(./dri:value) &lt; 1">
                            <i18n:text>xmlui.dri2xhtml.default.textarea.value</i18n:text>
                        </xsl:if>
                    </textarea>
                <!--</div>-->


                <!-- add place to store authority value -->
                <xsl:if test="dri:params/@authorityControlled">
                    <xsl:variable name="confidence">
                        <xsl:if test="./dri:value[@type='authority']">
                            <xsl:value-of select="./dri:value[@type='authority']/@confidence"/>
                        </xsl:if>
                    </xsl:variable>
                    <!-- add authority confidence widget -->
                    <xsl:call-template name="authorityConfidenceIcon">
                        <xsl:with-param name="confidence" select="$confidence"/>
                        <xsl:with-param name="id" select="$confidenceIndicatorID"/>
                    </xsl:call-template>
                    <xsl:call-template name="authorityInputFields">
                        <xsl:with-param name="name" select="@n"/>
                        <xsl:with-param name="id" select="@id"/>
                        <xsl:with-param name="authValue" select="dri:value[@type='authority']/text()"/>
                        <xsl:with-param name="confValue" select="dri:value[@type='authority']/@confidence"/>
                        <xsl:with-param name="confIndicatorID" select="$confidenceIndicatorID"/>
                        <xsl:with-param name="unlockButton" select="dri:value[@type='authority']/dri:field[@rend='ds-authority-lock']/@n"/>
                        <xsl:with-param name="unlockHelp" select="dri:value[@type='authority']/dri:field[@rend='ds-authority-lock']/dri:help"/>
                    </xsl:call-template>
                </xsl:if>
                <!-- add choice mechanisms -->
                <xsl:choose>
                    <xsl:when test="dri:params/@choicesPresentation = 'suggest'">
                        <xsl:call-template name="addAuthorityAutocomplete">
                            <xsl:with-param name="confidenceIndicatorID" select="$confidenceIndicatorID"/>
                            <xsl:with-param name="confidenceName">
                                <xsl:value-of select="concat(@n,'_confidence')"/>
                            </xsl:with-param>
                        </xsl:call-template>
                    </xsl:when>
                    <xsl:when test="dri:params/@choicesPresentation = 'lookup'">
                        <xsl:call-template name="addLookupButton">
                            <xsl:with-param name="isName" select="'false'"/>
                            <xsl:with-param name="confIndicator" select="$confidenceIndicatorID"/>
                        </xsl:call-template>
                    </xsl:when>
                </xsl:choose>
            </xsl:when>

            <!-- This is changing dramatically -->
            <xsl:when test="@type= 'checkbox' or @type= 'radio'">
                <fieldset>
                    <xsl:call-template name="standardAttributes">
                        <xsl:with-param name="class">
                            <xsl:text>ds-</xsl:text><xsl:value-of select="@type"/><xsl:text>-field </xsl:text>
                            <xsl:if test="dri:error">
                                <xsl:text>error </xsl:text>
                            </xsl:if>
                        </xsl:with-param>
                    </xsl:call-template>
                    <xsl:attribute name="id"><xsl:value-of select="generate-id()"/></xsl:attribute>
                    <xsl:apply-templates />
                </fieldset>
            </xsl:when>
            <!--
                <input>
                            <xsl:call-template name="fieldAttributes"/>
                    <xsl:if test="dri:value[@checked='yes']">
                                <xsl:attribute name="checked">checked</xsl:attribute>
                    </xsl:if>
                    <xsl:apply-templates/>
                </input>
                -->
            <xsl:when test="@type= 'composite'">
                <!-- TODO: add error and help stuff on top of the composite -->
                <span class="ds-composite-field">
                    <xsl:apply-templates select="dri:field" mode="compositeComponent"/>
                </span>
                <xsl:apply-templates select="dri:field/dri:error" mode="compositeComponent"/>
                <xsl:apply-templates select="dri:error" mode="compositeComponent"/>
                <xsl:apply-templates select="dri:field/dri:help" mode="compositeComponent"/>
                <!--<xsl:apply-templates select="dri:help" mode="compositeComponent"/>-->
            </xsl:when>
            <!-- text, password, file, and hidden types are handled the same.
                Buttons: added the xsl:if check which will override the type attribute button
                    with the value 'submit'. No reset buttons for now...
            -->
            <xsl:when test="@type='button'">
                <button>
                    <xsl:call-template name="fieldAttributes"/>
                    <xsl:attribute name="type">submit</xsl:attribute>
                    <xsl:choose>
                        <xsl:when test="dri:value/i18n:text">
                            <xsl:apply-templates select="dri:value/*"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="dri:value"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </button>
            </xsl:when>
            <xsl:otherwise>
                <!--<div class="col-sm-10">-->
                    <input>
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
                        <xsl:apply-templates/>
                    </input>
                <!--</div>-->


                <xsl:variable name="confIndicatorID" select="concat(@id,'_confidence_indicator')"/>
                <xsl:if test="dri:params/@authorityControlled">
                    <xsl:variable name="confidence">
                        <xsl:if test="./dri:value[@type='authority']">
                            <xsl:value-of select="./dri:value[@type='authority']/@confidence"/>
                        </xsl:if>
                    </xsl:variable>
                    <!-- add authority confidence widget -->
                    <xsl:call-template name="authorityConfidenceIcon">
                        <xsl:with-param name="confidence" select="$confidence"/>
                        <xsl:with-param name="id" select="$confidenceIndicatorID"/>
                    </xsl:call-template>
                    <xsl:call-template name="authorityInputFields">
                        <xsl:with-param name="name" select="@n"/>
                        <xsl:with-param name="id" select="@id"/>
                        <xsl:with-param name="authValue" select="dri:value[@type='authority']/text()"/>
                        <xsl:with-param name="confValue" select="dri:value[@type='authority']/@confidence"/>
                    </xsl:call-template>
                </xsl:if>
                <xsl:choose>
                    <xsl:when test="dri:params/@choicesPresentation = 'suggest'">
                        <xsl:call-template name="addAuthorityAutocomplete">
                            <xsl:with-param name="confidenceIndicatorID" select="$confidenceIndicatorID"/>
                            <xsl:with-param name="confidenceName">
                                <xsl:value-of select="concat(@n,'_confidence')"/>
                            </xsl:with-param>
                        </xsl:call-template>
                    </xsl:when>
                    <xsl:when test="dri:params/@choicesPresentation = 'lookup'">
                        <xsl:call-template name="addLookupButton">
                            <xsl:with-param name="isName" select="'false'"/>
                            <xsl:with-param name="confIndicator" select="$confidenceIndicatorID"/>
                        </xsl:call-template>
                    </xsl:when>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="dri:field[@type='checkbox' or @type='radio']/dri:option">
        <div class="{../@type}">
            <label>
                <input>
                    <xsl:attribute name="name">
                        <xsl:value-of select="../@n"/>
                    </xsl:attribute>
                    <xsl:attribute name="type">
                        <xsl:value-of select="../@type"/>
                    </xsl:attribute>
                    <xsl:attribute name="value">
                        <xsl:value-of select="@returnValue"/>
                    </xsl:attribute>
                    <xsl:if test="../dri:value[@type='option'][@option = current()/@returnValue]">
                        <xsl:attribute name="checked">checked</xsl:attribute>
                    </xsl:if>
                    <xsl:if test="../@disabled='yes'">
                        <xsl:attribute name="disabled">disabled</xsl:attribute>
                    </xsl:if>
                </input>
                <xsl:apply-templates/>
            </label>
        </div>

    </xsl:template>



    <!-- The handling of the special case of instanced composite fields under "form" lists -->
    <xsl:template match="dri:field[@type='composite'][dri:field/dri:instance | dri:params/@operations]" mode="formComposite" priority="2">
        <xsl:variable name="confidenceIndicatorID" select="concat(translate(@id,'.','_'),'_confidence_indicator')"/>
        <div class="ds-form-content">
            <div>
                <xsl:attribute name="class">
                    <xsl:text>contol-group row</xsl:text>
                    <xsl:if test="dri:error">
                        <xsl:text> has-error</xsl:text>
                    </xsl:if>
                </xsl:attribute>
                <xsl:apply-templates select="dri:label" mode="compositeLabel"/>
                <xsl:apply-templates select="dri:field" mode="compositeComponent"/>


                <xsl:if test="dri:params/@choicesPresentation = 'lookup' or contains(dri:params/@operations,'add') or dri:params/@choicesPresentation = 'suggest'">
                  <div class="col-xs-2">
                      <xsl:attribute name="class">
                      <xsl:choose>
                          <xsl:when test="dri:params/@choicesPresentation = 'lookup'"><xsl:text>col-xs-3 col-sm-2</xsl:text></xsl:when>
                          <xsl:otherwise><xsl:text>col-xs-2</xsl:text></xsl:otherwise>
                      </xsl:choose>
                  </xsl:attribute>

                    <xsl:if test="dri:field/dri:label">
                        <label>
                            <xsl:attribute name="class">
                                <xsl:text>control-label</xsl:text>
                                <xsl:if test="dri:field/@required = 'yes'">
                                    <xsl:text> required</xsl:text>
                                </xsl:if>
                            </xsl:attribute>
                            <xsl:text>&#160;</xsl:text>
                        </label>
                    </xsl:if>
                    <div class="clearfix">
                        <xsl:if test="contains(dri:params/@operations,'add')">
                        <button type="submit" name="{concat('submit_',@n,'_add')}"
                                class="ds-button-field btn btn-default pull-right ds-add-button">
                            <xsl:if test="dri:params/@choicesPresentation = 'lookup'">
                                <xsl:attribute name="style">
                                    <xsl:text>display:none;</xsl:text>
                                </xsl:attribute>
                            </xsl:if>
                            <!-- Make invisible if we have choice-lookup operation that provides its own Add. -->
                            <i18n:text>xmlui.ChoiceLookupTransformer.add</i18n:text>  <!--TODO needs separate i18n msg-->
                        </button>
                    </xsl:if>

                <xsl:choose>
                    <!-- insert choice mechansim and/or Add button here -->
                    <xsl:when test="dri:params/@choicesPresentation = 'suggest'">
                        <xsl:message terminate="yes">
                            <xsl:text>ERROR: Input field with "suggest" (autocomplete) choice behavior is not implemented for Composite (e.g. "name") fields.</xsl:text>
                        </xsl:message>
                    </xsl:when>
                    <!-- lookup popup includes its own Add button if necessary. -->
                    <xsl:when test="dri:params/@choicesPresentation = 'lookup'">
                        <xsl:call-template name="addLookupButton">
                            <xsl:with-param name="isName" select="'true'"/>
                            <xsl:with-param name="confIndicator" select="$confidenceIndicatorID"/>
                        </xsl:call-template>
                    </xsl:when>
                </xsl:choose>
                    </div>
                    </div>
                </xsl:if>
                <!-- place to store authority value -->
                <xsl:if test="dri:params/@authorityControlled">
                    <xsl:call-template name="authorityConfidenceIcon">
                        <xsl:with-param name="confidence" select="dri:value[@type='authority']/@confidence"/>
                        <xsl:with-param name="id" select="$confidenceIndicatorID"/>
                    </xsl:call-template>
                    <xsl:call-template name="authorityInputFields">
                        <xsl:with-param name="name" select="@n"/>
                        <xsl:with-param name="authValue" select="dri:value[@type='authority']/text()"/>
                        <xsl:with-param name="confValue" select="dri:value[@type='authority']/@confidence"/>
                    </xsl:call-template>
                </xsl:if>
            </div>

            <xsl:apply-templates select="dri:help" mode="help"/>
            <xsl:apply-templates select="dri:error" mode="compositeComponent"/>
            <xsl:apply-templates select="dri:field/dri:error" mode="compositeComponent"/>
            <xsl:if test="dri:instance or dri:field/dri:instance">
                <div class="ds-previous-values">
                    <xsl:call-template name="fieldIterator">
                        <xsl:with-param name="position">1</xsl:with-param>
                    </xsl:call-template>
                    <xsl:if test="contains(dri:params/@operations,'delete') and (dri:instance or dri:field/dri:instance)">
                        <!-- Delete buttons should be named "submit_[field]_delete" so that we can ignore errors from required fields when simply removing values-->
                        <button type="submit" name="{concat('submit_',@n,'_delete')}" class="ds-button-field ds-delete-button btn btn-default">
                            <i18n:text>xmlui.administrative.group.EditGroupForm.submit_remove</i18n:text>  <!--TODO needs separate i18n msg-->
                        </button>
                    </xsl:if>
                    <xsl:for-each select="dri:field">
                        <xsl:apply-templates select="dri:instance" mode="hiddenInterpreter"/>
                    </xsl:for-each>
                </div>
            </xsl:if>
        </div>
    </xsl:template>


    <!-- The iterator is a recursive function that creates a checkbox (to be used in deletion) for
    each value instance and interprets the value inside. It also creates a hidden field from the
    raw value contained in the instance.

     What makes it different from the simpleFieldIterator is that it works with a composite field's
    components rather than a single field, which requires it to consider several sets of instances. -->
    <xsl:template name="fieldIterator">
        <xsl:param name="position"/>
        <!-- add authority value for this instance -->
        <xsl:if test="dri:instance[position()=$position]/dri:value[@type='authority']">
            <xsl:call-template name="authorityInputFields">
                <xsl:with-param name="name" select="@n"/>
                <xsl:with-param name="position" select="$position"/>
                <xsl:with-param name="authValue" select="dri:instance[position()=$position]/dri:value[@type='authority']/text()"/>
                <xsl:with-param name="confValue" select="dri:instance[position()=$position]/dri:value[@type='authority']/@confidence"/>
            </xsl:call-template>
        </xsl:if>
        <xsl:choose>
            <!-- First check to see if the composite itself has a non-empty instance value in that
                position. In that case there is no need to go into the individual fields. -->
            <xsl:when test="count(dri:instance[position()=$position]/dri:value[@type != 'authority'])">
                <div class="checkbox">
                    <input type="checkbox" value="{concat(@n,'_',$position)}" name="{concat(@n,'_selected')}"/>
                    <xsl:apply-templates select="dri:instance[position()=$position]" mode="interpreted"/>
                    <xsl:call-template name="authorityConfidenceIcon">
                        <xsl:with-param name="confidence"
                                        select="dri:instance[position()=$position]/dri:value[@type='authority']/@confidence"/>
                    </xsl:call-template>
                </div>

                <xsl:call-template name="fieldIterator">
                    <xsl:with-param name="position"><xsl:value-of select="$position + 1"/></xsl:with-param>
                </xsl:call-template>
            </xsl:when>
            <!-- Otherwise, build the string from the component fields -->
            <xsl:when test="dri:field/dri:instance[position()=$position]">
                <div class="checkbox">
                    <input type="checkbox" value="{concat(@n,'_',$position)}" name="{concat(@n,'_selected')}"/>
                    <xsl:apply-templates select="dri:field" mode="compositeField">
                        <xsl:with-param name="position" select="$position"/>
                    </xsl:apply-templates>
                </div>

                <xsl:call-template name="fieldIterator">
                    <xsl:with-param name="position"><xsl:value-of select="$position + 1"/></xsl:with-param>
                </xsl:call-template>
            </xsl:when>
        </xsl:choose>
    </xsl:template>


    <!-- NON-instance composite fields (i.e. not repeatable) -->
    <xsl:template match="dri:field[@type='composite']" mode="formComposite">
        <div class="ds-form-content">
            <xsl:variable name="confidenceIndicatorID" select="concat(translate(@id,'.','_'),'_confidence_indicator')"/>
            <div>
                <xsl:attribute name="class">
                    <xsl:text>contol-group</xsl:text>
                    <xsl:if test="dri:error">
                        <xsl:text> has-error</xsl:text>
                    </xsl:if>
                </xsl:attribute>
                <div class="row">
                    <xsl:apply-templates select="dri:label" mode="compositeLabel"/>
                </div>

                <div class="row">
                    <xsl:apply-templates select="dri:field" mode="compositeComponent"/>
                </div>


                <xsl:choose>
                    <xsl:when test="dri:params/@choicesPresentation = 'suggest'">
                        <xsl:message terminate="yes">
                            <xsl:text>ERROR: Input field with "suggest" (autocomplete) choice behavior is not implemented for Composite (e.g. "name") fields.</xsl:text>
                        </xsl:message>
                    </xsl:when>
                    <!-- lookup popup includes its own Add button if necessary. -->
                    <xsl:when test="dri:params/@choicesPresentation = 'lookup'">
                        <xsl:call-template name="addLookupButton">
                            <xsl:with-param name="isName" select="'true'"/>
                            <xsl:with-param name="confIndicator" select="$confidenceIndicatorID"/>
                        </xsl:call-template>
                    </xsl:when>
                </xsl:choose>
                <xsl:if test="dri:params/@authorityControlled">
                    <xsl:variable name="confValue" select="dri:field/dri:value[@type='authority'][1]/@confidence"/>
                    <xsl:call-template name="authorityConfidenceIcon">
                        <xsl:with-param name="confidence" select="$confValue"/>
                        <xsl:with-param name="id" select="$confidenceIndicatorID"/>
                    </xsl:call-template>
                    <xsl:call-template name="authorityInputFields">
                        <xsl:with-param name="name" select="@n"/>
                        <xsl:with-param name="authValue" select="dri:field/dri:value[@type='authority'][1]/text()"/>
                        <xsl:with-param name="confValue" select="$confValue"/>
                    </xsl:call-template>
                </xsl:if>
                <div>
                    <xsl:apply-templates select="dri:help" mode="compositeComponent"/>
                </div>
                <div>
                    <xsl:apply-templates select="dri:error" mode="compositeComponent"/>
                </div>
                <div>
                    <xsl:apply-templates select="dri:field/dri:error" mode="compositeComponent"/>
                </div>
            </div>

        </div>
    </xsl:template>


    <xsl:template match="dri:field" mode="compositeComponent">
        <xsl:variable name="nb_fields" select="count(../dri:field)"/>
        <xsl:variable name="dividend">
            <!--when there's an add or a delete button,
            the fields should only take up 11 columns-->
            <xsl:choose>
                <xsl:when test="../dri:params[@operations]">
                    <xsl:choose>
                        <xsl:when test="position() = last() and contains(../dri:params/@choicesPresentation,'lookup')">
                            <xsl:value-of select="9"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:number value="10"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:number value="12"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="rounded" select="floor($dividend div $nb_fields)"/>
        <xsl:variable name="sm_size">
            <xsl:choose>
                <xsl:when test="position() = last()">

                    <xsl:value-of select="$rounded + ($dividend mod $nb_fields)"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$rounded"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <div>
            <xsl:attribute name="class">
                <xsl:text>col-xs-</xsl:text><xsl:value-of select="$dividend"/>
                <xsl:text> col-sm-</xsl:text><xsl:value-of select="$sm_size"/>
                <xsl:if test="not(position() = last())">
                    <xsl:text> needs-xs-spacing</xsl:text>
                </xsl:if>

            </xsl:attribute>
            <p>
            <xsl:choose>
                <xsl:when test="@type = 'checkbox'  or @type='radio'">
                    <xsl:apply-templates select="." mode="normalField"/>
                    <xsl:if test="dri:label">
                        <br/>
                        <xsl:apply-templates select="dri:label" mode="compositeComponent"/>
                    </xsl:if>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:if test="../dri:field/dri:label">
                        <label class="ds-composite-component control-label" for="{translate(@id, '.', '_')}">
                            <xsl:choose>
                                <xsl:when test="dri:label">
                                    <xsl:apply-templates select="dri:label" mode="compositeComponent"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:text>&#160;</xsl:text>
                                </xsl:otherwise>
                            </xsl:choose>

                        </label>
                    </xsl:if>
                    <xsl:apply-templates select="." mode="normalField"/>
            </xsl:otherwise>
            </xsl:choose>
            </p>
        </div>

    </xsl:template>


    <!-- Fieldset (instanced) field stuff, in the case of non-composites -->
    <xsl:template match="dri:field[dri:field/dri:instance | dri:params/@operations]" priority="2">
        <xsl:choose>
            <xsl:when test="contains(dri:params/@operations,'add')">
                <div class="row">
                    <div class="col-xs-10">
                        <xsl:apply-templates select="." mode="normalField"/>
                    </div>

                    <div class="col-xs-2">
                        <button type="submit" name="{concat('submit_',@n,'_add')}"
                                class="pull-right ds-button-field btn btn-default ds-add-button">
                            <!-- Make invisible if we have choice-lookup popup that provides its own Add. -->
                            <xsl:if test="dri:params/@choicesPresentation = 'lookup'">
                                <xsl:attribute name="style">
                                    <xsl:text>display:none;</xsl:text>
                                </xsl:attribute>
                            </xsl:if>
                            <i18n:text>xmlui.ChoiceLookupTransformer.add</i18n:text>
                            <!--TODO needs separate i18n msg-->
                        </button>
                    </div>
                </div>


            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates select="." mode="normalField"/>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:apply-templates select="dri:help" mode="help"/>
        <xsl:apply-templates select="dri:error" mode="error"/>
        <xsl:if test="dri:instance">
            <div class="ds-previous-values">
                <!-- Iterate over the dri:instance elements contained in this field. The instances contain
                    stored values as either "interpreted", "raw", or "default" values. -->
                <xsl:call-template name="simpleFieldIterator">
                    <xsl:with-param name="position">1</xsl:with-param>
                </xsl:call-template>
                <!-- Conclude with a DELETE button if the delete operation is specified. This allows
                    removing one or more values stored for this field. -->
                <xsl:if test="contains(dri:params/@operations,'delete') and dri:instance">
                    <!-- Delete buttons should be named "submit_[field]_delete" so that we can ignore errors from required fields when simply removing values-->
                    <p>
                    <button type="submit" name="{concat('submit_',@n,'_delete')}" class="ds-button-field btn btn-default ds-delete-button">
                        <i18n:text>xmlui.administrative.group.EditGroupForm.submit_remove</i18n:text>  <!--TODO needs separate i18n msg-->
                    </button>
                    </p>
                </xsl:if>
                <!-- Behind the scenes, add hidden fields for every instance set. This is to make sure that
                    the form still submits the information in those instances, even though they are no
                    longer encoded as HTML fields. The DRI Reference should contain the exact attributes
                    the hidden fields should have in order for this to work properly. -->
                <xsl:apply-templates select="dri:instance" mode="hiddenInterpreter"/>
            </div>
        </xsl:if>
    </xsl:template>


    <!-- The iterator is a recursive function that creates a checkbox (to be used in deletion) for
        each value instance and interprets the value inside. It also creates a hidden field from the
        raw value contained in the instance. -->
    <xsl:template name="simpleFieldIterator">
        <xsl:param name="position"/>
        <xsl:if test="dri:instance[position()=$position]">
            <div class="checkbox">
                <label>
                    <input type="checkbox" value="{concat(@n,'_',$position)}" name="{concat(@n,'_selected')}"/>
                    <xsl:apply-templates select="dri:instance[position()=$position]" mode="interpreted"/>

                    <!-- look for authority value in instance. -->
                    <xsl:if test="dri:instance[position()=$position]/dri:value[@type='authority']">
                        <xsl:call-template name="authorityConfidenceIcon">
                            <xsl:with-param name="confidence"
                                            select="dri:instance[position()=$position]/dri:value[@type='authority']/@confidence"/>
                        </xsl:call-template>
                    </xsl:if>
                </label>
            </div>

            <xsl:call-template name="simpleFieldIterator">
                <xsl:with-param name="position"><xsl:value-of select="$position + 1"/></xsl:with-param>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <xsl:template match="dri:p[count(dri:field) = 2 and dri:field[@type='button'] and dri:field[not(@type='button')]]" priority="4">
        <p>
            <xsl:apply-templates select="*[not(name()='field')]"/>
        </p>
        <div class="row">
            <div>
                <xsl:call-template name="standardAttributes">
                    <xsl:with-param name="class">
                        <xsl:text>col-sm-6</xsl:text>
                    </xsl:with-param>
                </xsl:call-template>
                <p class="input-group">
                    <xsl:apply-templates select="dri:field[not(@type='button')]"/>
                    <span class="input-group-btn">
                        <xsl:apply-templates select="dri:field[@type='button']"/>
                    </span>
                 </p>
            </div>
        </div>
    </xsl:template>

    <xsl:template match="dri:item[count(dri:field) = 2 and dri:field[@type='button'] and dri:field[not(@type='button')]]" priority="4" mode="labeled">
        <div class="row">
            <div class="col-sm-2">
                <xsl:call-template name="pick-label"/>
            </div>

            <div>
                <xsl:call-template name="standardAttributes">
                    <xsl:with-param name="class">
                        <xsl:text>input-group col-sm-4</xsl:text>
                    </xsl:with-param>
                </xsl:call-template>
                <xsl:apply-templates select="dri:field[not(@type='button')]"/>
                <span class="input-group-btn">
                    <xsl:apply-templates select="dri:field[@type='button']"/>
                </span>

            </div>
        </div>
    </xsl:template>

    <xsl:template match="dri:*[count(dri:field) > 1 and dri:field[@type='button'] and count(dri:field[not(@type='button' or @type='')]) = 0]" priority="4">
        <div>
            <xsl:call-template name="standardAttributes">
                <!--<xsl:with-param name="class">-->
                    <!--<xsl:text>btn-group</xsl:text>-->
                <!--</xsl:with-param>-->
            </xsl:call-template>
            <p class="btn-group">
            <xsl:apply-templates/>
            </p>
        </div>
    </xsl:template>

    <!--a list that contains only buttons should be rendered as a button group-->
    <xsl:template match="dri:list[dri:item/dri:field[@type='button'] and count(dri:item/dri:field[not(@type='button')]) + count(*[not(name() = 'item')]) = 0]" priority="4">
        <div>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">
                    <xsl:text>btn-group</xsl:text>
                </xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="dri:item/dri:field"/>
        </div>
    </xsl:template>

    <!--<xsl:template match="dri:list[@type='progress'][dri:item/dri:field[@type='button'] and count(dri:item/dri:field[not(@type='button')]) = 0]" priority="4">-->
        <!--<div>-->
            <!--<xsl:call-template name="standardAttributes">-->
                <!--<xsl:with-param name="class">btn-group</xsl:with-param>-->
            <!--</xsl:call-template>-->
            <!--<xsl:apply-templates select="dri:item/dri:field"/>-->
        <!--</div>-->
    <!--</xsl:template>-->

    <xsl:template name="pick-label">
        <xsl:choose>
            <xsl:when test="dri:field/dri:label">
                <!--<label class="control-label col-sm-2">-->
                <label>
                    <xsl:attribute name="class">
                        <xsl:text>control-label</xsl:text>
                        <xsl:if test="dri:field/@required = 'yes'">
                            <xsl:text> required</xsl:text>
                        </xsl:if>
                    </xsl:attribute>
                    <xsl:choose>
                        <xsl:when test="./dri:field/@id">
                            <xsl:attribute name="for">
                                <xsl:value-of select="translate(./dri:field/@id,'.','_')"/>
                            </xsl:attribute>
                        </xsl:when>
                        <xsl:otherwise></xsl:otherwise>
                    </xsl:choose>
                    <xsl:apply-templates select="dri:field/dri:label" mode="formComposite"/>
                    <xsl:text>:&#160;</xsl:text>
                </label>
            </xsl:when>
            <xsl:when test="string-length(string(preceding-sibling::*[1][local-name()='label'])) > 0">
                <label>
                    <xsl:apply-templates select="preceding-sibling::*[1][local-name()='label']"/>
                    <xsl:text>:&#160;</xsl:text>
                </label>
            </xsl:when>
            <xsl:when test="dri:field">
                <xsl:choose>
                    <xsl:when test="preceding-sibling::*[1][local-name()='label']">
                        <xsl:if test="string-length(string(preceding-sibling::*[1][local-name()='label'])) > 0">
                            <label class="ds-form-label">
                                <xsl:choose>
                                    <xsl:when test="./dri:field/@id">
                                        <xsl:attribute name="for">
                                            <xsl:value-of select="translate(./dri:field/@id,'.','_')"/>
                                        </xsl:attribute>
                                    </xsl:when>
                                    <xsl:otherwise></xsl:otherwise>
                                </xsl:choose>
                                <xsl:apply-templates select="preceding-sibling::*[1][local-name()='label']"/>
                            </label>
                        </xsl:if>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:apply-templates select="preceding-sibling::*[1][local-name()='label']"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <!-- If the label is empty and the item contains no field, omit the label. This is to
                    make the text inside the item (since what else but text can be there?) stretch across
                    both columns of the list. -->
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="dri:field/dri:label" mode="compositeLabel">
        <div class="col-sm-12">
            <label>
                <xsl:attribute name="class">
                    <xsl:text>control-label</xsl:text>
                    <xsl:if test="../@required = 'yes'">
                        <xsl:text> required</xsl:text>
                    </xsl:if>
                </xsl:attribute>
                <xsl:apply-templates/>
                <xsl:text>:&#160;</xsl:text>
            </label>
        </div>
    </xsl:template>

    <!-- A set of standard attributes common to all fields -->
    <xsl:template name="fieldAttributes">
        <xsl:call-template name="standardAttributes">
            <xsl:with-param name="class">
                <xsl:text>ds-</xsl:text><xsl:value-of select="@type"/><xsl:text>-field </xsl:text>
            <xsl:choose>
                <xsl:when test="@type='button'">
                    <xsl:text>btn</xsl:text>
                    <xsl:if test="not(contains(@rend, 'btn-'))">
                        <xsl:text> btn-default</xsl:text>
                    </xsl:if>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>form-control </xsl:text>
                </xsl:otherwise>
            </xsl:choose>

                <xsl:if test="@rend">
                    <xsl:text> </xsl:text>
                    <xsl:value-of select="@rend
                    "/>
                </xsl:if>
                <xsl:if test="dri:error or parent::node()[@type='composite']/dri:error">
                    <xsl:text>input-with-feedback </xsl:text>
                </xsl:if>
            </xsl:with-param>
            <xsl:with-param name="placeholder">
                <xsl:if test="@placeholder">
                    <xsl:value-of select="@placeholder"/>
                </xsl:if>
            </xsl:with-param>
        </xsl:call-template>
        <!--<xsl:if test="@required='yes'">-->
            <!--<xsl:attribute name="required">required</xsl:attribute>-->
        <!--</xsl:if>-->
        <xsl:if test="@disabled='yes' or ../@rend = 'disabled'">
            <xsl:attribute name="disabled">disabled</xsl:attribute>
        </xsl:if>
        <xsl:if test="@type != 'checkbox' and @type != 'radio' ">
            <xsl:attribute name="name"><xsl:value-of select="@n"/></xsl:attribute>
        </xsl:if>
        <xsl:if test="@type != 'select' and @type != 'textarea' and @type != 'checkbox' and @type != 'radio' ">
            <xsl:attribute name="type">
                <xsl:choose>
                    <xsl:when test="@n = 'login_email'">
                        <xsl:text>email</xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="@type"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>

        </xsl:if>
        <xsl:if test="@type= 'textarea'">
            <xsl:attribute name="onfocus">javascript:tFocus(this);</xsl:attribute>
        </xsl:if>
    </xsl:template>

    <!--errors are already put in the tooltip, and a modern browser will show that tooltip when-->
    <xsl:template match="dri:error" mode="error">
        <xsl:if test="./text() or ./node()">
            <p class="alert alert-danger">
                <xsl:apply-templates/>
            </p>
        </xsl:if>
    </xsl:template>

    <xsl:template match="dri:help" mode="help">
        <!--Only create the <span> if there is content in the <dri:help> node-->
        <xsl:if test="./text() or ./node()">
            <!--<p class="help-block col-sm-10 col-offset-2">-->
            <p class="help-block">
                <xsl:apply-templates />
            </p>
        </xsl:if>
    </xsl:template>


    <xsl:template match="dri:help" mode="compositeComponent">
        <xsl:apply-templates select="." mode="help"/>
    </xsl:template>

    <xsl:template match="dri:list[@rend='horizontalVanilla'][@n='selectlist']/dri:item/dri:figure[contains(@source,'Reference/images/information.png')]">

        <a href="javascript:void(0)" class="information">
                <xsl:attribute name="title">
                    <xsl:value-of select="@title"/>
                </xsl:attribute>
        <span class="glyphicon glyphicon-info-sign btn-xs active"/>
        </a>
    </xsl:template>

</xsl:stylesheet>
