import subprocess
import sys
from datetime import datetime, timezone, timedelta

def get_time_in_timezone(timezone_offset):
    return datetime.now(timezone.utc) + timedelta(hours=timezone_offset)

if __name__ == '__main__':
    cet_offset = 1
    ts = get_time_in_timezone(cet_offset)
    print(f"This info was generated on: {ts.strftime('%Y-%m-%d %H:%M:%S %Z%z')}")

    cmd = 'git log -1 --pretty=format:"Git hash: %H Date of commit: %ai"'
    subprocess.check_call(cmd, shell=True)

    link = sys.argv[1] + sys.argv[2]
    print(' Build run: ' + link + ' ')
