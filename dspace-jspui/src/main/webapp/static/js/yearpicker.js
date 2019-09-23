const yearPickerVersion = "1.0.0";
const yearPickerAppName = "YearPicker";

var defaults = {
  // Auto Hide
  autoHide: true,
  // The Initial Date
  year: null,
  // Start Date
  startYear: null,
  // End Date
  endYear: null,
  // A element tag items
  itemTag: "li",
  //css class selected date item
  selectedClass: "selected",
  // css class disabled
  disabledClass: "disabled",
  hideClass: "hide",
  highlightedClass: "highlighted",
  template: `<div class="yearpicker-container">
                    <div class="yearpicker-header">
                        <div class="yearpicker-prev" data-view="yearpicker-prev">&lsaquo;</div>
                        <div class="yearpicker-current" data-view="yearpicker-current">SelectedYear</div>
                        <div class="yearpicker-next" data-view="yearpicker-next">&rsaquo;</div>
                    </div>
                    <div class="yearpicker-body">
                        <ul class="yearpicker-year" data-view="years">
                        </ul>
                    </div>
                </div>
`,

  // Event shortcuts
  show: null,
  hide: null,
  pick: null
};

var window = typeof window !== "undefained" ? window : {};

var event_click = "click.";
var event_focus = "focus.";
var event_keyup = "keyup.";
var event_selected = "selected.";
var event_show = "show.";
var event_hide = "hide.";

var methods = {
  // Show datepicker
  showView: function showView() {
    if (!this.build) {
      this.init();
    }

    if (this.show) {
      return;
    }

    if (this.trigger(event_show).isDefaultPrevented()) {
      return;
    }
    this.show = true;
    var $template = this.$template,
      options = this.options;

    $template
      .removeClass(options.hideClass)
      .on(event_click, $.proxy(this.click, this));
    $(document).on(
      event_click,
      (this.onGlobalClick = proxy(this.globalClick, this))
    );
    this.place();
  },

  // Hide the datepicker
  hideView: function hideView() {
    if (!this.show) {
      return;
    }

    if (this.trigger(event_hide).isDefaultPrevented()) {
      return;
    }

    var $template = this.$template,
      options = this.options;

    $template.addClass(options.hideClass).off(event_click, this.click);
    $(document).off(event_click, this.onGlobalClick);
    this.show = false;
  },
  // toggle show and hide
  toggle: function toggle() {
    if (this.show) {
      this.hideView();
    } else {
      this.show();
    }
  },
  setStartYear: function setStartYear(year) {
    this.startYear = year;

    if (this.build) {
      this.render();
    }
  },
  setEndYear: function setEndYear(year) {
    this.endYear = year;
    if (this.build) {
      this.render();
    }
  }
};

var handlers = {
  click: function click(e) {
    var $target = $(e.target);
    var options = this.options;
    var viewYear = this.viewYear;
    if ($target.hasClass("disabled")) {
      return;
    }
    var view = $target.data("view");
    switch (view) {
      case "yearpicker-prev":
        var year = viewYear - 12;
        this.viewYear = year;
        this.renderYear();
        break;
      case "yearpicker-next":
        var year = viewYear + 12;
        this.viewYear = year;
        this.renderYear();
        break;
      case "yearpicker-items":
        this.year = parseInt($target.html());
        this.renderYear();
        this.hideView();
        break;
      default:
        break;
    }
  },
  globalClick: function globalClick(_ref) {
    var target = _ref.target;
    var element = this.element;
    var hidden = true;

    if (target !== document) {
      while (
        target === element ||
        $(target).closest(".yearpicker-header").length === 1
      ) {
        hidden = false;
        break;
      }

      target = target.parentNode;
    }

    if (hidden) {
      this.hideView();
    }
  }
};

var render = {
  renderYear: function renderYear() {
    var options = this.options,
      startYear = options.startYear,
      endYear = options.endYear;
    var disabledClass = options.disabledClass;

    // viewed year in the calenter
    var viewYear = this.viewYear;
    // selected year
    var selectedYear = this.year;
    var now = new Date();
    // current year
    var thisYear = now.getFullYear();

    var start = -5;
    var end = 6;
    var items = [];
    var prevDisabled = false;
    var nextDisabled = false;
    var i = void 0;

    for (i = start; i <= end; i++) {
      var year = viewYear + i;
      var disabled = false;

      if (startYear) {
        disabled = year < startYear;
        if (i === start) {
          prevDisabled = disabled;
        }
      }

      if (!disabled && endYear) {
        disabled = year > endYear;
        if (i === end) {
          nextDisabled = disabled;
        }
      }

      // check for this is a selected year
      var isSelectedYear = year === selectedYear;
      var view = isSelectedYear ? "yearpicker-items" : "yearpicker-items";
      items.push(
        this.createItem({
          selected: isSelectedYear,
          disabled: disabled,
          text: viewYear + i,
          //view: disabled ? "yearpicker disabled" : view,
          view: disabled ? "yearpicker-items disabled" : view,
          highlighted: year === thisYear
        })
      );
    }

    this.yearsPrev.toggleClass(disabledClass, prevDisabled);
    this.yearsNext.toggleClass(disabledClass, nextDisabled);
    this.yearsCurrent.html(selectedYear);
    this.yearsBody.html(items.join(" "));
    this.setValue();
  }
};

function isString(value) {
  return typeof value === "string";
}

function isNumber(value) {
  return typeof value === "number" && value !== "NaN";
}

function isUndefained(value) {
  return typeof value === "undefined";
}

function proxy(fn, context) {
  for (
    var len = arguments.length, args = Array(len > 2 ? len - 2 : 0), key = 2;
    key < len;
    key++
  ) {
    args[key - 2] = arguments[key];
  }

  return function() {
    for (
      var len2 = arguments.length, args2 = Array(len2), key2 = 0;
      key2 < len2;
      key2++
    ) {
      args2[key2] = arguments[key2];
    }

    return fn.apply(context, args.concat(args2));
  };
}

("use strict");

var _setupError = "YearPicker Error";
if (isUndefained(jQuery)) {
  alert(`${yearPickerAppName} ${yearPickerVersion} requires jQuery`);
}

var classCheck = function(instance, constractor) {
  if (!(instance instanceof constractor)) {
    alert("cannot call a class as instance of function!!!");
  }
};

var class_top_left = yearPickerAppName + "-top-left";
var class_top_right = yearPickerAppName + "-top-right";
var class_bottom_left = yearPickerAppName + "-bottom-left";
var class_bottom_right = yearPickerAppName + "-bottom-right";
var class_placements = [
  class_top_left,
  class_top_right,
  class_bottom_left,
  class_bottom_right
].join(" ");

var Yearpicker = (function() {
  function Yearpicker(element) {
    var options =
      arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : {};

    classCheck(this, Yearpicker);

    this.$element = $(element);
    this.element = element;
    this.options = $.extend({}, defaults, options);
    this.build = false;
    this.show = false;
    this.startYear = null;
    this.endYear = null;

    this.create();
  }

  // yearpicker
  Yearpicker.prototype = {
    create: function() {
      var $this = this.$element,
        options = this.options;
      var startYear = options.startYear,
        endYear = options.endYear,
        year = options.year;

      //this.trigger = $(options.trigger);
      this.isInput = $this.is("input") || $this.is("textarea");
      initialValue = this.getValue();
      this.initialValue = initialValue;
      this.oldValue = initialValue;
      year = year || initialValue || new Date().getFullYear();

      if (startYear) {
        if (year < startYear) {
          year = startYear;
        }
        this.startYear = startYear;
      }

      if (endYear) {
        if (year > endYear) {
          year = endYear;
        }
        this.endYear = endYear;
      }

      this.year = year;
      this.viewYear = year;
      this.initialYear = year;
      this.bind();
      this.init();
    },
    init: function() {
      if (this.build) {
        return;
      }
      this.build = true;

      var $this = this.$element,
        options = this.options;
      var $template = $(options.template);
      this.$template = $template;

      this.yearsPrev = $template.find(".yearpicker-prev");
      this.yearsCurrent = $template.find(".yearpicker-current");
      this.yearsNext = $template.find(".yearpicker-next");
      this.yearsBody = $template.find(".yearpicker-year");

      $template.addClass(options.hideClass);
      $(document.body).append(
        $template.addClass(yearPickerAppName + "-dropdown")
      );
      this.renderYear();
    },
    unbuild: function() {
      if (!this.build) {
        return;
      }
      this.build = false;
      this.$template.remove();
    },
    // assign a events
    bind: function() {
      var $this = this.$element,
        options = this.options;

      if ($.isFunction(options.show)) {
        $this.on(event_show, options.show);
      }
      if ($.isFunction(options.hide)) {
        $this.on(event_hide, options.hide);
      }
      if ($.isFunction(options.click)) {
        $this.on(event_click, options.click);
      }
      if (this.isInput) {
        $this.on(event_focus, $.proxy(this.showView, this));
      } else {
        $this.on(event_click, $.proxy(this.showView, this));
      }
    },
    getValue: function() {
      var $this = this.$element;
      var value = this.isInput ? $this.val() : $this.text();
      value = parseInt(value);
      return this.isInput ? parseInt($this.val()) : $this.text();
    },
    setValue: function() {
      var $this = this.$element;
      var value = this.year;
      if (this.isInput) {
        $this.val(value);
      } else {
        $this.html(value);
      }
    },
    trigger: function(type, data) {
      var e = $.Event(type, data);
      this.$element.trigger(e);
      return e;
    },
    place: function() {
      var $this = this.$element,
        options = this.options,
        $template = this.$template;

      var containerWidth = $(document).outerWidth(),
        containerHeight = $(document).outerHeight(),
        elementWidth = $this.outerWidth(),
        elementHeight = $this.outerHeight(),
        width = $template.width(),
        height = $template.height();

      var elementOffset = $this.offset(),
        top = elementOffset.top,
        left = elementOffset.left;

      var offset = parseFloat(options.offset);
      var placements = class_top_left;

      offset = isNaN(offset) ? 10 : offset;

      // positioning the y axis
      if (top > height && top + elementHeight + height > containerHeight) {
        top -= height + offset;
        placements = class_bottom_left;
      } else {
        top += elementHeight + offset;
      }

      // positioning the x axis
      if (left + width > containerWidth) {
        left += elementWidth - width;
        placements = placements.replace("left", "right");
      }

      $template
        .removeClass(class_placements)
        .addClass(placements)
        .css({
          top: top,
          left: left,
          zIndex: parseInt(this.zIndex, 10)
        });
    },
    createItem: function(data) {
      var options = this.options;
      var itemTag = options.itemTag;

      var items = {
        text: "",
        view: "",
        selected: false,
        disabled: false,
        highlighted: false
      };

      var classes = [];
      $.extend(items, data);
      if (items.selected) {
        classes.push(options.selectedClass);
      }

      if (items.disabled) {
        classes.push(options.disabledClass);
      }

      if (items.highlighted) {
        classes.push(options.highlightedClass);
      }

      return `<${itemTag} class="${items.view} ${classes.join(
        " "
      )}" data-view="${items.view}">${items.text}</${itemTag}>`;
    }
  };

  return Yearpicker;
})();

if ($.extend) {
  $.extend(Yearpicker.prototype, methods, render, handlers);
}

if ($.fn) {
  $.fn.yearpicker = function jQueryYearpicker(option) {
    for (
      var len = arguments.length, args = Array(len > 1 ? len - 1 : 0), key = 1;
      key < len;
      key++
    ) {
      args[key - 1] = arguments[key];
    }
    var result = void 0;

    this.each(function(i, element) {
      var $element = $(element);
      var isDestory = option === "destroy";
      var yearpicker = $element.data(yearPickerAppName);

      if (!yearpicker) {
        if (isDestory) {
          return;
        }
        var options = $.extend(
          {},
          $element.data(),
          $.isPlainObject(option) && option
        );
        yearpicker = new Yearpicker(element, options);
        $element.data(yearPickerAppName, yearpicker);
      }
      if (isString(option)) {
        var fn = yearpicker[option];

        if ($.isFunction(fn)) {
          result = fn.apply(yearpicker, args);

          if (isDestory) {
            $element.removeData(yearPickerAppName);
          }
        }
      }
    });

    return !isUndefained(result) ? result : this;
  };
  $.fn.yearpicker.constractor = Yearpicker;
}
