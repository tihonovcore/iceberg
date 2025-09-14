#include "postgres.h"
#include "fmgr.h"
#include "catalog/pg_proc.h"
#include "utils/elog.h"
#include "utils/builtins.h"
#include "utils/syscache.h"
#include "access/htup_details.h"
#include "executor/executor.h"   // для FCInfo и триггеров
#include "commands/trigger.h"  

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

PG_MODULE_MAGIC;

/* Главный entrypoint */
PG_FUNCTION_INFO_V1(iceberg_call_handler);

void interpret();

Datum
iceberg_call_handler(PG_FUNCTION_ARGS)
{
    if (CALLED_AS_TRIGGER(fcinfo)) {
        elog(ERROR, "Trigger not supported yet");
        PG_RETURN_NULL();
    }

    /* Получаем OID функции */
    Oid fn_oid = fcinfo->flinfo->fn_oid;
    /* Достаём кортеж из pg_proc */
    HeapTuple procTuple = SearchSysCache1(PROCOID, ObjectIdGetDatum(fn_oid));
    if (!HeapTupleIsValid(procTuple))
        elog(ERROR, "cache lookup failed for function %u", fn_oid);

    Form_pg_proc procStruct = (Form_pg_proc) GETSTRUCT(procTuple);

    /* prosrc — это тело функции (AS $$ ... $$) */
    Datum prosrc_datum;
    bool isnull;
    
    prosrc_datum = SysCacheGetAttr(PROCOID, procTuple,
                               Anum_pg_proc_prosrc, &isnull);

    if (isnull)
        elog(ERROR, "Function prosrc is NULL");

    char *src = TextDatumGetCString(prosrc_datum);

    elog(INFO, "Iceberg executing function %s(...):", NameStr(procStruct->proname));
    elog(INFO, "Source code: %s", src);
    elog(INFO, "===========================");
    interpret(src);
    elog(INFO, "===========================");

    ReleaseSysCache(procTuple);

    PG_RETURN_NULL();
}

void interpret(char* src) {
// создаём временный файл
  char tmp_filename[] = "/tmp/source.ibXXXXXX";
  int fd = mkstemp(tmp_filename);
  if (fd == -1) {
      elog(ERROR, "Failed to create temporary file");
      return;
  }

  // записываем src
  FILE *tmp_fp = fdopen(fd, "w");
  if (!tmp_fp) {
      close(fd);
      unlink(tmp_filename);
      elog(ERROR, "Failed to open temporary file");
      return;
  }
  fprintf(tmp_fp, "%s", src);
  fclose(tmp_fp);  // закрываем, чтобы java могла читать

  // формируем команду
  char cmd[4096];
  snprintf(cmd, sizeof(cmd),
           "java -cp /usr/lib/iceberg/iceberg.jar iceberg.CompilationPipeline -run %s",
           tmp_filename);

  // запускаем JAR
  FILE *fp = popen(cmd, "r");
  if (fp == NULL) {
      unlink(tmp_filename);
      elog(ERROR, "Failed to run Java interpreter");
      return;
  }

  // читаем stdout
  char output[1024];
  while (fgets(output, sizeof(output)-1, fp) != NULL) {
      size_t len = strlen(output);
      if (len > 0 && output[len-1] == '\n')
          output[len-1] = '\0'; // удаляем перенос строки
      elog(INFO, output);
  }

  pclose(fp);

  // удаляем временный файл
  unlink(tmp_filename);
}
