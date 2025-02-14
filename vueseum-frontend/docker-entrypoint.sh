#!/bin/bash
set -e

if [ "$NODE_ENV" = "development" ]; then
    echo "Starting in development mode..."
    exec npm run dev -- --host 0.0.0.0
else
    echo "Starting in production mode..."
    # npm run build
    exec "$@"
fi