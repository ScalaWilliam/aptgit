#!/bin/sh
REPO_PATH="$1"
git config --global user.email "test.user@example.com"
git config --global user.name "Test User"
cd /tmp
git clone git@local-git:${REPO_PATH} repo
cd repo
date >> date
git add date
git commit -m "Set current date"
git push
cd ..
rm -rf repo