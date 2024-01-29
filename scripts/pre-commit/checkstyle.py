import sys
import logging
import subprocess

_logger = logging.getLogger()
logging.basicConfig(format='%(message)s', level=logging.DEBUG)


if __name__ == '__main__':
    files = [x for x in sys.argv[1:] if x.lower().endswith('java')]
    _logger.info(f'Found [{len(files)}] files from [{len(sys.argv) - 1}] input files')

    cmd = "mvn checkstyle:check"

    try:
        with subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE, text=True) as process:
            for line in process.stdout:
                print(line, end='')
    except Exception as e:
        _logger.critical(f'Error: {repr(e)}, ret code: {e.returncode}')

    # for filename in files:
    #     pass
