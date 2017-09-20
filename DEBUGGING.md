## Debugging

Currently Kotlin native compiler produces debug info compatible with DWARF 2 specification, so modern debugger tools could
perform following operations:
- breakpoints
- stepping
- inspection of type information
- (var) variable inspection

### Producing binaries with debug info with Kotlin/Native compiler

To produce binaries with the Kotlin/Native compiler it's sufficient to use ``-g`` option on the command line.<br/>
_Example:_

```
0:b-debugger-fixes:minamoto@unit-703(0)# cat - > hello.kt
fun main(args: Array<String>) {
  println("Hello world");
  println("I need your clothes, boots and your motocycle");
}
0:b-debugger-fixes:minamoto@unit-703(0)# dist/bin/konanc -g hello.kt -o terminator.kexe
KtFile: hello.kt
0:b-debugger-fixes:minamoto@unit-703(0)# lldb terminator.kexe
(lldb) target create "terminator.kexe"
Current executable set to 'terminator.kexe' (x86_64).
(lldb) b kfun:main(kotlin.Array<kotlin.String>)
Breakpoint 1: where = terminator.kexe`kfun:main(kotlin.Array<kotlin.String>) + 4 at hello.kt:2, address = 0x00000001000012e4
(lldb) r
Process 28473 launched: '/Users/minamoto/ws/.git-trees/debugger-fixes/terminator.kexe' (x86_64)
Process 28473 stopped
* thread #1, queue = 'com.apple.main-thread', stop reason = breakpoint 1.1
    frame #0: 0x00000001000012e4 terminator.kexe`kfun:main(kotlin.Array<kotlin.String>) at hello.kt:2
   1    fun main(args: Array<String>) {
-> 2      println("Hello world");
   3      println("I need your clothes, boots and your motocycle");
   4    }
(lldb) n
Hello world
Process 28473 stopped
* thread #1, queue = 'com.apple.main-thread', stop reason = step over
    frame #0: 0x00000001000012f0 terminator.kexe`kfun:main(kotlin.Array<kotlin.String>) at hello.kt:3
   1    fun main(args: Array<String>) {
   2      println("Hello world");
-> 3      println("I need your clothes, boots and your motocycle");
   4    }
(lldb)
```

### Breakpoints
Modern debuggers provide several ways to set breakpoint, see below for per-tool breakdown:

#### lldb
- by name
````
(lldb) b -n kfun:main(kotlin.Array<kotlin.String>)
Breakpoint 4: where = terminator.kexe`kfun:main(kotlin.Array<kotlin.String>) + 4 at hello.kt:2, address = 0x00000001000012e4
````
 _``-n`` is optional, this flag is applied by default_
- by location (filename, line number)
````
(lldb) b -f hello.kt -l 1
Breakpoint 1: where = terminator.kexe`kfun:main(kotlin.Array<kotlin.String>) + 4 at hello.kt:2, address = 0x00000001000012e4
````
- by address
````
(lldb) b -a 0x00000001000012e4
Breakpoint 2: address = 0x00000001000012e4
````
- by regex, ones might find it useful for debugging generated artifacts, like lambda etc (where used ``#`` symbol in name).
````
3: regex = 'main\(', locations = 1
  3.1: where = terminator.kexe`kfun:main(kotlin.Array<kotlin.String>) + 4 at hello.kt:2, address = terminator.kexe[0x00000001000012e4], unresolved, hit count = 0
````
#### gdb
- by regex
````
(gdb) rbreak main(
Breakpoint 1 at 0x1000109b4
struct ktype:kotlin.Unit &kfun:main(kotlin.Array<kotlin.String>);
````
- by name __unusable__, because ``:`` is separator for breakpoint by location

``
(gdb) b kfun:main(kotlin.Array<kotlin.String>)
No source file named kfun.
Make breakpoint pending on future shared library load? (y or [n]) y
Breakpoint 1 (kfun:main(kotlin.Array<kotlin.String>)) pending
``
- by location
````
 (gdb) b hello.kt:1
 Breakpoint 2 at 0x100001704: file /Users/minamoto/ws/.git-trees/hello.kt, line 1.
````
- by address
````
(gdb) b *0x100001704
Note: breakpoint 2 also set at pc 0x100001704.
Breakpoint 3 at 0x100001704: file /Users/minamoto/ws/.git-trees/hello.kt, line 2.
````

### Stepping
Stepping functions mostly the same way as for C/C++ programs

### Type info
Some details about type information functionality in modern debuggers:

#### lldb
````
0:b-debugger-fixes:minamoto@unit-703(1)# lldb build/bin/Tetris.kexe
(lldb) target create "build/bin/Tetris.kexe"
Current executable set to 'build/bin/Tetris.kexe' (x86_64).
(lldb) b kfun:main(kotlin.Array<kotlin.String>)
Breakpoint 1: where = Tetris.kexe`kfun:main(kotlin.Array<kotlin.String>) + 21 at Tetris.kt:955, address = 0x00000001000109c5
(lldb) r
Process 41740 launched: '/Users/minamoto/ws/.git-trees/debugger-fixes/samples/tetris/build/bin/Tetris.kexe' (x86_64)
Process 41740 stopped
* thread #1, queue = 'com.apple.main-thread', stop reason = breakpoint 1.1
    frame #0: 0x00000001000109c5 Tetris.kexe`kfun:main(kotlin.Array<kotlin.String>) at Tetris.kt:955
   952      }
   953  }
   954
-> 955  fun main(args: Array<String>) {
   956      var startLevel = 0
   957      var width = 10
   958      var height = 20
(lldb) type lookup ktype:Point
struct ktype:Point {
    int kprop:Point.x;
    int kprop:Point.y;
}
````

#### gdb
Unfortunately ``ptype`` is affected with ``:`` and unusable.

### (var) Variable inspection

Variable inspections for var variables works out of the box for primitive types.
For non-primitive types there are custom pretty printers for lldb in
`konan_lldb.py`:

```
λ cat main.kt | nl
     1  fun main(args: Array<String>) {
     2      var x = 1
     3      var y = 2
     4      var p = Point(x, y)
     5      println("p = $p")
     6  }
       
     7  data class Point(val x: Int, val y: Int)

λ lldb ./program.kexe -o 'b main.kt:5' -o
(lldb) target create "./program.kexe"
Current executable set to './program.kexe' (x86_64).
(lldb) b main.kt:5
Breakpoint 1: where = program.kexe`kfun:main(kotlin.Array<kotlin.String>) + 289 at main.kt:5, address = 0x000000000040af11
(lldb) r
Process 4985 stopped
* thread #1, name = 'program.kexe', stop reason = breakpoint 1.1
    frame #0: program.kexe`kfun:main(kotlin.Array<kotlin.String>) at main.kt:5
   2        var x = 1
   3        var y = 2
   4        var p = Point(x, y)
-> 5        println("p = $p")
   6    }
   7   
   8    data class Point(val x: Int, val y: Int)

Process 4985 launched: './program.kexe' (x86_64)
(lldb) fr var
(int) x = 1
(int) y = 2
(ObjHeader *) p = 0x00000000007643d8
(lldb) command script import dist/tools/konan_lldb.py
(lldb) fr var
(int) x = 1
(int) y = 2
(ObjHeader *) p = Point(x=1, y=2)
(lldb) p p
(ObjHeader *) $2 = Point(x=1, y=2)
(lldb) 
```

Getting representation of the object variable (var) could also be done using
builtin runtime function `Konan_DebugPrint` (this approach also works for gdb,
by module of command syntax):

````
0:b-debugger-fixes:minamoto@unit-703(0)# cat ../debugger-plugin/1.kt | nl -p
     1  fun foo(a:String, b:Int) = a + b
     2  fun one() = 1
     3  fun main(arg:Array<String>) {
     4    var a_variable = foo("(a_variable) one is ", 1)
     5    var b_variable = foo("(b_variable) two is ", 2)
     6    var c_variable = foo("(c_variable) two is ", 3)
     7    var d_variable = foo("(d_variable) two is ", 4)
     8    println(a_variable)
     9    println(b_variable)
    10    println(c_variable)
    11    println(d_variable)
    12  }
0:b-debugger-fixes:minamoto@unit-703(0)# lldb ./program.kexe -o 'b -f 1.kt -l 9' -o r
(lldb) target create "./program.kexe"
Current executable set to './program.kexe' (x86_64).
(lldb) b -f 1.kt -l 9
Breakpoint 1: where = program.kexe`kfun:main(kotlin.Array<kotlin.String>) + 463 at 1.kt:9, address = 0x0000000100000dbf
(lldb) r
(a_variable) one is 1
Process 80496 stopped
* thread #1, queue = 'com.apple.main-thread', stop reason = breakpoint 1.1
    frame #0: 0x0000000100000dbf program.kexe`kfun:main(kotlin.Array<kotlin.String>) at 1.kt:9
   6      var c_variable = foo("(c_variable) two is ", 3)
   7      var d_variable = foo("(d_variable) two is ", 4)
   8      println(a_variable)
-> 9      println(b_variable)
   10     println(c_variable)
   11     println(d_variable)
   12   }

Process 80496 launched: './program.kexe' (x86_64)
(lldb) expression -- Konan_DebugPrint(a_variable)
(a_variable) one is 1(KInt) $0 = 0
(lldb)
````


### Known issues
- stepping in imported inline functions 
- inspection is not implemented for (val) variables.

_Note:_ Support DWARF 2 specification means that debugger tool recognize Kotlin as C89, because till DWARF 5 specification, there is no
identifier for Kotlin language type in specification.

