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
После создания `EXTENSION` можно создавать и вызывать функции. <br>
NOTE: `print` выводится в консоль, а не в `return` функции. 
```sql
CREATE OR REPLACE FUNCTION fibonacci(
    n int4
) RETURNS void AS $$
    if n <= 0 then return;

    def f = 1;
    def s = 1;
                
    def i = 0;
    while i < n - 1 then {
        def tmp = f + s;
        f = s;
        s = tmp;

        i = i + 1;
    }

    print f;
$$ LANGUAGE iceberg;

select fibonacci();
```
Поддерживаются аргументы `int4`, `int8`, `text`, `bool`:
```sql
CREATE OR REPLACE FUNCTION test_args(
    a int4, b int8, c text, d bool
) RETURNS void AS $$
    print b + 10;
    print a + 10;
    print d or true;
    print c;
$$ LANGUAGE iceberg;

select test_args(3, 14, 'qux', false);
```
Можно использовать стандартную библиотеку Java:
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