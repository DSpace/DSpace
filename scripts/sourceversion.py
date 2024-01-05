import subprocess
from datetime import datetime

if __name__ == '__main__':
    ts = datetime.now()
    print(f"timestamp: {ts}")

    cmd = 'git log -1 --pretty=format:"%h - %ai"'
    print(f">{cmd}")
    subprocess.check_call(cmd, shell=True)

    cmd = 'git status --porcelain'
    print(f">{cmd}:")
    subprocess.check_call(cmd, shell=True)
