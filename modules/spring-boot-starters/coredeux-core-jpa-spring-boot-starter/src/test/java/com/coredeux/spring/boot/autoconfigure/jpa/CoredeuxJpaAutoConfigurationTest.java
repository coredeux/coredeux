package com.coredeux.spring.boot.autoconfigure.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.coredeux.core.jpa.service.impl.DefaultCoredeuxJpaDataAccessService;
import com.coredeux.core.jpa.service.impl.PostgresCoredeuxJpaDataAccessService;

class CoredeuxJpaAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CoredeuxJpaAutoConfiguration.class))
            .withBean(EntityManagerFactory.class, () -> mock(EntityManagerFactory.class))
            .withBean(EntityManager.class, () -> mock(EntityManager.class));

    @Test
    void registersJpaServices() {
        contextRunner.withPropertyValues("coredeux.jpa.enabled=true").run(context -> {
            assertThat(context).hasBean("defaultCoredeuxJpaDataAccessService");
            assertThat(context).hasBean("postgresCoredeuxJpaDataAccessService");
            assertThat(context.getBean("defaultCoredeuxJpaDataAccessService",
                    DefaultCoredeuxJpaDataAccessService.class)).isNotNull();
            assertThat(context.getBean("postgresCoredeuxJpaDataAccessService",
                    PostgresCoredeuxJpaDataAccessService.class)).isNotNull();
        });
    }

    @Test
    void doesNotRegisterJpaServicesWhenNotEnabled() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(DefaultCoredeuxJpaDataAccessService.class);
            assertThat(context).doesNotHaveBean(PostgresCoredeuxJpaDataAccessService.class);
        });
    }

    @Test
    void backsOffWhenJpaServicesAlreadyExist() {
        DefaultCoredeuxJpaDataAccessService customDefault = new DefaultCoredeuxJpaDataAccessService();
        PostgresCoredeuxJpaDataAccessService customPostgres = new PostgresCoredeuxJpaDataAccessService();

        contextRunner.withPropertyValues("coredeux.jpa.enabled=true")
                .withBean("defaultCoredeuxJpaDataAccessService", DefaultCoredeuxJpaDataAccessService.class,
                        () -> customDefault)
                .withBean("postgresCoredeuxJpaDataAccessService", PostgresCoredeuxJpaDataAccessService.class,
                        () -> customPostgres)
                .run(context -> {
                    assertThat(context.getBean("defaultCoredeuxJpaDataAccessService",
                            DefaultCoredeuxJpaDataAccessService.class)).isSameAs(customDefault);
                    assertThat(context.getBean("postgresCoredeuxJpaDataAccessService",
                            PostgresCoredeuxJpaDataAccessService.class)).isSameAs(customPostgres);
                });
    }
}
