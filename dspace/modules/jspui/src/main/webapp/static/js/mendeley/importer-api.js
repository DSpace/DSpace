/**
 * Mendeley Importer API
 *
 * Exposes methods for interacting with the importer bookmarket:
 *
 * open()
 * close()
 * registerDataCallback()
 * registerUserIdentityCallback()
 * registerHostId()
 */
MendeleyImporterApi = {};

(function(api) {

	var data = {},
		bookmarkletUrl = 'https://www.mendeley.com/minified/bookmarklet.js';

	// Main
	init();

	/**
	 * Initialise the API
	 */
	function init() {

		api.open = open;
		api.close = close;
		api.setOpenedWithApi = getSetter('openedWithApi');
		api.getOpenedWithApi = getGetter('openedWithApi');

		// Create methods for getting/setting data and callbacks
		var methods = ['HostId', 'UserIdentityCallback', 'DataCallback'];
		for(var i=0, l=methods.length; i < l; i++) {
			var key = methods[i];
			api['register' + key] = getSetter(key);
			api['get' + key] = getGetter(key);
		}
	}

	/**
	 * Generate getter
	 *
	 * @param string key for data
	 */
	function getGetter(key) {
		return function() {
			return data[key];
		};
	}

	/**
	 * Generate setter
	 *
	 * @param string key for data
	 */
	function getSetter(key) {
		return function(value) {
			data[key] = value;
		};
	}

	/**
	 * Open the bookmarklet
	 */
	function open() {
		this.setOpenedWithApi(true);
		document.getElementsByTagName('body')[0]
			.appendChild(document.createElement('script'))
			.setAttribute('src', bookmarkletUrl);
	}

	/**
	 * Close the bookmarklet
	 */
	function close() {
		try {
			document.getElementById('mendeley-bookmarklet-close').click();
		} catch (e) {}
	}

})(MendeleyImporterApi);

