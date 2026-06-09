package com.coredeux.drl.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConvertedDrlTest {

    @Test
    void exposesRuleIdAndDrlText() {
        ConvertedDrl converted = new ConvertedDrl("sample-rule", "rule \"sample-rule\"");

        assertEquals("sample-rule", converted.getRuleId());
        assertEquals("rule \"sample-rule\"", converted.getDrl());
    }
}
