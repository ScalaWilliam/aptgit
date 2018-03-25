#!/bin/sh
HUB_URL="$1"
TOPIC_URL="$2"
TOPIC_FILE="$3"
REPOSITORY_NAME="$4"
echo GIT_DIR="/git-server/repos/${REPOSITORY_NAME}.git" python /test-setup/publish.py >> /git-server/repos/${REPOSITORY_NAME}.git/hooks/post-receive
cd /git-server/repos/${REPOSITORY_NAME}.git/
cp /test-setup/default.html ${TOPIC_FILE}
chown git:git /target
chown git:git ${TOPIC_FILE}
git config --add websub.files ${TOPIC_FILE}
GIT_DIR=/git-server/repos/${REPOSITORY_NAME}.git python /test-setup/set-self-url.py ${TOPIC_URL}
GIT_DIR=/git-server/repos/${REPOSITORY_NAME}.git python /test-setup/set-hub-url.py ${HUB_URL}
