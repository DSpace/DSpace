#!/usr/bin/env python
# coding=utf-8
#
# LINDAT/CLARIN dev team
#
import ConfigParser
import StringIO
import codecs

import getopt
import glob
import os
import subprocess
import re
import sys

_settings = {

    "versions": {
        "ant": ("1.8", "ant -version", u"version ([\d\.]+) "),
        "maven": ("3.0", "mvn -version", u"Apache Maven ([\d\.]+)\s"),
    },

    "substitute": {
        "ignore": ("lr.replication",
                   ".analytics.",
                   ".backup.",
                   ".name",
                   ".id",
                   ".password",
                   "lr.harvestable.assetstore",
        ),
    },

    "params": {
    },
}

_OK = 0
_NOT_OK = 1


#=======================================
# help
#=======================================

def help_msg():
    """
        Returns help message.
    """
    print u"""Supported commands:
check for required ant version
 --ant-version=
check for required  mvn version
 --mvn-version=
get variable from file
 --get= --from=
substitute file with template variables
 --substitute=file1 --with=file2 [--to=] [--substitute-alternative=]"""


#=======================================
# helper methods
#=======================================

def parse_command_line():
    """ Parses the command line arguments. """
    opts = None
    try:
        options = (
            "ant-version=",
            "mvn-version=",
            "substitute=",
            "with=",
            "to=",
            "get=",
            "from=",
            "substitute-alternative=",
        )
        opts, _ = getopt.getopt( sys.argv[1:], "", options )
    except getopt.GetoptError, e:
        help_msg()
        fail( 1, "Invalid command found [%s]", str(e) )

    what_to_do = None
    for option, param in opts:
        if option == "--ant-version":
            return lambda: check_version("ant", param)
        if option == "--mvn-version":
            return lambda: check_version("maven", param)
        if option == "--substitute":
            what_to_do = substitute
            _settings["params"]["substitute"] = param
        if option == "--get":
            what_to_do = get_variable
            _settings["params"]["get"] = param
        if option in (
                "--substitute-alternative",
                "--with",
                "--to",
                "--from",
        ):
            _settings["params"][option.lstrip("--")] = param

    if what_to_do is not None:
        return what_to_do

    help_msg()
    fail( 1, "Do not know what to do?!" )


def fail(ret_code, fail_msg, *args):
    print fail_msg % args
    print "Exiting with a FAIL return code [%d]" % ret_code
    sys.exit(ret_code)


def required_param(opts, *args):
    """ Check if the required parameters exist in the dict. """
    for a in args:
        if not a in opts:
            help_msg()
            fail( _NOT_OK, "Expected argument [%s] not found!", a )


def required_exists(*args):
    """ Check if the file (or glob regexp) exists on fs. """
    for f in args:
        if not os.path.exists(f):
            if len(glob.glob(f)) > 0:
                continue
            return False, f
    return True, None


def read_config(config_file):
    """ Read config values mimicking the config template required by python. """
    values_str = '[root]\n' + codecs.open(config_file, mode="rb", encoding="utf-8").read()
    values_fp = StringIO.StringIO(values_str)
    config = ConfigParser.RawConfigParser()
    config.optionxform = str
    config.readfp(values_fp)
    return dict(config.items("root"))

#=======================================
# real methods
#=======================================


# noinspection PyBroadException
def check_version(key, expected_str=None):
    """
        Check version of ant
    """
    min_expected, cmd, regexp = _settings["versions"][key]
    if expected_str is not None and len(expected_str) > 0:
        min_expected = expected_str
    stdout = subprocess.Popen(
        cmd, shell=True, stdout=subprocess.PIPE).stdout.read()
    try:
        version = re.compile(regexp).search(stdout).group(1)
    except:
        print "CANNOT validate %s version, continuing..." % key
        return _OK

    for i in range(min(len(min_expected), len(version))):
        if min_expected[i] > version[i]:
            fail(_NOT_OK, "%s version [%s] is *NOT* enough!", key, version)
    print "%s version %s meets requirements" % (key, version)
    return _OK


def get_variable():
    required_param( _settings["params"], "from", "get" )
    from_f = _settings["params"]["from"]
    var_name = _settings["params"]["get"]
    exists, f = required_exists( from_f )
    if not exists:
        fail( _NOT_OK, "Expected file [%s] does *NOT* exist!", f )
    values = read_config(from_f)
    if var_name not in values:
        fail( _NOT_OK, "Could not find [%s] variable", var_name )
    print values[var_name]
    return _OK


def substitute():
    """
        Substitute file with variables
    """
    required_param( _settings["params"], "substitute", "with" )

    subst_f = _settings["params"]["substitute"]
    with_f = _settings["params"]["with"]
    exists, f = required_exists( subst_f, with_f )
    if not exists:
        required_param( _settings["params"], "substitute-alternative" )
        subst_f = _settings["params"]["substitute-alternative"]
        exists, f = required_exists( subst_f, with_f )
        if not exists:
            fail( _NOT_OK, "Expected file [%s] does *NOT* exist!", f )

    # read what should be used
    values_d = read_config(with_f)

    # for k in sorted(values_d.keys()):
    #     sys.stderr.write( "%s = %s\n" % (k, values_d[k]) )
    # sys.stderr.write( 40 * "=" + "\n" )

    # check for values
    ignored = 0
    for k, v in values_d.iteritems():
        if len(v.strip()) == 0:
            ignore = False
            for ign in _settings["substitute"]["ignore"]:
                if ign in k:
                    ignore = True
                    ignored += 1
                    break
            if not ignore:
                sys.stderr.write( "WARNING: *empty* property [%s]!\n" % k )
    #sys.stderr.write( 40 * "=" + "\n" )

    # substitute - recursive?
    def subst_text(text, info_bool=False):
        patterns = set(re.findall( u"\$\{.*?\}", text))
        not_found = 0
        for p in patterns:
            p_stripped = p.strip("{}$")
            if p_stripped in values_d:
                text = re.sub( u"\$\{%s\}" % p_stripped, values_d[p_stripped], text )
            else:
                if re.search( u"\s*%s\s*=\s*\$\{%s?\}" % (p_stripped, p_stripped), text):
                    fail( _NOT_OK, "ERROR: cannot find variable definition for [%s] "
                                   "- DEFINE it in local.conf" % p)
                else:
                    #sys.stderr.write("Warning: did not substitute [%s]\n" % p)
                    not_found += 1
        if info_bool:
            sys.stderr.write( "replaced [%d] variables out of [%d]\n" % (
                len(patterns) - not_found, len(patterns)) )
        return text

    #
    for k, v in values_d.items():
        values_d[k] = subst_text(v)

    sys.stderr.write( "Ignored [%d] empty variables; " % ignored )
    input_files = glob.glob(subst_f)
    for subst_f in input_files:
        # read contents
        with codecs.open(subst_f, mode="rb", encoding="utf-8") as fin:
            contents = fin.read()
        # substitute and output to stdout or directory
        contents = subst_text(contents, True)
        sys.stderr.write( 40 * "=" + "\n" )
        sys.stderr.flush()
        if "to" in _settings["params"]:
            to_file = _settings["params"]["to"]
            if os.path.isdir(to_file):
                to_file = os.path.join(to_file, os.path.basename(subst_f))
            with codecs.open(to_file, mode="wb+", encoding="utf-8") as fout:
                fout.write(contents)
        else:
            sys.stdout.write(contents)

    return _OK

#
#
#

if __name__ == '__main__':
    what_to_do_callable = parse_command_line()
    ret = what_to_do_callable()
    sys.exit( ret )
