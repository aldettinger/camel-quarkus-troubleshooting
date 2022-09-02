package org.aldettinger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.jboss.logging.Logger;

@ApplicationScoped
@Named("myBean")
public class MyBean {

    private static final Logger LOG = Logger.getLogger(MyBean.class);

    @Handler
    public String doIt(Exchange exchange) {
        LOG.info("MyBean.doIt is called and I show a log with INFO level");
        LOG.debug("MyBean.doIt is called and I show a log with DEBUG level");

        String body = exchange.getMessage().getBody(String.class);

        if(body == null) {
            throw new RuntimeException("Body is null");
        }

        return body + " :: MyBean";
    }

}
