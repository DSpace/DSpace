// $Id$

/**
 * The ParameterStore, as its name suggests, stores Solr parameters. Widgets
 * expose some of these parameters to the user. Whenever the user changes the
 * values of these parameters, the state of the application changes. In order to
 * allow the user to move back and forth between these states with the browser's
 * Back and Forward buttons, and to bookmark these states, each state needs to
 * be stored. The easiest method is to store the exposed parameters in the URL
 * hash (see the <tt>ParameterHashStore</tt> class). However, you may implement
 * your own storage method by extending this class.
 *
 * <p>For a list of possible parameters, please consult the links below.</p>
 *
 * @see http://wiki.apache.org/solr/CoreQueryParameters
 * @see http://wiki.apache.org/solr/CommonQueryParameters
 * @see http://wiki.apache.org/solr/SimpleFacetParameters
 * @see http://wiki.apache.org/solr/HighlightingParameters
 * @see http://wiki.apache.org/solr/MoreLikeThis
 * @see http://wiki.apache.org/solr/SpellCheckComponent
 * @see http://wiki.apache.org/solr/StatsComponent
 * @see http://wiki.apache.org/solr/TermsComponent
 * @see http://wiki.apache.org/solr/TermVectorComponent
 * @see http://wiki.apache.org/solr/LocalParams
 *
 * @param properties A map of fields to set. Refer to the list of public fields.
 * @class ParameterStore
 */
AjaxSolr.ParameterStore = AjaxSolr.Class.extend(
  /** @lends AjaxSolr.ParameterStore.prototype */
  {
  /**
   * The names of the exposed parameters. Any parameters that your widgets
   * expose to the user, directly or indirectly, should be listed here.
   *
   * @field
   * @public
   * @type String[]
   * @default []
   */
  exposed: [],

  /**
   * The Solr parameters.
   *
   * @field
   * @private
   * @type Object
   * @default {}
   */
  params: {},

  /**
   * A reference to the parameter store's manager. For internal use only.
   *
   * @field
   * @private
   * @type AjaxSolr.AbstractManager
   */
  manager: null,

  /**
   * An abstract hook for child implementations.
   *
   * <p>This method should do any necessary one-time initializations.</p>
   */
  init: function () {},

  /**
   * Some Solr parameters may be specified multiple times. It is easiest to
   * hard-code a list of such parameters. You may change the list by passing
   * <code>{ multiple: /pattern/ }</code> as an argument to the constructor of
   * this class or one of its children, e.g.:
   *
   * <p><code>new ParameterStore({ multiple: /pattern/ })</code>
   *
   * @param {String} name The name of the parameter.
   * @returns {Boolean} Whether the parameter may be specified multiple times.
   */
  isMultiple: function (name) {
    return name.match(/^(?:bf|bq|facet\.date|facet\.date\.other|facet\.field|facet\.query|fq|pf|qf)$/);
  },

  /**
   * Returns a parameter. If the parameter doesn't exist, creates it.
   *
   * @param {String} name The name of the parameter.
   * @returns {AjaxSolr.Parameter|AjaxSolr.Parameter[]} The parameter.
   */
  get: function (name) {
    if (this.params[name] === undefined) {
      var param = new AjaxSolr.Parameter({ name: name });
      if (this.isMultiple(name)) {
        this.params[name] = [ param ];
      }
      else {
        this.params[name] = param;
      }
    }
    return this.params[name];
  },

  /**
   * If the parameter may be specified multiple times, returns the values of
   * all identically-named parameters. If the parameter may be specified only
   * once, returns the value of that parameter.
   *
   * @param {String} name The name of the parameter.
   * @returns {String[]|Number[]} The value(s) of the parameter.
   */
  values: function (name) {
    if (this.params[name] !== undefined) {
      if (this.isMultiple(name)) {
        var values = [];
        for (var i = 0, l = this.params[name].length; i < l; i++) {
          values.push(this.params[name][i].val());
        }
        return values;
      }
      else {
        return [ this.params[name].val() ];
      }
    }
    return [];
  },

  /**
   * If the parameter may be specified multiple times, adds the given parameter
   * to the list of identically-named parameters, unless one already exists with
   * the same value. If it may be specified only once, replaces the parameter.
   *
   * @param {String} name The name of the parameter.
   * @param {AjaxSolr.Parameter} [param] The parameter.
   * @returns {AjaxSolr.Parameter|Boolean} The parameter, or false.
   */
  add: function (name, param) {
    if (param === undefined) {
      param = new AjaxSolr.Parameter({ name: name });
    }
    if (this.isMultiple(name)) {
      if (this.params[name] === undefined) {
        this.params[name] = [ param ];
      }
      else {
        if (AjaxSolr.inArray(param.val(), this.values(name)) == -1) {
          this.params[name].push(param);
        }
        else {
          return false;
        }
      }
    }
    else {
      this.params[name] = param;
    }
    return param;
  },

  /**
   * Deletes a parameter.
   *
   * @param {String} name The name of the parameter.
   * @param {Number} [index] The index of the parameter.
   */
  remove: function (name, index) {
    if (index === undefined) {
      delete this.params[name];
    }
    else {
      this.params[name].splice(index, 1);
      if (this.params[name].length == 0) {
        delete this.params[name];
      }
    }
  },

  /**
   * Finds all parameters with matching values.
   *
   * @param {String} name The name of the parameter.
   * @param {String|Number|String[]|Number[]|RegExp} value The value.
   * @returns {String|Number[]} The indices of the parameters found.
   */
  find: function (name, value) {
    if (this.params[name] !== undefined) {
      if (this.isMultiple(name)) {
        var indices = [];
        for (var i = 0, l = this.params[name].length; i < l; i++) {
          if (AjaxSolr.equals(this.params[name][i].val(), value)) {
            indices.push(i);
          }
        }
        return indices.length ? indices : false;
      }
      else {
        if (AjaxSolr.equals(this.params[name].val(), value)) {
          return name;
        }
      }
    }
    return false;
  },

  /**
   * If the parameter may be specified multiple times, creates a parameter using
   * the given name and value, and adds it to the list of identically-named
   * parameters, unless one already exists with the same value. If it may be
   * specified only once, replaces the parameter.
   *
   * @param {String} name The name of the parameter.
   * @param {String|Number|String[]|Number[]} value The value.
   * @returns {AjaxSolr.Parameter|Boolean} The parameter, or false.
   */
  addByValue: function (name, value) {
    if (this.isMultiple(name) && AjaxSolr.isArray(value)) {
      var ret = [];
      for (var i = 0, l = value.length; i < l; i++) {
        ret.push(this.add(name, new AjaxSolr.Parameter({ name: name, value: value[i] })));
      }
      return ret;
    }
    else {
      return this.add(name, new AjaxSolr.Parameter({ name: name, value: value }))
    }
  },

  /**
   * Deletes any parameter with a matching value.
   *
   * @param {String} name The name of the parameter.
   * @param {String|Number|String[]|Number[]|RegExp} value The value.
   * @returns {String|Number[]} The indices deleted.
   */
  removeByValue: function (name, value) {
    var indices = this.find(name, value);
    if (indices) {
      if (AjaxSolr.isArray(indices)) {
        for (var i = indices.length - 1; i >= 0; i--) {
          this.remove(name, indices[i]);
        }
      }
      else {
        this.remove(indices);
      }
    }
    return indices;
  },

  /**
   * Returns the Solr parameters as a query string.
   *
   * <p>IE6 calls the default toString() if you write <tt>store.toString()
   * </tt>. So, we need to choose another name for toString().</p>
   */
  string: function () {
    var params = [];
    for (var name in this.params) {
      if (this.isMultiple(name)) {
        for (var i = 0, l = this.params[name].length; i < l; i++) {
          params.push(this.params[name][i].string());
        }
      }
      else {
        params.push(this.params[name].string());
      }
    }
    return AjaxSolr.compact(params).join('&');
  },

  /**
   * Parses a query string into Solr parameters.
   *
   * @param {String} str The string to parse.
   */
  parseString: function (str) {
    var pairs = str.split('&');
    for (var i = 0, l = pairs.length; i < l; i++) {
      if (pairs[i]) { // ignore leading, trailing, and consecutive &'s
        var param = new AjaxSolr.Parameter();
        param.parseString(pairs[i]);
        this.add(param.name, param);
      }
    }
  },

  /**
   * Returns the exposed parameters as a query string.
   *
   * @returns {String} A string representation of the exposed parameters.
   */
  exposedString: function () {
    var params = [];
    for (var i = 0, l = this.exposed.length; i < l; i++) {
      if (this.params[this.exposed[i]] !== undefined) {
        if (this.isMultiple(this.exposed[i])) {
          for (var j = 0, m = this.params[this.exposed[i]].length; j < m; j++) {
            params.push(this.params[this.exposed[i]][j].string());
          }
        }
        else {
          params.push(this.params[this.exposed[i]].string());
        }
      }
    }
    return AjaxSolr.compact(params).join('&');
  },

  /**
   * Resets the values of the exposed parameters.
   */
  exposedReset: function () {
    for (var i = 0, l = this.exposed.length; i < l; i++) {
      this.remove(this.exposed[i]);
    }
  },

  /**
   * Loads the values of exposed parameters from persistent storage. It is
   * necessary, in most cases, to reset the values of exposed parameters before
   * setting the parameters to the values in storage. This is to ensure that a
   * parameter whose name is not present in storage is properly reset.
   *
   * @param {Boolean} [reset=true] Whether to reset the exposed parameters.
   *   before loading new values from persistent storage. Default: true.
   */
  load: function (reset) {
    if (reset === undefined) {
      reset = true;
    }
    if (reset) {
      this.exposedReset();
    }
    this.parseString(this.storedString());
  },
  
  /**
   * An abstract hook for child implementations.
   *
   * <p>Stores the values of the exposed parameters in persistent storage. This
   * method should usually be called before each Solr request.</p>
   */
  save: function () {},

  /**
   * An abstract hook for child implementations.
   *
   * <p>Returns the string to parse from persistent storage.</p>
   *
   * @returns {String} The string from persistent storage.
   */
  storedString: function () {
    return '';
  }
});
