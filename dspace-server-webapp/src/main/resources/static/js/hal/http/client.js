 /***********************************
  * Customized version of the HAL Browser client.js provided from https://github.com/mikekelly/hal-browser
  * Copyright (c) 2012 Mike Kelly, http://stateless.co/
  * MIT LICENSE: https://github.com/mikekelly/hal-browser/blob/master/MIT-LICENSE.txt)
  *
  * This DSpace version of client.js has be customized to include:
  *     * Download file functionality (see new downloadFile() method)
  *     * Improved AuthorizationHeader parsing (see new getAuthorizationHeader() method)
  ***********************************/
HAL.Http.Client = function(opts) {
    this.vent = opts.vent;
    this.defaultHeaders = {'Accept': 'application/hal+json, application/json, */*; q=0.01'};
    var authorizationHeader = getAuthorizationHeader();
    authorizationHeader ? this.defaultHeaders.Authorization = authorizationHeader : '';
    // Write all headers to console (for easy debugging)
    //console.log(this.defaultHeaders);
    this.headers = this.defaultHeaders;
};

/**
 * Get CSRF Token by parsing it out of the "MyHalBrowserCsrfToken" cookie.
 * This cookie is set in login.html after a successful login occurs.
 **/
function getCSRFToken() {
    var cookie = document.cookie.match('(^|;)\\s*' + 'MyHalBrowserCsrfToken' + '\\s*=\\s*([^;]+)');
    if (cookie != null) {
        return cookie.pop();
    } else {
        return null;
    }
}

/**
 * Check current response headers to see if the CSRF Token has changed. If a new value is found in headers,
 * save the new value into our "MyHalBrowserCsrfToken" cookie.
 **/
function checkForUpdatedCSRFTokenInResponse(jqxhr) {
    // look for DSpace-XSRF-TOKEN header & save to our MyHalBrowserCsrfToken cookie (if found)
    var updatedCsrfToken = jqxhr.getResponseHeader('DSPACE-XSRF-TOKEN');
    if (updatedCsrfToken != null) {
        document.cookie = "MyHalBrowserCsrfToken=" + updatedCsrfToken;
    }
}

/**
 * Get Authorization Header by parsing it out of the "MyHalBrowserToken" cookie.
 * This cookie is set in login.html after a successful login occurs.
 **/
function getAuthorizationHeader() {
    var cookie = document.cookie.match('(^|;)\\s*' + 'MyHalBrowserToken' + '\\s*=\\s*([^;]+)');
    if (cookie != null) {
        return 'Bearer ' + cookie.pop();
    } else {
        return null;
    }
}

function downloadFile(url) {
    var request = new XMLHttpRequest();
    request.open('GET', url, true);
    request.responseType = 'blob';
    var authorizationHeader = getAuthorizationHeader();
    if (authorizationHeader != undefined) {
        request.setRequestHeader("Authorization", authorizationHeader);
    }
    request.onload = function () {
        // Only handle status code 200
        if (request.status === 200) {
            // Try to find out the filename from the content disposition `filename` value
            var disposition = request.getResponseHeader('content-disposition');
            var matches = /"([^"]*)"/.exec(disposition);
            var filename = (matches != null && matches[1] ? matches[1] : 'content');
            // The actual download
            var contentTypeHeader = request.getResponseHeader("content-type");
            if (contentTypeHeader === undefined || contentTypeHeader === "") {
                contentTypeHeader = "application/octet-stream";
            }
            var blob = new Blob([request.response], {type: contentTypeHeader});
            var link = document.createElement('a');
            link.href = window.URL.createObjectURL(blob);
            link.download = filename;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        }
        // some error handling should be done here...
    };
    request.send();
}


HAL.Http.Client.prototype.get = function(url) {
    var self = this;
    this.vent.trigger('location-change', { url: url });
    $.ajax({
        url: url,
        dataType: 'json',
        xhrFields: {
            withCredentials: true
        },
        headers: this.headers,
        success: function(resource, textStatus, jqXHR) {
            // NOTE: A GET never requires sending an CSRF Token, but the response may send an updated token back.
            // So, we need to check if a token came back in this GET response.
            checkForUpdatedCSRFTokenInResponse(jqXHR);
            self.vent.trigger('response', {
                resource: resource,
                jqxhr: jqXHR,
                headers: jqXHR.getAllResponseHeaders()
            });
        },
        error: function(jqXHR, textStatus, errorThrown) {
            // Also check for updated token during errors. E.g. when a login failure occurs, token may be changed.
            checkForUpdatedCSRFTokenInResponse(jqXHR);
            self.vent.trigger('fail-response', { jqxhr: jqXHR });
            var contentTypeResponseHeader = jqXHR.getResponseHeader("content-type");
            if (contentTypeResponseHeader != undefined
                    && !contentTypeResponseHeader.startsWith("application/hal")
                    && !contentTypeResponseHeader.startsWith("application/json")) {
                downloadFile(url);
            }
        }
    });
};

HAL.Http.Client.prototype.request = function(opts) {
    var self = this;
    opts.dataType = 'json';
    opts.xhrFields = opts.xhrFields || {};
    opts.xhrFields.withCredentials = opts.xhrFields.withCredentials || true;
    opts.headers = opts.headers || {};
    // If CSRFToken exists, append as a new X-XSRF-Token header
    var csrfToken = getCSRFToken();
    if (csrfToken != null) {
      opts.headers['X-XSRF-Token'] = csrfToken;
    }

    // Also check response to see if CSRF Token has been updated
    opts.success = function(resource, textStatus, jqXHR) {
        checkForUpdatedCSRFTokenInResponse(jqXHR);
    };

    // Also check error responses to see if CSRF Token has been updated
    opts.error = function(jqXHR, textStatus, errorThrown) {
         checkForUpdatedCSRFTokenInResponse(jqXHR);
    };

    self.vent.trigger('location-change', { url: opts.url });
    return jqxhr = $.ajax(opts);
};

HAL.Http.Client.prototype.updateHeaders = function(headers) {
    this.headers = headers;
};

HAL.Http.Client.prototype.getHeaders = function() {
    return this.headers;
};
