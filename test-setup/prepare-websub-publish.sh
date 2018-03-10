#!/bin/sh
echo GIT_DIR="/git-server/repos/sample.git" python /test-setup/publish.py >> /git-server/repos/sample.git/hooks/post-receive
cd /git-server/repos/sample.git/
cp /test-setup/default.html /target/blah.html
git config --add websub.files /target/blah.html