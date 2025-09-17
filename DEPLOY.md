Заполнить `~/.m2/settings.xml`:
```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
http://maven.apache.org/xsd/settings-1.0.0.xsd">
   
     <servers>
        <server>
            <id>github</id>
            <username>username</username>
            <password>token</password>
        </server>
    </servers>
</settings>
```
Токен можно создать тут https://github.com/settings/tokens - Tokens (classic) <br>
Для чтения нужны права `read:packages`, для публикации `write:packages`.

Деплой делается через `mvn deploy`.

Внедрение как зависимость (все равно нужен токен на чтение)
```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/tihonovcore/iceberg</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.tihonovcore</groupId>
        <artifactId>iceberg</artifactId>
        <version>0.1</version>
    </dependency>
</dependencies>
```