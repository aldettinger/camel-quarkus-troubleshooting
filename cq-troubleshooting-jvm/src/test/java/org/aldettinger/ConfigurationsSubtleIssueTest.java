package org.aldettinger;

import javax.inject.Inject;

import org.apache.camel.CamelContext;
import org.apache.camel.component.jpa.JpaComponent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
public class ConfigurationsSubtleIssueTest {

    @Inject
    CamelContext context;

    @Test
    public void jpaComponentShouldBeConfiguredWithSharedEntityManagerSetToTrue() {
        JpaComponent jpaComponent = context.getComponent("jpa", JpaComponent.class);
        assertTrue(jpaComponent.isSharedEntityManager(), "JpaComponent should be configured with sharedEntityManager");
    }

    @Test
    public void jpaComponentShouldBeConfiguredWithATransactionManager() {
        JpaComponent jpaComponent = context.getComponent("jpa", JpaComponent.class);
        assertNotNull(jpaComponent.getTransactionManager());
    }

}
