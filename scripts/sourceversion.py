import subprocess
import sys
from datetime import datetime, timezone

def get_time_in_timezone(zone: str = "Europe/Bratislava"):
    try:
        from zoneinfo import ZoneInfo
        my_tz = ZoneInfo(zone)
    except Exception as e:
        my_tz = timezone.utc
    return datetime.now(my_tz)


if __name__ == '__main__':
    ts = get_time_in_timezone()
    print(f"This info was generated on: {ts.strftime('%Y-%m-%d %H:%M:%S %Z%z')}")

    cmd = 'git log -1 --pretty=format:"Git hash: %H Date of commit: %ai"'
    subprocess.check_call(cmd, shell=True)

    link = sys.argv[1] + sys.argv[2]
    print(' Build run: ' + link + ' ')
