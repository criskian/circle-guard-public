#!/bin/bash
# Fix docker socket permissions so jenkins user can access Docker
chmod 666 /var/run/docker.sock 2>/dev/null || true
exec /usr/bin/tini -- /usr/local/bin/jenkins.sh "$@"
