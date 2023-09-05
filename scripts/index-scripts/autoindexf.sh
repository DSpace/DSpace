mkdir -p "${1%/*}"
./indexhandle $1 > $1manage.out 2> $1manage.err &
disown
