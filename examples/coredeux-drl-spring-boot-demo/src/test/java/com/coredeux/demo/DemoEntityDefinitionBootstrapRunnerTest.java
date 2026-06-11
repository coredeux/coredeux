package com.coredeux.demo;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import com.coredeux.demo.definition.EntityDefinitionManager;

class DemoEntityDefinitionBootstrapRunnerTest {

    @Test
    void delegatesBootstrapToTheManager() {
        EntityDefinitionManager manager = mock(EntityDefinitionManager.class);
        DemoEntityDefinitionBootstrapRunner runner = new DemoEntityDefinitionBootstrapRunner(manager);

        runner.run();

        verify(manager).updateEntityDefinitionFromFile();
    }
}
