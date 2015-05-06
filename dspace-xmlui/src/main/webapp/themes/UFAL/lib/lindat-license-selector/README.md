
<!-- TITLE/ -->

# License Selector

<!-- /TITLE -->


<!-- DESCRIPTION/ -->

JQuery plugin for easy selection of various licenses

<!-- /DESCRIPTION -->

## Give It a Try

Use the selector [directly on Github](https://ufal.github.io/lindat-license-selector). You can link to this to always use our latest version.


## Install

### Using Bower

```
bower install lindat-license-selector --save
```

### Manual

Download the latest version of the plugin from the repository
([Javascript](https://raw.githubusercontent.com/ufal/lindat-license-selector/releases/license-selector.js)
and [CSS](https://raw.githubusercontent.com/ufal/lindat-license-selector/releases/license-selector.css))

The plugin requires [Lo-Dash](http://lodash.com/) or [Underscore](http://underscorejs.org/) utility library.


## Usage
```.html
<link rel="stylesheet" href="license-selector.css">
<script type="text/javascript" src="license-selector.js"></script>
<script type="text/javascript">
  $(function() {
    'use strict';
    $('selector').licenseSelector({ ...options... });
  });
</script>
```

### Options

#### onLicenseSelected

Callback to action that should happen after the license is selected. Receives selected license as a first argument.

```.javascript
onLicenseSelected : function (license) {
    $('body').append($('<pre></pre>').text(JSON.stringify(license, null, 4)))
    console.log(license)
}
```

#### licenseItemTemplate (function|jQuery)

A template function to customize license display in the license list. See the example below. The function takes three arguments:

1. jQuery object of an `<li>` element
2. `license` object with attributes defined bellow
3. select function - the function that actually does the license selection. Can be used as `onClick` handler

#### appendTo

JQuery selector specifying a html element where license selector should be attached. Default is `'body'`.

#### start

Name of the starting question. See to sources for the full list of names. Here are the most useful:

- **'KindOfContent'** (default) is asking about the kind of content (Software or Data)
- **'DataCopyrightable'** jumps straight to data licensing. Use this as a `start` if you want to choose only licenses for data.
- **'YourSoftware'** jumps to software licensing. The same as above but for software.

#### licenses

A list of licenses that will get merged to the predefined license. The merge is done by [`_.merge`](https://lodash.com/docs#merge) so you can use it to add new licenses or to change configuration of the predefined licenses.

```.javascript
.licenseSelector({
    licenses: {
      'abc-license': {
        name: 'NEW license',
        priority: 1,
        available: true,
        url: 'http://www.example.com/new-license',
        description: 'This is new license inserted as a test',
        categories: ['data', 'new'],
        template: function($el, license, selectFunction) {
          var h = $('<h4 />').text(license.name);
          h.append($('<a/>').attr({
            href: license.url,
            target: '_blank'
          }));
          $el.append(h);
          $el.append('<p>Custom template function</p>');
          $el.append(
            $('<button/>')
              .append('<span>Click here to select license</span>')
              .click(selectFunction)
          );
        }
      },
      'cc-by': {
        description: 'Modified description ...',
        cssClass: 'featured-license'
      },
      'lgpl-3': {
        available: false // hide the LGPL 3 license
      }
    }
);
```

##### License Attributes

- `string` **key** - The hash key (will be automatically added)
- `string` **name** - Full name of the license
- `bool` **available** - Flag whether the license is visible in the license list
- `unsigned int` **priority** - Sort priority (lower means higher in the license list)
- `string` **url** - Url pointing to the license full text
- `string` **description** - A short description of the license
- `string` **cssClass** - Custom CSS class set on `<li>` element
- `function|jQuery` **template** - Template used for custom format
- `array[string]` **categories** - A list of arbitrary category names used for filtering in the questions

## Development

Node environment is not required but strongly recommended for the development

1. Install Node
    
        curl https://raw.githubusercontent.com/creationix/nvm/v0.17.2/install.sh | bash
        nvm install stable
        nvm use stable

2. Install Grunt & Bower
        
        npm install -g grunt-cli
        npm install -g bower

3. Clone repository
    
        git clone https://github.com/ufal/lindat-license-selector.git
        cd lindat-license-selector
        npm install
        bower install

4. Start development server
    
        grunt start
    
## Authors

- Pawel Kamocki
- Pavel Straňák <stranak@ufal.mff.cuni.cz>
- Michal Sedlák <sedlak@ufal.mff.cuni.cz>

## Attribution

Descriptions for some licenses taken from (or inspired by) descriptions at [tldrLegal](https://tldrlegal.com).

## Warning / Disclaimer

You must not rely on the information from License Selector as an alternative to legal advice from your attorney or other professional legal services provider.   

<!-- LICENSE/ -->

## License

Licensed under the incredibly [permissive](http://en.wikipedia.org/wiki/Permissive_free_software_licence) [MIT license](http://creativecommons.org/licenses/MIT/)

Copyright &copy; 2014 Institute of Formal and Applied Linguistics (http://ufal.mff.cuni.cz)

<!-- /LICENSE -->
