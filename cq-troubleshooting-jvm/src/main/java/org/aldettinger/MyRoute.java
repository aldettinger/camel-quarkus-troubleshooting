package org.aldettinger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ValidationException;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class MyRoute extends RouteBuilder {

    @Inject
    CamelContext context;

    @ConfigProperty(name = "basic.message")
    static String USER_INFO; // Config is set AFTER object initialization

    @Override
    public void configure() {
        onException(ValidationException.class).log("@TODO: only one exception by design ? really ?");

        from("platform-http:/hello").setBody(constant(USER_INFO)).bean("myBean").log("${in.body}");
    }

}
