package e2e;

import iceberg.SemanticException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ClassesTest extends Base {

    @ParameterizedTest
    @MethodSource
    void definitions(String source, String expected) {
        execute(source, expected);
    }

    static Stream<Arguments> definitions() {
        return Stream.of(
            Arguments.of("""
                class Foo {
                }
                
                print "hello";""", "hello\n"),
            Arguments.of("""
                class Foo {
                    fun sayFoo() {
                        print "foo";
                    }
                }

                print "hello";""", "hello\n")
        );
    }

    @Test
    void construct() {
        execute("""
            class Foo {}
            def foo = new Foo;
            
            print "hello";""", "hello\n");
    }

    @Test
    void call() {
        execute("""
            class Foo {
                fun hi() {
                    print "hi";
                }
            }
            
            def foo = new Foo;
            foo.hi();""", "hi\n");
    }

    @Test
    void chainedCall() {
        execute("""
            class Foo {
                fun hi() {
                    print "hi";
                }
            }
            
            class Bar {
                fun get(): Foo {
                    return new Foo;
                }
            }
            
            def bar = new Bar;
            bar.get().hi();""", "hi\n");
    }

    @Test
    void printReturnedValue() {
        execute("""
            class Calendar {
                fun year(): i32 {
                    return 2025;
                }
            }
            
            
            def cal = new Calendar;
            print cal.year();""", "2025\n");
    }

    @Test
    void evalReturnedValue() {
        execute("""
            class Calendar {
                fun mm(): i32 {
                    return 5;
                }

                fun ss(): i32 {
                    return 10;
                }
            }
            
            
            def cal = new Calendar;
            print cal.mm() * 60 + cal.ss();""", "310\n");
    }

    @Test
    void show() {
        execute("""
            class Show {
                fun show(x: i32) {
                    print x;
                }
            }

            def show = new Show;
            show.show(39);""", "39\n");
    }

    @Test
    void passParamsToInstanceMethod() {
        execute("""
            class Math {
                fun sq(x: i32): i32 {
                    return x * x;
                }
            }
            
            def math = new Math;
            print math.sq(3) + math.sq(4);""", "25\n");
    }

    @Test
    void fields() {
        execute("""
            class Rectangle {
                def x: i32
                def y: i32
            }
            
            def req = new Rectangle;
            print req.x * req.y;
            
            req.x = 10;
            print req.x * req.y;

            req.y = 99;
            print req.x * req.y;""", "0\n0\n990\n");
    }

    @Test
    void getterSetter() {
        execute("""
            class Square {
                def x: i32

                fun set(side: i32) {
                    this.x = side;
                }

                fun area(): i32 {
                    return this.x * this.x;
                }
            }
            
            def sq = new Square;
            sq.x = 3;
            print sq.area();
            
            sq.set(4);
            print sq.area();""", "9\n16\n");
    }

    @Test
    void thisOutsideOfFunction() {
        var exception = assertThrows(SemanticException.class, () -> execute("""
            def sq = this;
            print sq;
            """, null));
        assertThat(exception).hasMessage("`this` outside of member-function");
    }

    @Test
    void thisOutsideOfMemberFunction() {
        var exception = assertThrows(SemanticException.class, () -> execute("""
            fun foo() {
                print this;
            }""", null));
        assertThat(exception).hasMessage("`this` outside of member-function");
    }

    @Test
    void fieldDefaultValue() {
        execute("""
            class X {
                def t: i32 = 20
                fun foo() {}
            }
            
            def x = new X;
            print x.t;
            
            x.t = 99;
            print x.t;
            """, "20\n99\n");
    }

    @Test
    void fieldTypedWithUserDefinedClass() {
        execute("""
            class X {
                def value: i32
            }
            class Y {
                def x: X
                fun foo() {}
            }
            
            def x: X = new X;
            x.value = 99;
            
            def y: Y = new Y;
            y.x = x;
            
            print y.x.value;
            """, "99\n");
    }

    @Test
    void fieldTypedWithImportedClass() {
        execute("""
            import java.util.ArrayList;
            
            class NonEmptyList {
                def first: string
                def other: ArrayList

                fun show() {
                    print this.first;
                    print this.other;
                }
            }
            
            def list = new NonEmptyList;
            list.first = "foo";
            list.other = new ArrayList;
            list.other.add("bar");
            list.other.add("qux");
            
            list.show();
            """, "foo\n[bar, qux]\n");
    }

    @Test
    void recursiveType() {
        execute("""
            class Tree {
                def data: i32

                def left: Tree
                def right: Tree
            }
            
            def root = new Tree;
            root.data = 1;
            root.left = new Tree;
            root.left.data = 10;
            root.right = new Tree;
            root.right.data = 100;
            
            print root.data + root.left.data + root.right.data;
            """, "111\n");
    }

    @Test
    void userDefinedClassAsParameter__getField() {
        execute("""
            class Foo {
                def data: i32
            }
            
            fun bar(foo: Foo) {
                print foo.data * foo.data;
            }
            
            def foo = new Foo;
            foo.data = 20;
            
            bar(foo);
            """, "400\n");
    }

    @Test
    void userDefinedClassAsParameter__callMethod() {
        execute("""
            class Foo {
                fun show() {
                    print "hello";
                }
            }
            
            fun bar(foo: Foo) {
                foo.show();
            }
            
            bar(new Foo);
            """, "hello\n");
    }

    @Test
    void userDefinedClassAsReturnType() {
        execute("""
            class Point {
                def x: i32
                def y: i32

                fun len_sq(): i32 {
                    return this.x * this.x + this.y * this.y;
                }
            }
            
            fun build(x: i32, y: i32): Point {
                def point: Point = new Point;
                point.x = x;
                point.y = y;

                return point;
            }
            
            print build(3, 4).len_sq();
            """, "25\n");
    }

    @Test
    void lateInit() {
        execute("""
            class Point {
                def x: i32
                def y: i32
            }
            
            def point: Point;
            point = new Point;
            point.x = 10;
            point.y = 20;
            
            print point.x * point.y;
            """, "200\n");
    }

    @Test
    void highCoupling() {
        execute("""
            class Foo {
                def qux: Qux
                def data: i32

                fun bind(qux: Qux) {
                    qux.foo = this;
                    this.qux = qux;
                }

                fun me(): Foo {
                    return this;
                }

                fun other(): Qux {
                    return this.qux;
                }
            }
            
            class Qux {
                def foo: Foo
                def data: i32

                fun me(): Qux {
                    return this;
                }

                fun other(): Foo {
                    return this.foo;
                }
            }
            
            def foo = new Foo;
            foo.data = 10;
            
            def qux = new Qux;
            qux.data = 11;
            
            foo.bind(qux);
            
            print foo.me().data;
            print foo.other().data;
            print foo.me().data * foo.other().data;
            """, "10\n11\n110\n");
    }

    @Test
    void importedClassAsParameter__callMethod() {
        execute("""
            import java.util.ArrayList;
            
            fun build(list: ArrayList) {
                list.add("10");
                list.add("20");
            }
            
            def list = new ArrayList;
            build(list);
            
            print list.toString();
            """, "[10, 20]\n");
    }

    @Test
    void importedClassAsReturnType() {
        execute("""
            import java.util.ArrayList;
            
            fun buildList(): ArrayList {
                def list = new ArrayList;
                list.add("10");
                list.add("20");
                return list;
            }
            
            print buildList().toString();
            """, "[10, 20]\n");
    }

    @Test
    void lateInitWithImportedClass() {
        execute("""
            import java.util.ArrayList;
            
            def list: ArrayList;
            
            list = new ArrayList;
            list.add("10");
            list.add("20");
            
            print list.toString();
            """, "[10, 20]\n");
    }

    @Test
    void inconsistentType__userDefined() {
        var exception = assertThrows(SemanticException.class, () -> execute("""
            class Foo {}
            class Bar {}
            
            def foo: Foo = new Bar;
            print foo == foo;
            """, null));
        assertThat(exception).hasMessage("incompatible types: Foo and Bar");
    }

    @Test
    void inconsistentType__imported() {
        var exception = assertThrows(SemanticException.class, () -> execute("""
            import java.util.ArrayList;
            import java.util.HashMap;
            
            def list: ArrayList = new HashMap;
            print list.toString();
            """, null));
        assertThat(exception).hasMessage("incompatible types: java/util/ArrayList and java/util/HashMap");
    }

    @Test
    void methodRedefinition() {
        var exception = assertThrows(SemanticException.class, () -> execute("""
            class Foo {
                fun foo() {}
                fun foo() {}
            }
            """, null));
        assertThat(exception).hasMessage("function 'foo' already exists in class Foo");
    }

    @Test
    void fieldRedefinition() {
        var exception = assertThrows(SemanticException.class, () -> execute("""
            class Foo {
                def x: i32
                def x: i32
            }
            """, null));
        assertThat(exception).hasMessage("field 'x' already exists in class Foo");
    }

    @Test
    void createUndefinedClass() {
        var exception = assertThrows(SemanticException.class, () -> execute("""
            print (new X).toString();
            """, null));
        assertThat(exception).hasMessage("class 'X' is not defined");
    }

    @Test
    void callMethodFromMethod() {
        execute("""
            class Foo {
                fun foo() {
                    this.bar();
                }

                fun bar() {
                    print "hello";
                }
            }
            
            (new Foo).foo();
            """, "hello\n");
    }
}
