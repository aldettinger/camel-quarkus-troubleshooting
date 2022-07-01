# camel-quarkus-troubleshooting

# Breakfix exercise (what scenario ?)

# A common maintenance use case exercise (update a certificate ?)

# Debugging in native mode ?

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
 + maybe the scenario can be updated when troubleshooting a specific performance issue

# Other troubleshooting experience ?

# TODO
+ Check https://quarkus.io/version/2.7/guides/native-reference more deeply
