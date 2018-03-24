FROM jkarlos/git-server-docker

# https://github.com/gliderlabs/docker-alpine/blob/master/docs/usage.md
RUN apk add --no-cache \
  libxslt-dev \
  libxml2-dev \
  libgit2-dev \
  curl \
  python \
  python-dev \
  py-pip \
  build-base \
  libffi-dev \
  && pip install --upgrade pip \
  && rm -rf /var/cache/apk/*

COPY requirements.txt /
RUN pip install -r /requirements.txt
