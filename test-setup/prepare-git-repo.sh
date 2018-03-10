#!/bin/sh
git init --bare /git-server/repos/sample.git
chown -R git:git /git-server/repos/sample.git
cp /test-setup/post-receive /git-server/repos/sample.git/hooks/post-receive