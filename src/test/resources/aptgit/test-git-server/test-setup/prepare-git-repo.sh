#!/bin/sh
name="$1"
git init --bare /git-server/repos/${name}.git
chown -R git:git /git-server/repos/${name}.git
cp /test-setup/post-receive /git-server/repos/${name}.git/hooks/post-receive