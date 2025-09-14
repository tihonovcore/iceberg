### install
```shell
mvn clean package -DskipTests=true
cp target/*-with-dependencies ./postgres/iceberg.jar

docker build -t pg-iceberg ./postgres
docker-compose -f ./postgres/docker-compose.yml up -d
```

### extension usage
```sql
CREATE EXTENSION iceberg;

--check if extension added
SELECT * FROM pg_language;

CREATE OR REPLACE FUNCTION test_iceberg() RETURNS void AS $$
    import java.util.ArrayList;

    def list = new ArrayList;

    list.add("foo");
    list.add("bar");
    list.add("qux");

    print list;

    //fibonacci
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

select test_iceberg();
```