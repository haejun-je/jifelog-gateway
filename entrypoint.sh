#!/bin/sh
set -eu

need_file () {
  if [ ! -f "$1" ]; then
    echo "Missing secret file: $1" >&2
    exit 1
  fi
}

need_file /etc/secrets/JIFELOG_DB_HOST
need_file /etc/secrets/JIFELOG_DB_NAME
need_file /etc/secrets/JIFELOG_DB_USER
need_file /etc/secrets/JIFELOG_DB_PASSWORD
need_file /etc/secrets/JIFELOG_REDIS_HOST
need_file /etc/secrets/JIFELOG_REDIS_PASSWORD
need_file /etc/secrets/JIFELOG_MAIL_API_KEY
need_file /etc/secrets/JIFELOG_JWT_SECRET

export JIFELOG_DB_HOST="$(tr -d '\r\n' <  /etc/secrets/JIFELOG_DB_HOST)"
export JIFELOG_DB_NAME="$(tr -d '\r\n' <  /etc/secrets/JIFELOG_DB_NAME)"
export JIFELOG_DB_USER="$(tr -d '\r\n' <  /etc/secrets/JIFELOG_DB_USER)"
export JIFELOG_DB_PASSWORD="$(tr -d '\r\n' <  /etc/secrets/JIFELOG_DB_PASSWORD)"
export JIFELOG_REDIS_HOST="$(tr -d '\r\n' <  /etc/secrets/JIFELOG_REDIS_HOST)"
export JIFELOG_REDIS_PASSWORD="$(tr -d '\r\n' <  /etc/secrets/JIFELOG_REDIS_PASSWORD)"
export JIFELOG_MAIL_API_KEY="$(tr -d '\r\n' <  /etc/secrets/JIFELOG_MAIL_API_KEY)"
export JIFELOG_JWT_SECRET="$(tr -d '\r\n' <  /etc/secrets/JIFELOG_JWT_SECRET)"

exec java -jar /app/app.jar