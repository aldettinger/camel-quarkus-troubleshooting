package org.aldettinger.troubleshooting;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class MyRoute extends RouteBuilder {

    @Inject
    MyBean myBean;

    @Override
    public void configure() {
        from("timer:test").bean(myBean).log("${in.body}");
    }

    public void unusedMethodIncludedInTheGraph() {
        // Because MyRoute is registered for reflection
    }

}
