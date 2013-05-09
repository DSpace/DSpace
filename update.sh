git fetch datadryad
git checkout -f dryad-dev
git rebase datadryad/dryad-dev
git checkout -f dryad-master
git rebase datadryad/dryad-master
git checkout -f dryad-staging
git rebase datadryad/dryad-staging
