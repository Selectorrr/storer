#! /bin/sh

set -e

PWD=$(dirname $(readlink -f $0))
APP_HOME=$(dirname ${PWD})
LIB_DIR=${APP_HOME}/lib
APP=${LIB_DIR}/app.war

cd ${APP_HOME}

exec java -jar ${APP} --spring.profiles.active=prod
