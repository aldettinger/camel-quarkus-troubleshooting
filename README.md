# camel-quarkus-troubleshooting

# Breakfix exercise (what scenario ?)

# A common maintenance use case exercise (update a certificate ?)

# Troubleshooting in native mode

In native mode, an executable targeting a specific operating system is built.
There is a first process assembling a command line to invoke the GraalVM native image tool.
Then, `native-image` is invoked.

Let's do a build with some interesting options:

```
mvn clean package -Dnative -Dquarkus.native.additional-build-args='--trace-class-initialization=org.aldettinger.troubleshooting.MyRoute -H:-OmitInlinedMethodDebugLineInfo' -Dquarkus.native.enable-reports -Dquarkus.native.debug.enabled
```

## Pass an option to the native-image tool

The native-image tool has a lot of [interesting options](https://www.graalvm.org/22.1/reference-manual/native-image/Options/#options-to-native-image-builder) that could be useful to investigate an issue.

Those options could be passed to `native-image` through quarkus using `-Dquarkus.native.additional-build-args`.

For instance, we have traced the class initialization of the `MyRoute` class with the option:

```
-Dquarkus.native.additional-build-args='--trace-class-initialization=org.aldettinger.troubleshooting.MyRoute'
```

In the console logs, the `native-image` command line has been impacted:

```
native-image ... --trace-class-initialization=org.aldettinger.troubleshooting.MyRoute ...
```

And we have a logs more related to class initialization traces:

```
# Printing 1 class initialization trace(s) of class(es) traced by TraceClassInitialization to: /home/agallice/dev/projects/camel-quarkus-troubleshooting/cq-troubleshooting-native/target/cq-troubleshooting-native-1.0.0-SNAPSHOT-native-image-source-jar/reports/traced_class_initialization_20220701_124538.txt
```

The reports contains the stack trace involved in `MyRoute` initialization:

```
org.aldettinger.troubleshooting.MyRoute
---------------------------------------------
        at org.aldettinger.troubleshooting.MyRoute.<clinit>(MyRoute.java)
        at jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance0(Unknown Source)
        at jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:62)
        at jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
        at java.lang.reflect.Constructor.newInstance(Constructor.java:490)
        at org.apache.camel.support.ObjectHelper.newInstance(ObjectHelper.java:393)
        at org.apache.camel.impl.engine.DefaultInjector.newInstance(DefaultInjector.java:70)
        at org.apache.camel.quarkus.main.CamelMainRecorder.addRoutesBuilder(CamelMainRecorder.java:60)
        at io.quarkus.deployment.steps.CamelMainProcessor$main2097889726.deploy_0(Unknown Source)
        at io.quarkus.deployment.steps.CamelMainProcessor$main2097889726.deploy(Unknown Source)
        at io.quarkus.runner.ApplicationImpl.<clinit>(Unknown Source)
```

We see that the `MyRoute` class initialization has been recorded at build time in `CamelMainRecorder` where a new instance has been created by reflection.

This is just an example, the bottom line being that we can pass options to `native-image` using `-Dquarkus.native.additional-build-args`.

## Extracting information from a native executable

Standard tools working with executable could be used. For instance `strings`.

What is the Java version run by the native executable ?
What is the GraalVM version ?
Is it Mandrel distribution ?

```
[main_upstream @ target]$ strings rest-to-nats-demo-1.0.0-SNAPSHOT-runner | grep core.VM
com.oracle.svm.core.VM=GraalVM 22.0.0.2 Java 11 CE
com.oracle.svm.core.VM.Java.Version=11.0.14
com.oracle.svm.core.VM.Target.Platform=org.graalvm.nativeimage.Platform$LINUX_AMD64

[27x_downstream @ nats]$ strings target/camel-quarkus-integration-test-nats-2.7.1.fuse-SNAPSHOT-runner | grep core.VM
com.oracle.svm.core.VM=GraalVM 21.3.2.0-Final Java 11 Mandrel Distribution
com.oracle.svm.core.VM.Java.Version=11.0.15
com.oracle.svm.core.VM.Target.Platform=org.graalvm.nativeimage.Platform$LINUX_AMD64
```

## Checking what Java code has been embedded inside the native executable

In native mode, we are able to print some native reports, we have done this in the command line by passing:

```
-Dquarkus.native.enable-reports
```

In the `native-image` logs, let's focus on those txt reports:

```
# Printing list of used methods to: /home/agallice/dev/projects/camel-quarkus-troubleshooting/cq-troubleshooting-native/target/cq-troubleshooting-native-1.0.0-SNAPSHOT-native-image-source-jar/reports/used_methods_cq-troubleshooting-native-1.0.0-SNAPSHOT-runner_20220701_151920.txt
# Printing list of used classes to: /home/agallice/dev/projects/camel-quarkus-troubleshooting/cq-troubleshooting-native/target/cq-troubleshooting-native-1.0.0-SNAPSHOT-native-image-source-jar/reports/used_classes_cq-troubleshooting-native-1.0.0-SNAPSHOT-runner_20220701_151921.txt
# Printing list of used packages to: /home/agallice/dev/projects/camel-quarkus-troubleshooting/cq-troubleshooting-native/target/cq-troubleshooting-native-1.0.0-SNAPSHOT-native-image-source-jar/reports/used_packages_cq-troubleshooting-native-1.0.0-SNAPSHOT-runner_20220701_151921.txt
```

And let's see that `UnusedClass` is not embedded whereas `MyRoute.unusedMethodIncludedInTheGraph` is embedded:

```
grep 'unusedMethodIncludedInTheGraph' target/cq-troubleshooting-native-1.0.0-SNAPSHOT-native-image-source-jar/reports/used_methods_cq-troubleshooting-native-1.0.0-SNAPSHOT-runner_20220701_151920.txt 
com.oracle.svm.core.reflect.ReflectionAccessorHolder.MyRoute_unusedMethodIncludedInTheGraph_06308d43e14803a0a755a556fd063f42941c9235(boolean, Object, Object[]):Object
org.aldettinger.troubleshooting.MyRoute.unusedMethodIncludedInTheGraph():void
# unusedMethodIncludedInTheGraph is embedded

grep Unused target/cq-troubleshooting-native-1.0.0-SNAPSHOT-native-image-source-jar/reports/used_classes_cq-troubleshooting-native-1.0.0-SNAPSHOT-runner_20220701_151921.txt 
# UnusedClass is not embedded
```

Let's remember that `MyRoute` class is instantiated by reflection, where by default Quarkus embed all methods.

## Debug with gdb

With a native executable, it's possible to use a debugger like gdb.
In the command line, we have included some debug information with options below:

```
-Dquarkus.native.debug.enabled
-Dquarkus.native.additional-build-args=-H:-OmitInlinedMethodDebugLineInfo
```

Then we can invoke gdb and let it know to run the application:

```
gdb target/cq-troubleshooting-native-1.0.0-SNAPSHOT-runner
run
```

Then, gdb will catch the segmentation fault:

```
Program received signal SIGSEGV, Segmentation fault.
[Switching to Thread 0x7fffedbff700 (LWP 7575)]
com.oracle.svm.core.UnmanagedMemoryUtil.copyLongsBackward(union org.graalvm.word.Pointer, union org.graalvm.word.Pointer, union org.graalvm.word.UnsignedWord)void ()
    at com/oracle/svm/core/UnmanagedMemoryUtil.java:169
169	            long l24 = src.readLong(24);
Missing separate debuginfos, use: debuginfo-install glibc-2.17-326.el7_9.x86_64 libgcc-4.8.5-44.el7.x86_64 sssd-client-1.16.5-10.el7_9.12.x86_64 zlib-1.2.7-20.el7_9.x86_64
```

And we can print the stack trace

```
(gdb) bt
#0  com.oracle.svm.core.UnmanagedMemoryUtil.copyLongsBackward(union org.graalvm.word.Pointer, union org.graalvm.word.Pointer, union org.graalvm.word.UnsignedWord)void ()
    at com/oracle/svm/core/UnmanagedMemoryUtil.java:169
#1  0x0000000000416174 in com.oracle.svm.core.UnmanagedMemoryUtil.copyBackward(union org.graalvm.word.Pointer, union org.graalvm.word.Pointer, union org.graalvm.word.UnsignedWord)void ()
    at com/oracle/svm/core/UnmanagedMemoryUtil.java:110
#2  0x0000000000416128 in com.oracle.svm.core.UnmanagedMemoryUtil.copy(union org.graalvm.word.Pointer, union org.graalvm.word.Pointer, union org.graalvm.word.UnsignedWord)void ()
    at com/oracle/svm/core/UnmanagedMemoryUtil.java:67
#3  0x00000000004112c0 in com.oracle.svm.core.JavaMemoryUtil.unsafeCopyMemory(java.lang.Object, long, java.lang.Object, long, long)void () at com/oracle/svm/core/JavaMemoryUtil.java:276
#4  0x0000000000c95efe in jdk.internal.misc.Unsafe.copyMemory(java.lang.Object, long, java.lang.Object, long, long)void () at jdk/internal/misc/Unsafe.java:784
#5  0x0000000000d206af in org.aldettinger.troubleshooting.MyBean.crash()void () at org/aldettinger/troubleshooting/MyBean.java:35
#6  0x0000000000d2099f in org.aldettinger.troubleshooting.MyBean.doIt(org.apache.camel.Exchange)java.lang.String () at org/aldettinger/troubleshooting/MyBean.java:23
#7  0x000000000055aaef in com.oracle.svm.core.reflect.ReflectionAccessorHolder.MyBean_doIt_6ecb99b7150ef769738c4cd2ca873f7deeb93faf(boolean, java.lang.Object, java.lang.Object[])java.lang.Object ()
    at com/oracle/svm/core/reflect/ReflectionAccessorHolder.java:1
#8  0x00000000005bdab2 in com.oracle.svm.core.reflect.SubstrateMethodAccessor.invoke(java.lang.Object, java.lang.Object[])java.lang.Object () at com/oracle/svm/core/reflect/SubstrateMethodAccessor.java:60
#9  0x000000000117d853 in org.apache.camel.support.ObjectHelper.invokeMethodSafe(java.lang.reflect.Method, java.lang.Object, java.lang.Object[])java.lang.Object ()
    at org/apache/camel/support/ObjectHelper.java:380
#10 0x0000000000d5aa5b in org.apache.camel.component.bean.MethodInfo.invoke(java.lang.reflect.Method, java.lang.Object, java.lang.Object[], org.apache.camel.Exchange)java.lang.Object ()
    at org/apache/camel/component/bean/MethodInfo.java:494
#11 0x0000000000d54e33 in org.apache.camel.component.bean.MethodInfo$1.doProceed(org.apache.camel.AsyncCallback)boolean () at org/apache/camel/component/bean/MethodInfo.java:316
#12 0x0000000000d556fd in org.apache.camel.component.bean.MethodInfo$1.proceed(org.apache.camel.AsyncCallback)boolean () at org/apache/camel/component/bean/MethodInfo.java:286
#13 0x0000000000d39170 in org.apache.camel.component.bean.AbstractBeanProcessor.process(org.apache.camel.Exchange, org.apache.camel.AsyncCallback)boolean ()
    at org/apache/camel/component/bean/AbstractBeanProcessor.java:146
#14 0x0000000000d50ea4 in org.apache.camel.component.bean.BeanProcessor.process(org.apache.camel.Exchange, org.apache.camel.AsyncCallback)boolean () at org/apache/camel/component/bean/BeanProcessor.java:81
#15 0x00000000010e5174 in org.apache.camel.processor.errorhandler.RedeliveryErrorHandler$SimpleTask.run()void () at org/apache/camel/processor/errorhandler/RedeliveryErrorHandler.java:471
#16 0x0000000000e89b28 in org.apache.camel.impl.engine.DefaultReactiveExecutor$Worker.schedule(java.lang.Runnable, boolean, boolean, boolean)void ()
    at org/apache/camel/impl/engine/DefaultReactiveExecutor.java:189
#17 0x0000000000e8ae27 in org.apache.camel.impl.engine.DefaultReactiveExecutor.scheduleMain(java.lang.Runnable)void () at org/apache/camel/impl/engine/DefaultReactiveExecutor.java:61
#18 0x00000000010981bd in org.apache.camel.processor.Pipeline.process(org.apache.camel.Exchange, org.apache.camel.AsyncCallback)boolean () at org/apache/camel/processor/Pipeline.java:184
#19 0x0000000000e5f9f2 in org.apache.camel.impl.engine.CamelInternalProcessor.process(org.apache.camel.Exchange, org.apache.camel.AsyncCallback)boolean ()
    at org/apache/camel/impl/engine/CamelInternalProcessor.java:399
#20 0x0000000000d8c44a in org.apache.camel.component.timer.TimerConsumer.sendTimerExchange(long)void () at org/apache/camel/component/timer/TimerConsumer.java:210
#21 0x0000000000d8aea9 in org.apache.camel.component.timer.TimerConsumer$1.run()void () at org/apache/camel/component/timer/TimerConsumer.java:76
#22 0x0000000000b011dc in java.util.TimerThread.mainLoop()void () at java/util/Timer.java:556
#23 0x0000000000b0157b in java.util.TimerThread.run()void () at java/util/Timer.java:506
#24 0x00000000005cb51c in com.oracle.svm.core.thread.PlatformThreads.threadStartRoutine(org.graalvm.nativeimage.ObjectHandle)void () at com/oracle/svm/core/thread/PlatformThreads.java:704
#25 0x00000000004c3541 in com.oracle.svm.core.posix.thread.PosixPlatformThreads.pthreadStartRoutine(com.oracle.svm.core.thread.PlatformThreads$ThreadStartData)org.graalvm.word.WordBase ()
    at com/oracle/svm/core/posix/thread/PosixPlatformThreads.java:202
#26 0x0000000000462ef9 in com.oracle.svm.core.code.IsolateEnterStub.PosixPlatformThreads_pthreadStartRoutine_38d96cbc1a188a6051c29be1299afe681d67942e(com.oracle.svm.core.thread.PlatformThreads$ThreadStartData)org.graalvm.word.WordBase () at com/oracle/svm/core/code/IsolateEnterStub.java:1
#27 0x00007ffff7bc6ea5 in start_thread () from /lib64/libpthread.so.0
#28 0x00007ffff70b7b0d in clone () from /lib64/libc.so.6
```

The method `MyBean.crash` seems to be responsible.

Setting a breakpoint in the crash method and stepping over each line:

```
gdb target/cq-troubleshooting-native-1.0.0-SNAPSHOT-runner
(gdb) info function .*crash.*
All functions matching regular expression ".*crash.*":

File com/oracle/svm/core/reflect/ReflectionAccessorHolder.java:
java.lang.Object *com.oracle.svm.core.reflect.ReflectionAccessorHolder.MyBean_crash_407cd4400c4a49b3c08111fb8f47c677c32b73d4(boolean, java.lang.Object, java.lang.Object[])java.lang.Object;

File org/aldettinger/troubleshooting/MyBean.java:
void org.aldettinger.troubleshooting.MyBean.crash()void;

(gdb) break org.aldettinger.troubleshooting.MyBean.crash
Breakpoint 1 at 0xd203e0: file org/aldettinger/troubleshooting/MyBean.java, line 30.

Breakpoint 1, org.aldettinger.troubleshooting.MyBean.crash()void () at org/aldettinger/troubleshooting/MyBean.java:30
30	        Field theUnsafe = null;
Missing separate debuginfos, use: debuginfo-install glibc-2.17-326.el7_9.x86_64 libgcc-4.8.5-44.el7.x86_64 sssd-client-1.16.5-10.el7_9.12.x86_64 zlib-1.2.7-20.el7_9.x86_64
(gdb) next
34	            Unsafe unsafe = (Unsafe)theUnsafe.get(null);
(gdb) next
32	            theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
(gdb) next
34	            Unsafe unsafe = (Unsafe)theUnsafe.get(null);
(gdb) next
35	            unsafe.copyMemory(0, 128, 256);
(gdb) next

Program received signal SIGSEGV, Segmentation fault.
com.oracle.svm.core.UnmanagedMemoryUtil.copyLongsBackward(org.graalvm.word.Pointer, org.graalvm.word.Pointer, org.graalvm.word.UnsignedWord)void () at com/oracle/svm/core/UnmanagedMemoryUtil.java:169
169	            long l24 = src.readLong(24);

	
```

## Debug from an IDE

@TODO

# Performance regression detection

There is a performance regression prototype located [here](https://github.com/aldettinger/cq-perf-sandbox):
 + A sample scenario: `from("platform-http:...").to("atlasmap:...")`
 + Compare mean throughput against a list of camel-quarkus versions
 + Support released versions, release candidate and SNAPSHOT versions

In 2.10.0-SNAPSHOT, native mode is around 10% slower compared to camel-quarkus 2.9.0

![Quarkus Start](images/native-perf-regression-with-quarkus-2.10.0.CR1.png)

Let's find in which commit the regression was introduced

```
# Build camel-quarkus BEFORE upgrade to quarkus 2.10.0.CR1
git checkout 45d4ae0681ed03ef59fbcb51aac7360ab23f9d82
mvn clean install -Dquickly

# We don't see any regression
java -jar target/quarkus-app/quarkus-run.jar -an 2.9.0 2.10.0-SNAPSHOT

# Build camel-quarkus AFTER upgrade to quarkus 2.10.0.CR1
git checkout 1c6dd77b1743321b0daf1d7bd8344dee2683cd1c
mvn clean install -Dquickly

# We see a regression
java -jar target/quarkus-app/quarkus-run.jar -an 2.9.0 2.10.0-SNAPSHOT
```

Finally, let's remind a few things:
 + upstream only
 + mean throughput based only
 + native works only with environment where quarkus container-build is possible
 + maybe the scenario can be updated when troubleshooting a specific performance issue

To further profile runtime behaviour with flame graph, Quarkus describes a [tip](https://quarkus.io/guides/native-reference#profiling).

# Other troubleshooting experience ?

A static initializer that is executed at build time leading to issue. With a SecureRandom ?
Precise that the stack trace is not always the right one => So tracing output.

With a stamp ?

# More links
 + https://quarkus.io/guides/native-reference

# TODO
+ Check https://quarkus.io/version/2.7/guides/native-reference more deeply
