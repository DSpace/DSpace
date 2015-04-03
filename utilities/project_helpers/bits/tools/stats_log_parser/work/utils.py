# coding=utf-8
# This work is licensed!
# pylint: disable=W0702,R0201,C0111,W0613,R0914

"""
  Utils.
"""
import codecs
import logging
import logging.config
import os
import sys
import locale
import subprocess
import tempfile
from time import sleep


# noinspection PyRedeclaration,PyBroadException,PyDocstring
def initialize_logging(config_file):
    """
        Originally done using dictConfig but available only for python2.7+
    """
    try:
        os.makedirs("logs")
    except:
        pass

    # can be done using dictConfig but available only for python2.7+
    logging.config.fileConfig(config_file)


def host_info():
    """ Return simple info about OS/arch we run on. """
    import getpass
    import platform

    user = getpass.getuser( )
    os_, mach = platform.system( ), platform.machine( )
    pid = os.getpid( )
    python = u".".join( [unicode( x ) for x in sys.version_info] )
    return u"[%s @ %s/%s] [pid:%s] ran by [python %s]" % (
        user, os_, mach, pid, python)


# noinspection PyDocstring
def get_logger_messages(logger_name=None):
    """
        Read file and return the contents of the last N lines.
    """
    try:
        for h in logging.getLogger( logger_name ).handlers:
            if isinstance( h, logging.FileHandler ):
                h.flush( )
                with codecs.open( h.baseFilename, mode="r", encoding="utf-8" ) as fin:
                    lines = fin.readlines( )
                    return u"".join( lines[-1000:] )
        return u""
    except Exception, e:
        return unicode(repr( e ))


def run(cmd, logger=None, del_stdout_stderr=True):
    """
        Run local process using command line in cmd.
    """
    temp_dir = None
    tempik_stdout = tempfile.NamedTemporaryFile(
        mode="w+b",
        suffix=".cmd.stdout.txt",
        dir=temp_dir,
        delete=del_stdout_stderr
    )
    tempik_stderr = tempfile.NamedTemporaryFile(
        mode="w+b",
        suffix=".cmd.stderr.txt",
        dir=temp_dir,
        delete=del_stdout_stderr
    )
    if logger:
        logger.debug( u"Running [%s] into\n  [%s]\n  [%s]",
                      uni( cmd ),
                      tempik_stdout.name,
                      tempik_stderr.name )
    p = subprocess.Popen( cmd.encode( locale.getpreferredencoding( ) ),
                          shell=True,
                          stdin=None,
                          stdout=tempik_stdout,
                          stderr=tempik_stderr )

    # wait for the end
    p.communicate( )
    ret = [None, None]
    try:
        with tempik_stdout as ftemp:
            ftemp.seek(0)
            ret[0] = uni(ftemp.read().strip())
        with tempik_stderr as ftemp:
            ftemp.seek(0)
            ret[1] = uni(ftemp.read().strip()) or None
    except Exception:
        raise
    finally:
        tempik_stdout.close()
        tempik_stderr.close()

    return p.returncode, ret[0], ret[1]


# noinspection PyBroadException
def uni(str_str, encoding="utf-8"):
    """ Try to get unicode without errors """
    try:
        if isinstance( str_str, unicode ):
            return str_str
        elif isinstance( str_str, basestring ):
            return unicode( str_str, encoding )
    except UnicodeError:
        pass
    try:
        return unicode( str( str_str ),
                        encoding=encoding,
                        errors='ignore' )
    except UnicodeError:
        pass
    try:
        return str_str.decode( encoding=encoding,
                               errors="ignore" )
    except Exception:
        pass
    return u""


#noinspection PyBroadException
def safe_unlink(file_path):
    """
        Safe delete of a file looping up to 1 sec.

        .. note::

          On some systems (i.e., Windows) the process can still `somehow` have the file opened.
    """
    assert isinstance( file_path, basestring )
    max_loops = 10
    while 0 < max_loops:
        try:
            os.unlink( file_path )
            return
        except Exception:
            sleep( 0.1 )
            if max_loops == 1:
                pass
        finally:
            max_loops -= 1
