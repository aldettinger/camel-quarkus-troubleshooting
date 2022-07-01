package org.aldettinger.troubleshooting;

import org.apache.camel.builder.RouteBuilder;

public class MyRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("timer:test").log("Timer event has been fired");
    }

}
