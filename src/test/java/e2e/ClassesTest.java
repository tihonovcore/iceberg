package e2e;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

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
//            Arguments.of("""
//                class Foo {
//                    def x = 0
//                    def y: string
//
//                    fun sayFoo() {
//                        print "foo";
//                    }
//                }
//
//                print "hello";""", "hello\n")
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
}
