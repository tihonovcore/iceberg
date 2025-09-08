#!/usr/bin/env bash

JAR="/usr/local/share/ice/compiler.jar"

classpath="$(pwd)"
if [[ $1 == "-cp" ]]; then
  classpath="$2"
  shift 2
fi

mode="-compile"
if [[ $1 == "-jar" || $1 == "-run" ]]; then
  mode="$1"
  shift
fi

if [ -z "$1" ]; then
  echo "Не указан .ib файл"
  exit 1
fi

source=$1

exec java -cp "$classpath:$JAR" iceberg.CompilationPipeline "$mode" "$source"