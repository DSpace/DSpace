/*global jQuery */
/*jshint globalstrict: true*/
'use strict';

jQuery(document).ready(
    function () {
        var opts = (function () {
            var instance = {};
            //if ever port is needed (eg. testing other tomcat) it should be in responseUrl and target
            instance.port = (window.location.port === "" ? "" : ":" + window.location.port);
            instance.host = window.location.protocol + '//' +
                window.location.hostname;
            instance.repoPath = jQuery("a#repository_path").attr("href");
            if (instance.repoPath.charAt(instance.repoPath.length - 1) !== '/') {
                instance.repoPath = instance.repoPath + '/';
            }
            instance.target = instance.host + instance.port + instance.repoPath;
            //In order to use the discojuice store (improve score of used IDPs)
            //Works only with "verified" SPs - ie. ufal-point, displays error on ufal-point-dev
            instance.responseUrl =
                (window.location.hostname.search("ufal-point-dev") >= 0) ?
                        "" :
                        instance.host + instance.port + instance.repoPath +
                            "themes/UFAL/lib/html/disco-juice.html?";
            instance.metadataFeed = instance.target + "discojuice/feeds";
            instance.serviceName = "LINDAT/CLARIN Repository";
            instance.localauth =
                '<form method="post" action="' + instance.target + 'password-login"> ' +
                    '<p>Sign in using your local account obtained from the LINDAT/CLARIN administrator.</p>' +
                    '<p style="margin: 5px; color: #888" ><input type="text" name="login_email" style="font-size: 160%; width: 100%" id="login" /> <label for="login">E-Mail Address</label></p>' +
                    '<p style="margin: 5px; color: #888" ><input type="password" name="login_password" style="font-size: 160%; width: 100%" id="pass" /> <label for="pass">Password</label></p>' +
                    '<p style="margin: 5px; color: #607890; text-decoration: underline;"><a href="' + instance.target + 'forgot">Forgot your password?</a></p>' +
                    '<p  style="" ><input type="submit" style="margin: 20px 2px" name="submit" value="Sign in" /></p>' +
                    '</form>';
            instance.target = instance.target + "shibboleth-login";
            instance.textHelpMore = "First check You are searching under the right country.\nIt is possible that our service is not connected with Your identity provider. Please contact us at <a href='mailto:lindat-help@ufal.mff.cuni.cz'>lindat-help@ufal.mff.cuni.cz</a>. We'll try to solve it with Your provider or create an account for You.";
            return instance;
        })();
        if (!("aai" in window)) {
            throw "Failed to find UFAL AAI object. See https://redmine.ms.mff.cuni.cz/projects/lindat-aai for more details!";
        }
        window.aai.setup(opts);
    }
); // ready
