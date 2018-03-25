#!/bin/sh
echo GIT_DIR="/git-server/repos/sample.git" python /test-setup/publish.py >> /git-server/repos/sample.git/hooks/post-receive
cd /git-server/repos/sample.git/
cp /test-setup/default.html "$3"
chown git:git /target
chown git:git "$3"
git config --add websub.files "$3"
GIT_DIR=/git-server/repos/sample.git python /test-setup/set-self-url.py "$2"
GIT_DIR=/git-server/repos/sample.git python /test-setup/set-hub-url.py "$1"
