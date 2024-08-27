import subprocess
import sys
from datetime import datetime, timezone, timedelta

def get_time_in_timezone(timezone_offset):
    try:
        from zoneinfo import ZoneInfo
        my_tz = ZoneInfo("Europe/Bratislava")
    except Exception as e:
        my_tz = timezone.utc
    return datetime.now(my_tz)


if __name__ == '__main__':
    cet_offset = 1
    ts = get_time_in_timezone(cet_offset)
    # we have html tags, since this script ends up creating VERSION_D.html
    print(f"<h4>This info was generated on: <br> <strong> {ts.strftime('%Y-%m-%d %H:%M:%S %Z%z')} </strong> </h4>")

    cmd = 'git log -1 --pretty=format:"<h4>Git hash: <br><strong> %H </strong> <br> Date of commit: <br> <strong> %ai </strong></h4>"'
    subprocess.check_call(cmd, shell=True)

    # when adding argparse, this should be a bit more obvious
    link = sys.argv[1] + sys.argv[2]
    print('<br> <h4>Build run: </h4> <a href="' + link + '"> ' + link + '</a> ')
