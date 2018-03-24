#!/bin/sh
git config --global user.email "test.user@example.com"
git config --global user.name "Test User"
cd /tmp
git clone git@local-git:/git-server/repos/sample.git
cd sample
date >> date
git add date
git commit -m "Set current date"
git push
cd ..
rm -rf sample