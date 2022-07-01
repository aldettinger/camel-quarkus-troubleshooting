package org.aldettinger.troubleshooting;

import java.lang.reflect.Field;

import javax.inject.Singleton;

import org.apache.camel.Exchange;
import org.apache.camel.Handler;

import io.quarkus.runtime.annotations.RegisterForReflection;
import sun.misc.Unsafe;

@Singleton
@RegisterForReflection
public class MyBean {

    @Handler
    public String doIt(Exchange exchange) {
        long counter = exchange.getProperty(Exchange.TIMER_COUNTER, Long.class);
        String body = exchange.getMessage().getBody(String.class);

        if (counter > 5) {
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

}
