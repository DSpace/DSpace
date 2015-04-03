# coding=utf-8
# This work is licensed!
# pylint: disable=W0702,R0201,C0111,W0613,R0914

"""
  Main entry point.

  There are three objects.solradapter

  apachee - parsing apache logs storing
    [time] -> [ [url, ip], ... ]

  dspacee - replacing dspace log's ip addresses with the correct one from apache

  solree - asking solr stats for results in time range from apache logs and if it is not
  ambiguous it replaces it with a correct ip and without dns. It is done for all items
  in apachee.

  Should be properly checked which one is currently being applied.
   Specific for one time usage.

   jm@ufal (stuff taken from jm's private libs)
"""

import sys
import re
import logging
import getopt
import time
import glob
import solradapter
from datetime import datetime, timedelta

from settings import settings as settings_inst
import utils
# initialise logging
utils.initialize_logging( settings_inst["logger_config"] )
_logger = logging.getLogger( 'common' )


#=======================================
# help
#=======================================

def help_msg(env):
    """ Returns help message. """
    _logger.warning( u"""\n%s help. Supported commands:\n""", env["name"] )


#=======================================
# do stuff
#=======================================


class apachee(object):
    """
        Get logs according to time.
    """
    _regex = re.compile(ur"(?P<ip>[\d\.]+) - - \[(?P<time>.*)\] \"GET (?P<url>.*) HTTP")
    _report_after = 50000

    def __init__(self, logs, maxx=-1):
        self.d = {}
        i = 0
        for log in logs:
            with open(log, mode="rb") as fin:
                for line in fin:
                    m = apachee._regex.match(line)
                    if m is None:
                        continue
                    i += 1
                    ttime = m.group("time")
                    ttime = ttime[:ttime.find(" ")]
                    if ttime not in self.d:
                        self.d[ttime] = []
                    self.d[ttime].append( (m.group("url"), m.group("ip")) )
                    #_logger.info( m.groups() )
                    if maxx != -1 and i > maxx:
                        return
                    if i > 0 and i % apachee._report_after == 0:
                        _logger.info( "At [%s]", ttime )

    @staticmethod
    def atime2datetime(atime):
        date = atime[:atime.find(" ")]
        line_timestamp = datetime.strptime(date, "%d/%b/%Y:%H:%M:%S")
        return line_timestamp

    @staticmethod
    def datetime2atime(ttime):
        return ttime.strftime("%d/%b/%Y:%H:%M:%S")

    def __getitem__(self, time_str):
        return self.d.get(time_str)


#noinspection PyShadowingNames,PyUnusedLocal
class dspacee(object):
    """
        Get logs.
    """
    _regex = re.compile(
        ur"^(?P<time>.*) INFO.*LoggerUsageEventListener.*ip_addr=(?P<ip>[\d\.]+):.*view_item:handle=(?P<url>.*)$")
    _report_after = 500000

    def __init__(self, logs, apachee, maxx=-1):

        def _dec_time(dtime):
            tmp = datetime.strptime(dtime, "%d/%b/%Y:%H:%M:%S")
            tmp -= timedelta(seconds=1)
            return apachee.datetime2atime( tmp )

        def _find_time(dtime, url, ip):
            # is the time
            ret = apachee[dtime]
            if ret is None:
                return []
            # is the url
            for (aurl, aip) in ret:
                if url in aurl:
                    return ip, aip
            return []

        self.d = {}
        for log in logs:
            _logger.info( "Working on [%s]", log )
            with open(log, mode="rb") as fin:
                with open(log + ".fixed", mode="wb+") as fout:
                    for line in fin:
                        m = dspacee._regex.match(line)
                        if m is not None:
                            dtime = m.group("time")
                            dtime = dspacee.dtime2atime(dtime)
                            url = m.group("url")
                            ip = m.group("ip")
                            ret = _find_time(dtime, url, ip)
                            if len(ret) == 0:
                                ret = _find_time(_dec_time(dtime), url, ip)
                            if len(ret) != 0 and ret[0] != ret[1]:
                                line = line.replace( ret[0], ret[1])
                                _logger.info( "Replacing [%s]->[%s]", ret[0], ret[1])
                            else:
                                line = line

                        #
                        fout.write(line)

    @staticmethod
    def dtime2atime(ttime):
        ttime = datetime.strptime(ttime, "%Y-%m-%d %H:%M:%S,%f")
        return ttime.strftime("%d/%b/%Y:%H:%M:%S")

    def __getitem__(self, time_str):
        return self.d.get(time_str)


#noinspection PyShadowingNames,PyUnusedLocal
class solree( object ):

    def __init__(self, env_inst, apachee, maxx=-1):
        key_item = "11858/00-097C-0000-000E-011B-8"
        key_item_id = "141"
        key_item_ip = "195.113.20.140"

        _logger.info( "Checking [%d] entries", len(apachee.d) )
        # get solr
        solrstats = solradapter.adapter( env_inst )
        changed = 0
        for ttime, vals in apachee.d.iteritems():
            # id 141
            # handle 11858/00-097C-0000-000E-011B-8
            for (url, ip) in vals:
                if url.endswith(key_item):
                    # try to replace the solr item if it exists
                    stime_arr = solree.atime2stime( ttime )
                    query = "id:%s AND ip:%s AND time:%s" % (
                        key_item_id, key_item_ip, stime_arr)
                    resp = solrstats.search( {"q": query} )
                    if 1 == len(resp.result.docs):
                        # delete it and add it
                        doc = resp.result.docs[0]
                        solrstats.delete( queries=[query] )
                        solrstats.commit()
                        if "dns" in doc:
                            del doc["dns"]
                        old_ip = doc["ip"]
                        doc["ip"] = ip
                        solrstats.add( doc )
                        solrstats.commit()
                        changed += 1
                        _logger.info("Changing [%s]->[%s] done %d", old_ip, ip, changed)
                    break
        _logger.info( "Changed [%d] times", changed )

    @staticmethod
    def atime2stime( ttime ):
        line_timestamp = datetime.strptime(ttime, "%d/%b/%Y:%H:%M:%S")
        onsec_minus_ttime = line_timestamp - timedelta(seconds=1)
        onsec_plus_ttime = line_timestamp + timedelta(seconds=1)
        # 2012-03-19T04:23:15Z
        return "[%s TO %s]" % (
            onsec_minus_ttime.strftime("%Y-%m-%dT%H:%M:%SZ"),
            onsec_plus_ttime.strftime("%Y-%m-%dT%H:%M:%SZ"))


def parse_logs( env_inst ):
    """
        Parse the logs.
    """

    def _find_files( key ):
        """ Get files """
        glob_specs = env_inst[key + "_dir"]
        _logger.info( u"Looking for %s logs [%s]", key, glob_specs )
        files = []
        for glob_spec in glob_specs:
            files += glob.glob( glob_spec )
        _logger.info( u"Found [%d] files", len(files) )
        return files

    # get apache logs
    #
    apache_logs = _find_files( "apache" )
    apa = apachee(apache_logs, maxx=-1)
    _logger.info( "apachee >= [%d] entries", len(apa.d) )

    # solr
    #
    solree( env_inst, apa )

    # get dspace logs
    #
    #dspace_logs = _find_files( "dspace" )
    # dspace logs
    #dsp = dspacee(dspace_logs, apa)
    #_logger.info( "dspacee >= [%d] entries", len(dsp.d) )


#=======================================
# command line
#=======================================

def parse_command_line(env):
    """ Parses the command line arguments. """
    try:
        options = ["help",
                   ]
        input_options = sys.argv[1:]
        opts, _ = getopt.getopt( input_options, "", options )
    except getopt.GetoptError:
        help_msg( env )
        sys.exit( 1 )

    what_to_do = None
    for option, _1 in opts:
        if option == "--help":
            env["print_info"] = False
            return help_msg

    if what_to_do:
        return what_to_do
        # what to do but really?
    return parse_logs


#=======================================
# main
#=======================================

if __name__ == "__main__":
    lasted = time.time()
    _logger.info( u"Starting at " + utils.host_info() )

    # do what was specified or default
    try:
        what_to_do_callable = parse_command_line( settings_inst )
        what_to_do_callable( settings_inst )
    except Exception, e_inst:
        _logger.critical( "An exception occurred, ouch:\n%s", e_inst )
        raise
    finally:
        lasted = time.time() - lasted
        _logger.info( "Stopping after [%f] secs.", lasted )
