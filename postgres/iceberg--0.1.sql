CREATE FUNCTION iceberg_call_handler()
    RETURNS language_handler
    AS 'iceberg'
    LANGUAGE C;

CREATE LANGUAGE iceberg
    HANDLER iceberg_call_handler;
