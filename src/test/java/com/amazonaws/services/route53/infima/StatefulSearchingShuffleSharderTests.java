/**
 * Copyright 2013-2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazonaws.services.route53.infima;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.amazonaws.services.route53.infima.StatefulSearchingShuffleSharder.FragmentStore;
import com.amazonaws.services.route53.infima.StatefulSearchingShuffleSharder.NoShardsAvailableException;

public class StatefulSearchingShuffleSharderTests {

    private class MockFragmentStore implements FragmentStore<String> {
        private final HashSet<String> store = new HashSet<String>();

        @Override
        public void saveFragment(List<String> fragment) {
            Collections.sort(fragment);
            store.add(fragment.toString());
        }

        @Override
        public boolean isFragmentUsed(List<String> fragment) {
            Collections.sort(fragment);
            return store.contains(fragment.toString());
        }
    }

    @Test
    public void overlapStatefulSearchingShuffleSharderTest() {
        /* Use a single cell lattice with 20 endpoints for a very simple test */
        String[] endpoints = new String[] { "A", "B", "C", "D", "E" };
        SingleCellLattice<String> lattice = new SingleCellLattice<String>();
        lattice.addEndpoints(Arrays.asList(endpoints));

        MockFragmentStore mockStore = new MockFragmentStore();
        StatefulSearchingShuffleSharder<String> sharder = new StatefulSearchingShuffleSharder<String>(mockStore);

        for (int i = 0; i < 2; i++) {
            try {
                sharder.shuffleShard(lattice, 4, 2);
                if (i == 1) {
                    fail("Only 1 valid shard");
                }
            } catch (Exception e) {
                if (i != 1) {
                    e.printStackTrace();
                    fail();
                }
            }
        }
    }

    @Test
    public void singleCellStatefulSearchingShuffleSharderTest() throws NoShardsAvailableException {
        /* Use a single cell lattice with 20 endpoints for a very simple test */
        String[] endpoints = new String[] { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O",
                "P", "Q", "R", "S", "T" };

        SingleCellLattice<String> lattice = new SingleCellLattice<String>();
        lattice.addEndpoints(Arrays.asList(endpoints));

        MockFragmentStore mockFragmentStore = new MockFragmentStore();
        StatefulSearchingShuffleSharder<String> sharder = new StatefulSearchingShuffleSharder<String>(mockFragmentStore);

        /*
         * Compute 100 different shards and count how often each letter is
         * observed
         */
        Map<String, Integer> countByLetter = new HashMap<String, Integer>();
        for (int i = 0; i < 100; i++) {
            Lattice<String> shard = sharder.shuffleShard(lattice, 4, 2);

            /*
             * Check that each shard has 4 endpoints and is itself a single-cell
             * lattice
             */
            assertEquals(4, shard.getAllEndpoints().size());
            assertEquals(1, shard.getDimensionality().size());
            assertEquals(1, shard.getAllCoordinates().size());

            for (String letter : shard.getAllEndpoints()) {
                if (countByLetter.containsKey(letter)) {
                    countByLetter.put(letter, countByLetter.get(letter) + 1);
                } else {
                    countByLetter.put(letter, 0);
                }
            }
        }

        /* Check that all 20 letters were seen */
        assertEquals(20, countByLetter.keySet().size());
    }

    @Test
    public void oneDimensionalSimpleSignatureShuffleSharderTest() throws NoShardsAvailableException {
        /* Use a 1-D lattice with 20 endpoints for a very simple test */
        String[] endpointsA = new String[] { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J" };
        String[] endpointsB = new String[] { "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T" };

        OneDimensionalLattice<String> lattice = new OneDimensionalLattice<String>("AZ");
        lattice.addEndpoints("us-east-1a", Arrays.asList(endpointsA));
        lattice.addEndpoints("us-east-1b", Arrays.asList(endpointsB));

        StatefulSearchingShuffleSharder<String> sharder = new StatefulSearchingShuffleSharder<String>(
                new MockFragmentStore());

        /*
         * Compute all 100 different shards and count how often each letter is
         * observed
         */
        Map<String, Integer> countByLetter = new HashMap<String, Integer>();
        for (int i = 0; i < 45; i++) {
            ;
            Lattice<String> shard = sharder.shuffleShard(lattice, 2, 2);

            /*
             * Check that each shard has 4 endpoints and is itself a 2 cell 1-D
             * lattice
             */
            assertEquals(4, shard.getAllEndpoints().size());
            assertEquals(1, shard.getDimensionality().size());
            assertEquals(2, shard.getAllCoordinates().size());

            for (String letter : shard.getAllEndpoints()) {
                if (countByLetter.containsKey(letter)) {
                    countByLetter.put(letter, countByLetter.get(letter) + 1);
                } else {
                    countByLetter.put(letter, 0);
                }
            }

            /* Confirm that endpoints stay in their own cells */
            for (String letter : shard.getEndpointsForSector(Arrays.asList("us-east-1a"))) {
                assertEquals(true, Arrays.asList(endpointsA).contains(letter));
            }

            for (String letter : shard.getEndpointsForSector(Arrays.asList("us-east-1b"))) {
                assertEquals(true, Arrays.asList(endpointsB).contains(letter));
            }
        }

        /* Check that all 20 letters were seen */
        assertEquals(20, countByLetter.keySet().size());
    }

    @Test
    public void twoDimensionalSimpleSignatureShuffleSharderTest() throws NoShardsAvailableException {

        /* Use a 2-D lattice with 20 endpoints for a very simple test */
        String[] endpointsA1 = new String[] { "A", "B", "C", "D", "E" };
        String[] endpointsA2 = new String[] { "F", "G", "H", "I", "J" };
        String[] endpointsB1 = new String[] { "K", "L", "M", "N", "O" };
        String[] endpointsB2 = new String[] { "P", "Q", "R", "S", "T" };

        TwoDimensionalLattice<String> lattice = new TwoDimensionalLattice<String>("AZ", "Version");
        lattice.addEndpoints("us-east-1a", "1", Arrays.asList(endpointsA1));
        lattice.addEndpoints("us-east-1a", "2", Arrays.asList(endpointsA2));
        lattice.addEndpoints("us-east-1b", "1", Arrays.asList(endpointsB1));
        lattice.addEndpoints("us-east-1b", "2", Arrays.asList(endpointsB2));

        StatefulSearchingShuffleSharder<String> sharder = new StatefulSearchingShuffleSharder<String>(
                new MockFragmentStore());

        Map<String, Integer> countByLetter = new HashMap<String, Integer>();
        for (int i = 0; i < 20; i++) {
            Lattice<String> shard = sharder.shuffleShard(lattice, 2, 2);

            /*
             * Check that each shard has 4 endpoints and is itself a 4 cell 2-D
             * lattice
             */
            assertEquals(4, shard.getAllEndpoints().size());
            assertEquals(2, shard.getDimensionality().size());
            assertEquals(2, shard.getAllCoordinates().size());

            for (String letter : shard.getAllEndpoints()) {
                if (countByLetter.containsKey(letter)) {
                    countByLetter.put(letter, countByLetter.get(letter) + 1);
                } else {
                    countByLetter.put(letter, 0);
                }
            }

            /* Confirm that endpoints stay in their own cells */
            if (shard.getEndpointsForSector(Arrays.asList("us-east-1a", "1")) != null) {
                for (String letter : shard.getEndpointsForSector(Arrays.asList("us-east-1a", "1"))) {
                    assertEquals(true, Arrays.asList(endpointsA1).contains(letter));
                }
            }
            if (shard.getEndpointsForSector(Arrays.asList("us-east-1a", "2")) != null) {
                for (String letter : shard.getEndpointsForSector(Arrays.asList("us-east-1a", "2"))) {
                    assertEquals(true, Arrays.asList(endpointsA2).contains(letter));
                }
            }
            if (shard.getEndpointsForSector(Arrays.asList("us-east-1b", "1")) != null) {
                for (String letter : shard.getEndpointsForSector(Arrays.asList("us-east-1b", "1"))) {
                    assertEquals(true, Arrays.asList(endpointsB1).contains(letter));
                }
            }
            if (shard.getEndpointsForSector(Arrays.asList("us-east-1b", "2")) != null) {
                for (String letter : shard.getEndpointsForSector(Arrays.asList("us-east-1b", "2"))) {
                    assertEquals(true, Arrays.asList(endpointsB2).contains(letter));
                }
            }
        }

        /* Check that all 20 letters were seen */
        assertEquals(20, countByLetter.keySet().size());
    }
}
