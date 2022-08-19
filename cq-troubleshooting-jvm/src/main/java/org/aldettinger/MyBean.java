package org.aldettinger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.apache.camel.Exchange;
import org.apache.camel.Handler;

@ApplicationScoped
@Named("myBean")
public class MyBean {

    @Handler
    public String doIt(Exchange exchange) {
        String body = exchange.getMessage().getBody(String.class);

        if(body == null) {
            throw new RuntimeException("Body is null");
        }

        return body + " :: MyBean ";
    }

}
