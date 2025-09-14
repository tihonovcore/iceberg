FROM postgres:17

RUN apt-get update \
 && apt-get install -y build-essential postgresql-server-dev-17 openjdk-21-jdk \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /usr/src/iceberg
COPY iceberg.c iceberg.control iceberg--0.1.sql iceberg.jar ./

RUN mkdir -p /usr/lib/iceberg \
 && cp iceberg.jar /usr/lib/iceberg/

RUN gcc -fPIC -I/usr/include/postgresql/17/server -c iceberg.c \
 && gcc -shared -o iceberg.so iceberg.o \
 && cp iceberg.so `pg_config --pkglibdir` \
 && cp iceberg.control iceberg--0.1.sql `pg_config --sharedir`/extension/
