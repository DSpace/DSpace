# coding=utf-8
# This work is licensed!
# pylint: disable=W0702,R0201,C0111,W0613,R0914

"""
  Main entry point.
"""

import sys
import logging
import getopt
import time
import os
from settings import settings as settings_inst
import utils
# initialise logging
logging.basicConfig( level=logging.DEBUG, format='%(asctime)-15s %(message)s' )
_logger = logging.getLogger( )


# =======================================
# dspace specific config
# =======================================

class dspace_configuration(object):
    """
        Parse dspace.cfg/local.conf if found
    """
    def __init__(self, env):
        self.conf = None
        for dspace_cfg in env["config_dist_relative"]:
            dspace_cfg = os.path.join( os.getcwd( ), dspace_cfg )
            if os.path.exists( dspace_cfg ):
                env["config_dist_relative"][0] = dspace_cfg
                break
        dspace_cfg = os.path.join( os.getcwd( ), env["config_dist_relative"][0] )
        if not os.path.exists( dspace_cfg ):
            _logger.info( "Could not find [%s]", dspace_cfg )
            dspace_cfg, prefix = os.path.join( os.getcwd( ), env["dspace_cfg_relative"] ), ""
            # try dspace.cfg
            if not os.path.exists( dspace_cfg ):
                _logger.info( "Could not find [%s]", dspace_cfg )
                dspace_cfg = None

        # not found
        if dspace_cfg is None:
            return

        # get the variables
        #
        _logger.info( "Parsing [%s]", dspace_cfg )

        self.conf = {}
        if os.path.exists( dspace_cfg ):
            lls = [x.strip( ) for x in open( dspace_cfg, "r" ).readlines( )]
            for l in lls:
                if l.startswith( "#" ) or len( l.split("=") ) != 2:\
                    continue
                self.conf[l.split("=")[0].strip()] = l.split("=")[1].strip()

    def __getitem__(self, key):
        return self.conf.get( key, None )


# =======================================
# dspace db
# =======================================

class dspace_database( object ):
    """
        Dspace db wrapper.
    """
    def __init__(self, dspace_conf):
        self.con = None
        db = dspace_conf["lr.database"]
        u = dspace_conf["lr.db.username"]
        p = dspace_conf["lr.db.password"]

        if db is None:
            _logger.info( "DSpace config could not be parsed correctly" )
            return
        _logger.info( "Trying to connect to [%s] under [%s]", db, u )
        import bpgsql

        self.con = bpgsql.connect(
            username=u, password=p, host="127.0.0.1", dbname=db )
        self.cursor = None

    def ok(self):
        return self.con is not None

    def __enter__(self):
        self.cursor = self.con.cursor( )
        return self.cursor

    def __exit__(self, type, value, traceback):
        if self.cursor is not None:
            self.cursor.close( )
        self.con.close( )


# =======================================
# the stuff
# =======================================

def call_ftor(env, callable, opts):
    """
        Metadata info.
    """
    conf = dspace_configuration( env )
    db = dspace_database( conf )
    if db.ok( ):
        with db as cursor:
            callable( cursor, conf, opts )


# =======================================
# help
# =======================================

def help_msg(env):
    """ Returns help message. """
    _logger.warning( u"""
%s help. Supported commands:
--metadata
--assetstore-path --handle= --name=
    """, env["name"] )


def version_msg(env):
    """ Return the current application version. """
    return _logger.warning( u"Version: %s", env["name"] )


# =======================================
# command line
# =======================================

def parse_command_line(env):
    """ Parses the command line arguments. """
    try:
        options = [
            "metadata",
            "assetstore-path",
            "handle=",
            "name=",
            ]
        input_options = sys.argv[1:]
        opts, _ = getopt.getopt( input_options, "", options )
    except getopt.GetoptError:
        help_msg( env )
        sys.exit( 1 )

    what_to_do = None
    opts = dict( (x[2:], y) for x, y in opts)
    for option, param in opts.iteritems():
        if option == "help":
            return help_msg
        if option == "version":
            return version_msg
        if option == "metadata":
            from ftor import do
            what_to_do = lambda x: call_ftor(x, do, opts)
        if option == "assetstore-path":
            from ftor_assetstore_names import do
            what_to_do = lambda x: call_ftor(x, do, opts)

    # really, what to do?
    return what_to_do


#=======================================
# main
#=======================================

if __name__ == "__main__":
    lasted = time.time( )

    _logger.info( u"Starting at " + utils.host_info( ) )

    # do what was specified or default
    ret_code = 0
    try:
        what_to_do_callable = parse_command_line( settings_inst )
        if what_to_do_callable is not None:
            ret_code = what_to_do_callable( settings_inst )
    except Exception, e_inst:
        _logger.critical( "An exception occurred, ouch:\n%s", e_inst )
        raise
    finally:
        lasted = time.time( ) - lasted
        _logger.info( "Stopping after [%f] secs.", lasted )
    sys.exit( ret_code )
