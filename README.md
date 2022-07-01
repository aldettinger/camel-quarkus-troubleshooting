# camel-quarkus-troubleshooting

# Breakfix exercise (what scenario ?)

# A common maintenance use case exercise (update a certificate ?)

# Troubleshooting in native mode

In native mode, an executable targeting a specific operating system is built.
There is a first process assembling a command line to invoke the GraalVM native image tool.
Then, `native-image` is invoked.

## Pass an option to the native-image tool

The native-image tool has a lot of [interesting options](https://www.graalvm.org/22.1/reference-manual/native-image/Options/#options-to-native-image-builder) that could be useful to investigate an issue.

Those options could be passed to `native-image` through quarkus using `-Dquarkus.native.additional-build-args`.

For instance, let's trace the class initialization of the `MyRoute` class:

```
mvn clean package -Dnative -Dquarkus.native.additional-build-args='--trace-class-initialization=org.aldettinger.troubleshooting.MyRoute'
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

We see that the class initialization has been recorded at build time in `CamelMainRecorder` where a new instance has been created by reflection.

This is just an example, the bottom line being that we can pass options to `native-image`.

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

# Performance troubleshooting

There is a performance regression prototype located [here](https://github.com/aldettinger/cq-perf-sandbox):
 + A sample scenario: `from("platform-http:...").to("atlasmap:...")`
 + Compare mean throughput against a list of camel-quarkus versions
 + Support released versions, release candidate and SNAPSHOT versions

## In 2.10.0-SNAPSHOT, native mode is around 10% slower compared to camel-quarkus 2.9.0

![Quarkus Start](images/native-perf-regression-with-quarkus-2.10.0.CR1.png)

## Let's find in which commit the regression was introduced

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

## Conclusion

Let's remind a few things:
 + upstream only
 + mean throughput based only
 + native works only with environment where quarkus container-build is possible
 + maybe the scenario can be updated when troubleshooting a specific performance issue

# Other troubleshooting experience ?

# TODO
+ Check https://quarkus.io/version/2.7/guides/native-reference more deeply
