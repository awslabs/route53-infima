/**
 * Copyright 2013-2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazonaws.services.route53.infima;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.amazonaws.services.route53.infima.util.IterableSubListGenerator;

public class SubListIteratorTests {

    @Test
    public void fiveChooseThreeTest() {
        List<String> letters = Arrays.asList("A", "B", "C", "D", "E");
        IterableSubListGenerator<String> generator = new IterableSubListGenerator<String>(letters, 3);
        List<String> expectedSubLists = Arrays.asList("[A, B, C]", "[A, B, D]", "[A, B, E]", "[A, C, D]", "[A, C, E]",
                "[A, D, E]", "[B, C, D]", "[B, C, E]", "[B, D, E]", "[C, D, E]");

        int i = 0;
        for (List<String> subList : generator) {
            assertEquals(expectedSubLists.get(i++), subList.toString());
        }
    }

    @Test
    public void twentyChooseFourTest() {
        List<String> letters = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O",
                "P", "Q", "R", "S", "T");
        IterableSubListGenerator<String> generator = new IterableSubListGenerator<String>(letters, 4);

        int i = 0;
        for (List<String> subList : generator) {
            assertEquals(4, subList.size());
            i++;
        }

        assertEquals((20 * 19 * 18 * 17) / (4 * 3 * 2 * 1), i);
    }

    @Test
    public void twentyChooseOneTest() {
        List<String> letters = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O",
                "P", "Q", "R", "S", "T");
        IterableSubListGenerator<String> generator = new IterableSubListGenerator<String>(letters, 1);

        int i = 0;
        for (List<String> subList : generator) {
            assertEquals(1, subList.size());
            i++;
        }

        assertEquals(20, i);
    }

}
