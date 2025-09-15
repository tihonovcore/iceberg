#include "postgres.h"
#include "fmgr.h"
#include "catalog/pg_proc.h"
#include "catalog/pg_type.h"
#include "utils/array.h"
#include "utils/elog.h"
#include "utils/builtins.h"
#include "utils/syscache.h"
#include "access/htup_details.h"
#include "executor/executor.h"   // для FCInfo и триггеров
#include "commands/trigger.h"  
#include "utils/lsyscache.h"
#include "catalog/pg_type.h"

#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

PG_MODULE_MAGIC;

/* Главный entrypoint */
PG_FUNCTION_INFO_V1(iceberg_call_handler);

void preprocess(FunctionCallInfo fcinfo, char* src, char* out);
void add_argument(FunctionCallInfo fcinfo, int kth, char* out, int* j);
void interpret(FunctionCallInfo fcinfo, char* src);

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

    char preprocessed[16384];
    for (int i = 0; i < 16384; i++) {
        preprocessed[i] = '\0';
    }

    preprocess(fcinfo, src, preprocessed);
    interpret(fcinfo, preprocessed);
    elog(INFO, "===========================");

    ReleaseSysCache(procTuple);

    //TODO: read and parse output
    Oid ret_oid = get_func_rettype(fn_oid);
    switch (ret_oid) {
        case INT4OID: { // int4
            PG_RETURN_INT32(42);
        }
        case INT8OID: { // int8
            PG_RETURN_INT64(4200L);
        }
        case BOOLOID: { // bool
            PG_RETURN_BOOL(true);
        }
        case TEXTOID: { // text
            PG_RETURN_TEXT_P(cstring_to_text("hello"));
        }
        default: {
            PG_RETURN_NULL();
        }
    }
}

void preprocess(FunctionCallInfo fcinfo, char* src, char *out) {
    int i = 0;
    int j = 0;

    while (isspace(src[i])) {
        out[j++] = src[i++];
    }
    while (strncmp(src + i, "import", 6) == 0) {
        while (i == 0 || src[i - 1] != ';') {
            out[j++] = src[i++];
        }
        while (isspace(src[i])) {
            out[j++] = src[i++];
        }
    }

    for (int kth = 0; kth < PG_NARGS(); kth++) {
        add_argument(fcinfo, kth, out, &j);
    }

    while (src[i - 1] != '\0') {
        out[j++] = src[i++];
    }

    elog(INFO, "preprocessed:");
    elog(INFO, out);
}

void add_argument(FunctionCallInfo fcinfo, int kth, char* out, int* j) {
    //get argument name
    Oid funcid = fcinfo->flinfo->fn_oid;
    HeapTuple procTuple = SearchSysCache1(PROCOID, ObjectIdGetDatum(funcid));
    if (!HeapTupleIsValid(procTuple))
        elog(ERROR, "cache lookup failed for function %u", funcid);

    Datum proargnames_datum;
    bool isnull;
    proargnames_datum = SysCacheGetAttr(PROCOID, procTuple, Anum_pg_proc_proargnames, &isnull);
    if (isnull) {
      elog(ERROR, "isnull");
      return;
    }

    ArrayType *proargnames = DatumGetArrayTypeP(proargnames_datum);

    int indx[1] = { kth + ARR_LBOUND(proargnames)[0] }; // индекс элемента
    bool isnull2;
    Datum d = array_ref(proargnames,
        1,
        indx,
        -1,
        -1,
        false,
        'i',
        &isnull2
    );

    char *arg_name = text_to_cstring(DatumGetTextPP(d));
    ReleaseSysCache(procTuple);

    //get argument type
    Oid arg_type = get_fn_expr_argtype(fcinfo->flinfo, kth);

    //depends on arg_type extract arg_value and
    //write `def <arg_name> = <arg_value>;` to output
    switch (arg_type) {
        case INT4OID: {
            int32 arg_value = PG_GETARG_INT32(kth);

            char var[256];
            snprintf(var, sizeof(var), "def %s = %d;\n    ", arg_name, arg_value);

            strcat(out, var);
            *j += strlen(var);

            break;
        }
        case INT8OID: {
            int64 arg_value = PG_GETARG_INT64(kth);

            char var[256];
            snprintf(var, sizeof(var), "def %s = %lld;\n    ", arg_name, arg_value);

            strcat(out, var);
            *j += strlen(var);

            break;
        }
        case TEXTOID: {
            text *t = PG_GETARG_TEXT_PP(kth);

            char var[256];
            snprintf(var, sizeof(var), "def %s = \"%s\";\n    ", arg_name, text_to_cstring(t));

            strcat(out, var);
            *j += strlen(var);

            break;
        }
        case BOOLOID: {
            char var[256];
            if (PG_GETARG_BOOL(kth)) {
                snprintf(var, sizeof(var), "def %s = true;\n    ", arg_name);
            } else {
                snprintf(var, sizeof(var), "def %s = false;\n    ", arg_name);
            }

            strcat(out, var);
            *j += strlen(var);

            break;
        }
        default: {
            elog(INFO, "Arg %d has unsupported type oid=%u", kth, arg_type);
            break;
        }
    }
}

void interpret(FunctionCallInfo fcinfo, char* src) {
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
  strcpy(cmd, "java -cp /usr/lib/iceberg/iceberg.jar iceberg.CompilationPipeline -run ");
  strcat(cmd, tmp_filename);

  elog(INFO, cmd);

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

  //TODO: если out плохой - вернуть ошибку в консоль (не в логи)

  pclose(fp);

  // удаляем временный файл
  unlink(tmp_filename);
}
