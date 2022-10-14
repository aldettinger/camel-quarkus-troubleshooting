package org.aldettinger.troubleshooting;

import java.lang.reflect.Field;

import javax.inject.Singleton;

import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import sun.misc.Unsafe;

@Singleton
@RegisterForReflection
public class MyBean {

    @ConfigProperty(name = "crash", defaultValue = "true")
    boolean crash;

    @Handler
    public String doIt(Exchange exchange) {
        new DoItEvent().commit();

        long counter = exchange.getProperty(Exchange.TIMER_COUNTER, Long.class);
        String body = exchange.getMessage().getBody(String.class);

        if (counter > 5 && crash) {
            crash();
        }

        return body + " :: MyBean counter " + counter;
    }

    private void crash() {
        Field theUnsafe = null;
        try {
            theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe unsafe = (Unsafe)theUnsafe.get(null);
            unsafe.copyMemory(0, 128, 256);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Label("Do It")
    @Description("Signal do it has been called")
    static class DoItEvent extends Event {
        @Label("Message")
        String message;
    }
    
}
