### install
```shell
mvn clean package -DskipTests=true
cp target/*-with-dependencies.jar ./postgres/iceberg.jar

docker build -t pg-iceberg ./postgres
docker-compose -f ./postgres/docker-compose.yml up -d
```

### extension usage
```sql
CREATE EXTENSION iceberg;
```
```sql
CREATE OR REPLACE FUNCTION print_list() RETURNS void AS $$
    import java.util.ArrayList;

    def list = new ArrayList;

    list.add("foo");
    list.add("bar");
    list.add("qux");

    print list;
$$ LANGUAGE iceberg;

select print_list();
```
```sql
CREATE OR REPLACE FUNCTION fibonacci() RETURNS void AS $$
    def f = 1;
    def s = 1;
                
    def i = 0;
    while i < 10 then {
        print f;

        def tmp = f + s;
        f = s;
        s = tmp;

        i = i + 1;
    }
$$ LANGUAGE iceberg;

select fibonacci();
```

### passing parameters
```sql
CREATE or replace FUNCTION pass_arguments(
    a int4, b int8, c text, d bool
) RETURNS void AS $$
    import iceberg.pg.Reader;

    def reader = new Reader;

    print reader.i32(0) + 10;
    print reader.i64(1) + 100;
    print reader.string(2);
    print reader.bool(3) or true;
$$ LANGUAGE iceberg;

select pass_arguments(4, 40, 'qux', false);
```