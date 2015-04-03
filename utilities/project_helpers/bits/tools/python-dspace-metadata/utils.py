# coding=utf-8
# This work is licensed!
# pylint: disable=W0702,R0201,C0111,W0613,R0914

"""
  Utils.
"""
import logging
import logging.config
import os
import sys


# noinspection PyRedeclaration,PyBroadException,PyDocstring
def initialize_logging(config_file):
    """
        Originally done using dictConfig but available only for python2.7+
    """
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

