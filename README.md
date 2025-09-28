# Iceberg
##### simple language with JVM-backend

### simple syntax
```iceberg
def x: i32;
x = 123456;
def y = 99;

def i = 0;
while i < 9 then {
    print max(x, y);

    y = y * 9;
    i = i + 1;
}

fun max(x: i32, y: i32): i32 {
    if x > y 
    then return x;
    else return y;
}
```

### classes
```iceberg
class Calendar {
    def year: i32 = 2025
    def month: i32 = 12

    fun increment() {
        if (this.month == 12) then {
            this.month = 1;
            this.year = this.year + 1;
        } else {
            this.month = this.month + 1;
        }
    }

    fun total(): i32 {
        return this.year * 12 + this.month;
    }
}

def cal = new Calendar;
print cal.total();  //24312
print cal.year;     //2025
print cal.month;    //12

cal.increment();
print cal.total();  //24313
print cal.year;     //2026
print cal.month;    //1
```

### java imports
```iceberg
import java.util.ArrayList;
        
def list = new ArrayList;
list.add("10");
list.add("20");
list.add("30");

def sub = list.subList(1, 3);
print sub;
```

### cli
Установка
```shell
sudo cp script/ice.sh /usr/local/bin/ice
chmod +x /usr/local/bin/ice

mvn clean package -DskipTests=true
sudo mkdir -p /usr/local/share/ice
sudo cp target/*-with-dependencies.jar /usr/local/share/ice/compiler.jar
```
Возможности

    ice [-cp <path>] source.ib
    ice [-cp <path>] -run source.ib
    ice [-cp <path>] -jar source.ib

### bridge
Язык не умеет работать со статическими полями, такими как `System.in`.
Также нет поддержки конструкторов с несколькими аргументами.
Поэтому в чистом виде не получится, например, создать `Scanner`.
Тем не менее доступен следующий work-around. 
Можно создать обработчик на Java (или другом JVM языке), который
инкапсулирует незнакомую языку логику: 
```java
import java.util.Scanner;

public class Handler {
    public Scanner scanner() {
        return new Scanner(System.in);
    }
}
```
Этот класс нужно независимо откомпилировать (`javac Handler.java`) и передать в classpath
при компиляции основного кода: `ice -cp ./compiled-java source.ib`. Пример работы с Handler:
```iceberg
import Handler;

def handler = new Handler;
def scanner = handler.scanner();

def x: i32 = scanner.nextInt();
print x;
```

### list implementation
Пример реализации связного списка
```iceberg
import java.lang.StringBuilder;

class Node {
  def value: string

  def next: Node
  def hasNext: bool = false
}

class LinkedList {
  def head: Node = new Node

  fun add(value: string) {
    def next = new Node;
    next.value = value;

    def curr = this.head;
    while curr.hasNext then curr = curr.next;

    curr.next = next;
    curr.hasNext = true;
  }

  fun get(i: i32): string {
    return this.find(this.head.next, i).value;
  }

  fun set(i: i32, value: string) {
    this.find(this.head.next, i).value = value;
  }

  fun find(from: Node, i: i32): Node {
    if i == 0 then return from;
    else return this.find(from.next, i - 1);
  }

  fun toString(): string {
    def sb = new StringBuilder;
    sb.append("[");
    def curr = this.head;
    while curr.hasNext then {
      curr = curr.next;
      sb.append(curr.value);

      if curr.hasNext then {
        sb.append(", ");
      }
    }
    sb.append("]");
    
    return sb.toString();
  }
}

def list = new LinkedList;
print list;           // []

list.add("foo");
print list;           // [foo]

list.add("qux");
print list;           // [foo, qux]

list.add("bar");
print list;           // [foo, qux, bar]

print list.get(0);    // foo
print list.get(1);    // qux
print list.get(2);    // bar

list.set(0, "alpha");
print list;           // [alpha, qux, bar]

list.set(1, "beta");
print list;           // [alpha, beta, bar]

list.set(2, "gamma");
print list;           // [alpha, beta, gamma]
```