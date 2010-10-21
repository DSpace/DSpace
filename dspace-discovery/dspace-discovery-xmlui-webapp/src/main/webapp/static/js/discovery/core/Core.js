// $Id$

/**
 * @namespace A unique namespace for the AJAX Solr library.
 */
AjaxSolr = function () {};

/**
 * @namespace Baseclass for all classes
 */
AjaxSolr.Class = function () {};

/**
 * A class 'extends' itself into a subclass.
 *
 * @static
 * @param properties The properties of the subclass.
 * @returns A function that represents the subclass.
 */
AjaxSolr.Class.extend = function (properties) {
  var klass = this; // Safari dislikes 'class'
  // The subclass is just a function that when called, instantiates itself.
  // Nothing is _actually_ shared between _instances_ of the same class.
  var subClass = function (options) {
    // 'this' refers to the subclass, which starts life as an empty object.
    // Add its parent's properties, its own properties, and any passed options.
    AjaxSolr.extend(this, new klass(options), properties, options);
  }
  // Allow the subclass to extend itself into further subclasses.
  subClass.extend = this.extend;
  return subClass;
};

/**
 * @static
 * @param {Object} obj Any object.
 * @returns {Number} the number of properties on an object.
 * @see http://stackoverflow.com/questions/5223/length-of-javascript-associative-array
 */
AjaxSolr.size = function (obj) {
  var size = 0;
  for (var key in obj) {
    if (obj.hasOwnProperty(key)) {
      size++;
    }
  }
  return size;
};

/**
 * @static
 * @param foo A value.
 * @param bar A value.
 * @returns {Boolean} Whether the two given values are equal.
 */
AjaxSolr.equals = function (foo, bar) {
  if (AjaxSolr.isArray(foo) && AjaxSolr.isArray(bar)) {
    if (foo.length !== bar.length) {
      return false;
    }
    for (var i = 0, l = foo.length; i < l; i++) {
      if (foo[i] !== bar[i]) {
        return false;
      }
    }
    return true;
  }
  else if (AjaxSolr.isRegExp(foo) && AjaxSolr.isString(bar)) {
    return bar.match(foo);
  }
  else if (AjaxSolr.isRegExp(bar) && AjaxSolr.isString(foo)) {
    return foo.match(bar);
  }
  else {
    return foo === bar;
  }
};

/**
 * @static
 * @param value A value.
 * @param array An array.
 * @returns {Boolean} Whether value exists in the array.
 */
AjaxSolr.inArray = function (value, array) {
  if (array) {
    for (var i = 0, l = array.length; i < l; i++) {
      if (AjaxSolr.equals(array[i], value)) {
        return i;
      }
    }
  }
  return -1;
};

/**
 * A copy of MooTools' Array.flatten function.
 *
 * @static
 * @see http://ajax.googleapis.com/ajax/libs/mootools/1.2.4/mootools.js
 */
AjaxSolr.flatten = function(array) {
  var ret = [];
  for (var i = 0, l = array.length; i < l; i++) {
    ret = ret.concat(AjaxSolr.isArray(array[i]) ? AjaxSolr.flatten(array[i]) : array[i]);
  }
  return ret;
};

/**
 * A copy of jQuery's jQuery.grep function.
 *
 * @static
 * @see http://ajax.googleapis.com/ajax/libs/jquery/1.3.2/jquery.js
 */
AjaxSolr.grep = function(array, callback) {
  var ret = [];
  for (var i = 0, l = array.length; i < l; i++) {
    if (!callback(array[i], i) === false) {
      ret.push(array[i]);
    }
  }
  return ret;
}

/**
 * Equivalent to Ruby's Array#compact.
 */
AjaxSolr.compact = function(array) {
  return AjaxSolr.grep(array, function (item) {
    return item.toString();
  });
}

/**
 * Can't use toString.call(obj) === "[object Array]", as it may return
 * "[xpconnect wrapped native prototype]", which is undesirable.
 *
 * @static
 * @see http://thinkweb2.com/projects/prototype/instanceof-considered-harmful-or-how-to-write-a-robust-isarray/
 * @see http://ajax.googleapis.com/ajax/libs/prototype/1.6.0.3/prototype.js
 */
AjaxSolr.isArray = function (obj) {
  return obj != null && typeof obj == 'object' && 'splice' in obj && 'join' in obj;
};

/**
 * @param obj Any object.
 * @returns {Boolean} Whether the object is a RegExp object.
 */
AjaxSolr.isRegExp = function (obj) {
  return obj != null && (typeof obj == 'object' || typeof obj == 'function') && 'ignoreCase' in obj;
};

/**
 * @param obj Any object.
 * @returns {Boolean} Whether the object is a String object.
 */
AjaxSolr.isString = function (obj) {
  return obj != null && typeof obj == 'string';
};

/**
 * Define theme functions to separate, as much as possible, your HTML from your
 * JavaScript. Theme functions provided by AJAX Solr are defined in the
 * AjaxSolr.theme.prototype namespace, e.g. AjaxSolr.theme.prototype.select_tag.
 *
 * To override a theme function provided by AJAX Solr, define a function of the
 * same name in the AjaxSolr.theme namespace, e.g. AjaxSolr.theme.select_tag.
 *
 * To retrieve the HTML output by AjaxSolr.theme.prototype.select_tag(...), call
 * AjaxSolr.theme('select_tag', ...).
 *
 * @param {String} func
 *   The name of the theme function to call.
 * @param ...
 *   Additional arguments to pass along to the theme function.
 * @returns
 *   Any data the theme function returns. This could be a plain HTML string,
 *   but also a complex object.
 *
 * @static
 * @throws Exception if the theme function is not defined.
 * @see http://cvs.drupal.org/viewvc.py/drupal/drupal/misc/drupal.js?revision=1.58
 */
AjaxSolr.theme = function (func) {
  for (var i = 1, args = []; i < arguments.length; i++) {
    args.push(arguments[i]);
  }
  try {
    return (AjaxSolr.theme[func] || AjaxSolr.theme.prototype[func]).apply(this, args);
  }
  catch (e) {
    if (console && console.log) {
      console.log('Theme function "' + func + '" is not defined.');
    }
    throw e;
  }
};

/**
 * A simplified version of jQuery's extend function.
 *
 * @static
 * @see http://ajax.googleapis.com/ajax/libs/jquery/1.2.6/jquery.js
 */
AjaxSolr.extend = function () {
  var target = arguments[0] || {}, i = 1, length = arguments.length, options;
  for (; i < length; i++) {
    if ((options = arguments[i]) != null) {
      for (var name in options) {
        var src = target[name], copy = options[name];
        if (target === copy) {
          continue;
        }
        if (copy && typeof copy == 'object' && !copy.nodeType) {
          target[name] = AjaxSolr.extend(src || (copy.length != null ? [] : {}), copy);
        }
        else if (copy !== undefined) {
          target[name] = copy;
        }
      }
    }
  }
  return target;
};
