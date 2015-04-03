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
import glob
import os
# problem with python2.6 on our servers ... import multiprocessing

from settings import settings as settings_inst
import utils
# initialise logging
utils.initialize_logging( settings_inst["logger_config"] )
_logger = logging.getLogger( 'common' )
import subprocess


# noinspection PyBroadException
try:
    import magic
except:
    print "Have you installed python-magic?"
    sys.exit( 1 )


# =======================================
# check_files
#=======================================

def find_file_type(file_str):
    """
        Not working on windows.
    """
    try:
        #p = subprocess.Popen(
        #    'file --mime-type %s' % file_str, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        #output, errors = p.communicate()
        #return file_str, output.split(" ")[-1].strip(), errors
        mime = magic.from_file( file_str, mime=True )
        return file_str, mime, ""
    except Exception, e:
        return file_str, "unknown", repr( e )


def verify(cmd, f):
    """

    """
    try:
        cmd = cmd % f
        #_logger.info( "Popen verify started" )
        p = subprocess.Popen(
            #    cmd, shell=True, stdout=None, stderr=None)
            cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE )
        output, errors = p.communicate( )
        #p.communicate()
        #_logger.info( "Popen verify ended [%d]", p.returncode )
        #_logger.info( "Popen verify ended" )
        return p.returncode, output
        #return p.returncode, ""
        #return 0, ""
    except Exception, e:
        return -1, repr( e )


#noinspection PyBroadException
def db_assetstore(env):
    """
       Get assetstore id and filename from db scanning local.conf or dspace.cfg
       :(.
    """

    # try local.config
    for dspace_cfg in env["config_dist_relative"]:
        dspace_cfg = os.path.join( os.getcwd( ), dspace_cfg )
        if os.path.exists( dspace_cfg ):
            env["config_dist_relative"][0] = dspace_cfg
            break
    dspace_cfg = os.path.join( os.getcwd( ), env["config_dist_relative"][0] )
    prefix = "lr."
    if not os.path.exists( dspace_cfg ):
        _logger.info( "Could not find [%s]", dspace_cfg )
        dspace_cfg, prefix = os.path.join( os.getcwd( ), env["dspace_cfg_relative"] ), ""
        # try dspace.cfg
        if not os.path.exists( dspace_cfg ):
            _logger.info( "Could not find [%s]", dspace_cfg )
            dspace_cfg = None

    # not found
    if dspace_cfg is None:
        return None

    # get the variables
    #
    _logger.info( "Parsing for [%s]", dspace_cfg )
    db_username = None
    db_pass = None
    db_table = None
    if os.path.exists( dspace_cfg ):
        lls = open( dspace_cfg, "r" ).readlines( )
        for l in lls:
            l = l.strip( )
            if l.startswith( prefix + "db.username" ):
                db_username = l.strip( ).split( "=" )[1].strip( )
            if l.startswith( prefix + "db.password" ):
                db_pass = l.strip( ).split( "=" )[1].strip( )
            if db_table is None and l.startswith( prefix + "db.url" ):
                db_table = l.strip( ).split( "/" )[-1].strip( )
            if l.startswith( prefix + "database " ) or l.startswith( prefix + "database=" ):
                db_table = l.strip( ).split( "=" )[1].split( "/" )[-1].strip( )

    _logger.info( "Trying to connect to [%s] under [%s]",
                  db_table, db_username )

    # get the db table
    import bpgsql

    try:
        con = bpgsql.connect(
            username=db_username, password=db_pass, host="127.0.0.1", dbname=db_table )
        cursor = con.cursor( )
        cursor.execute( "select name, internal_id from bitstream" )
        objs = cursor.fetchall( )
        # better explicitly
        cursor.close( )
        con.close( )
        return dict( [(y, x) for x, y in objs] )
    except Exception, e:
        _logger.exception( "No connection could be made" )


_OK = 0


def check_files(env):
    """
        Check all files.
    """
    badbad = []
    unchecked = []
    assetstore_pairs = db_assetstore( env )
    if assetstore_pairs is None:
        _logger.critical( "Problems fetching assetstore file extensions" ) 
        return -1
    files_to_check = [os.path.abspath( x ) for x in glob.glob( env["input_dir"] )]
    _logger.info( "Checking [%d] files", len( files_to_check ) )
    for pos, f in enumerate( files_to_check ):
        base_f = os.path.basename( f )
        _1, mime_type, _2 = find_file_type( f )
        file_name = assetstore_pairs.get( base_f, "" )
        if file_name is None or 0 == len( file_name ):
            _logger.warn( "Could not find file [%s] in assetstore_pairs", base_f )
            continue
        _logger.info( "#%d. %s | %s\n\t%s", pos, base_f, file_name, mime_type )

        if mime_type in env["mime_type"]:
            verificator = env["mime_type"][mime_type]
            if callable( verificator ):
                ret, msg = verificator( f )
            else:
                ret, msg = verify( verificator, f )
                if ret != 0:
                    print ret, mime_type, f,  msg
            _logger.info( "checked: [%s] [%d]...", msg[:min( len( msg ), 200 )], ret )
            #_logger.info( "checked: return code [%d]...", ret )
            if ret != _OK:
                badbad.append( (f, msg) )
        else:
            _logger.info( "not checked..." )
            unchecked.append( (mime_type, f) )

    msg_unchecked = "Unchecked files: [%d]\n\t" % len(unchecked)
    un_d = {}
    for mime, f in unchecked:
        if mime not in un_d:
            un_d[mime] = 0
        un_d[mime] += 1
    msg_unchecked += "\n\t".join( [ "%s: %s" % (mime, cnt) for mime, cnt in un_d.iteritems() ] )
    msg_problematic = "Problematic files: [%d]\n\t" % len( badbad )
    msg_problematic += "\n\t".join( [ "%s: %s" % (f, msg) for f, msg in badbad ] )

    _logger.info( msg_unchecked )
    _logger.info( msg_problematic )

    print 40 * "="
    print msg_unchecked
    print msg_problematic
    print 40 * "="

    return len( badbad )


#=======================================
# help
#=======================================

def help_msg(env):
    """ Returns help message. """
    _logger.warning( u"""\n%s help. Supported commands:\n""", env["name"] )


def version_msg(env):
    """ Return the current application version. """
    return _logger.warning( u"Version: %s", env["name"] )


#=======================================
# command line
#=======================================

def parse_command_line(env):
    """ Parses the command line arguments. """
    try:
        options = ["help",
                   "check",
                   "version",
                   "dir=",
        ]
        input_options = sys.argv[1:]
        opts, _ = getopt.getopt( input_options, "", options )
    except getopt.GetoptError:
        help_msg( env )
        sys.exit( 1 )

    what_to_do = None
    for option, param in opts:
        if option == "--help":
            env["print_info"] = False
            return help_msg
        if option == "--version":
            return version_msg
        if option == "--check":
            what_to_do = check_files
        if option == "--dir":
            env["input_dir"] = os.path.join( param, env["input_dir_glob"] )

    if what_to_do:
        return what_to_do
    if "input_dir" not in env:
        return help_msg
    # what to do but really?
    return check_files


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
        ret_code = what_to_do_callable( settings_inst )
    except Exception, e_inst:
        _logger.critical( "An exception occurred, ouch:\n%s", e_inst )
        raise
    finally:
        lasted = time.time( ) - lasted
        _logger.info( "Stopping after [%f] secs.", lasted )
    sys.exit( ret_code )
