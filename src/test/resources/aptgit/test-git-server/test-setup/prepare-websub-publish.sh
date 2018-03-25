#!/bin/sh
HUB_URL="$1"
HTTP_STATIC_ENDPOINT="$2"
HTML_FILE_LOCATION="$3"
REPOSITORY_NAME="$4"
echo GIT_DIR="/git-server/repos/${REPOSITORY_NAME}.git" python /test-setup/publish.py >> /git-server/repos/${REPOSITORY_NAME}.git/hooks/post-receive
cd /git-server/repos/${REPOSITORY_NAME}.git/
cp /test-setup/default.html ${HTML_FILE_LOCATION}
chown git:git /target
chown git:git ${HTML_FILE_LOCATION}
git config --add websub.files ${HTML_FILE_LOCATION}
GIT_DIR=/git-server/repos/${REPOSITORY_NAME}.git python /test-setup/set-self-url.py ${HTTP_STATIC_ENDPOINT}
GIT_DIR=/git-server/repos/${REPOSITORY_NAME}.git python /test-setup/set-hub-url.py ${HUB_URL}
