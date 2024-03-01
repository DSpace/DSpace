import subprocess
import sys
from datetime import datetime

if __name__ == '__main__':
    ts = datetime.now()
    print(f"This info was generated on: {ts}")

    cmd = 'git log -1 --pretty=format:"Git hash: %H Date of commit: %ai"'
    subprocess.check_call(cmd, shell=True)

    link = sys.argv[1] + sys.argv[2]
    print(' Build run: ' + link + ' ')
