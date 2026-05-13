#!/bin/bash
# Run Jenkins as root so the pipeline can access the Docker socket (Docker Desktop for Windows)
exec /usr/bin/tini -- /usr/local/bin/jenkins.sh "$@"
