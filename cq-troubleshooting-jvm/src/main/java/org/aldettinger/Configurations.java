package org.aldettinger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Named;

import org.apache.camel.component.jpa.JpaComponent;
import org.apache.camel.quarkus.core.events.ComponentAddEvent;

@ApplicationScoped
public class Configurations {
    /*
    public void onComponentAdd(@Observes ComponentAddEvent event) {
        // Better intercept the ComponentAddEvent
        if (event.getComponent() instanceof JpaComponent) {
            JpaComponent jpaComponent = ((JpaComponent) event.getComponent());
            jpaComponent.setSharedEntityManager(true);
        }
    }*/

    /*
    @Named("jpa")
    JpaComponent jpa() {
        // Producing camel component can lead to subtle issues
        JpaComponent component = new JpaComponent();
        component.setSharedEntityManager(true);
        return component;
    }*/
}
