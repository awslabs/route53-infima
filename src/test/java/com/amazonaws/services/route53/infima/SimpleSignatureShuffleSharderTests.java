/**
 * Copyright 2013-2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazonaws.services.route53.infima;

import static org.junit.Assert.*;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class SimpleSignatureShuffleSharderTests {

    @Test
    public void singleCellSimpleSignatureShuffleSharderTest() throws NoSuchAlgorithmException {
        /* Use a single cell lattice with 20 endpoints for a very simple test */
        String[] endpoints = new String[] { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O",
                "P", "Q", "R", "S", "T" };

        SingleCellLattice<String> lattice = new SingleCellLattice<String>();
        lattice.addEndpoints(Arrays.asList(endpoints));

        SimpleSignatureShuffleSharder<String> sharder = new SimpleSignatureShuffleSharder<String>(5353L);

        /*
         * Compute 1,000 different shards and count how often each letter is
         * observed
         */
        Map<String, Integer> countByLetter = new HashMap<String, Integer>();
        for (int i = 0; i < 10000; i++) {
            Lattice<String> shard = sharder.shuffleShard(lattice, new Integer(i).toString().getBytes(), 4);

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

        /*
         * We computed 10,000 shards with 4 IPs each. And there are 20 IPs total
         * so each is expected to be seen 40,000 / 20 == 2,000.
         * 
         * Check that we're within 10% for each letter.
         */
        for (String letter : countByLetter.keySet()) {
            assertEquals(1.0, countByLetter.get(letter) / 2000.0, 0.1);
        }
    }

    @Test
    public void oneDimensionalSimpleSignatureShuffleSharderTest() throws NoSuchAlgorithmException {
        /* Use a 1-D lattice with 20 endpoints for a very simple test */
        String[] endpointsA = new String[] { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J" };
        String[] endpointsB = new String[] { "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T" };

        OneDimensionalLattice<String> lattice = new OneDimensionalLattice<String>("AZ");
        lattice.addEndpoints("us-east-1a", Arrays.asList(endpointsA));
        lattice.addEndpoints("us-east-1b", Arrays.asList(endpointsB));

        SimpleSignatureShuffleSharder<String> sharder = new SimpleSignatureShuffleSharder<String>(5353L);

        /*
         * Compute 1,000 different shards and count how often each letter is
         * observed
         */
        Map<String, Integer> countByLetter = new HashMap<String, Integer>();
        for (int i = 0; i < 100000; i++) {
            Lattice<String> shard = sharder.shuffleShard(lattice, new Integer(i).toString().getBytes(), 2);

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
        
        /*
         * We computed 100,000 shards with 4 IPs each. And there are 20 IPs total
         * so each is expected to be seen 400,000 / 20 == 20,000.
         * 
         * Check that we're within 10% for each letter.
         */
        for (String letter : countByLetter.keySet()) {
            assertEquals(1.0, countByLetter.get(letter) / 20000.0, 0.1);
        }
    }

    @Test
    public void twoDimensionalSimpleSignatureShuffleSharderTest() throws NoSuchAlgorithmException {

        /* Use a 1-D lattice with 20 endpoints for a very simple test */
        String[] endpointsA1 = new String[] { "A", "B", "C", "D", "E" };
        String[] endpointsA2 = new String[] { "F", "G", "H", "I", "J" };
        String[] endpointsB1 = new String[] { "K", "L", "M", "N", "O" };
        String[] endpointsB2 = new String[] { "P", "Q", "R", "S", "T" };

        TwoDimensionalLattice<String> lattice = new TwoDimensionalLattice<String>("AZ", "Version");
        lattice.addEndpoints("us-east-1a", "1", Arrays.asList(endpointsA1));
        lattice.addEndpoints("us-east-1a", "2", Arrays.asList(endpointsA2));
        lattice.addEndpoints("us-east-1b", "1", Arrays.asList(endpointsB1));
        lattice.addEndpoints("us-east-1b", "2", Arrays.asList(endpointsB2));

        SimpleSignatureShuffleSharder<String> sharder = new SimpleSignatureShuffleSharder<String>(5353L);

        /*
         * Compute 1,000 different shards and count how often each letter is
         * observed
         */
        Map<String, Integer> countByLetter = new HashMap<String, Integer>();
        for (int i = 0; i < 10000; i++) {
            Lattice<String> shard = sharder.shuffleShard(lattice, new Integer(i).toString().getBytes(), 2);

            /*
             * Check that each shard has 4 endpoints and is itself a 4 cell 1-D
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

        /*
         * We computed 10,000 shards with 4 IPs each. And there are 20 IPs total
         * so each is expected to be seen 40,000 / 20 == 2,000.
         * 
         * Check that we're within 10% for each letter.
         */
        for (String letter : countByLetter.keySet()) {
            assertEquals(1.0, countByLetter.get(letter) / 2000.0, 0.1);
        }
    }

    @Test
    public void asymmetricalTwoDimensionalSimpleSignatureShuffleSharderTest() throws NoSuchAlgorithmException {

        /* Use a 1-D lattice with 20 endpoints for a very simple test */
        String[] endpointsA1 = new String[] { "A", "B", "C", "D" };
        String[] endpointsA2 = new String[] { "E", "F", "G", "H" };
        String[] endpointsA3 = new String[] { "I", "J", "K", "L" };
        String[] endpointsB1 = new String[] { "M", "N", "O", "P" };
        String[] endpointsB2 = new String[] { "Q", "R", "S", "T" };
        String[] endpointsB3 = new String[] { "U", "V", "W", "X" };

        TwoDimensionalLattice<String> lattice = new TwoDimensionalLattice<String>("AZ", "Version");
        lattice.addEndpoints("us-east-1a", "1", Arrays.asList(endpointsA1));
        lattice.addEndpoints("us-east-1a", "2", Arrays.asList(endpointsA2));
        lattice.addEndpoints("us-east-1a", "3", Arrays.asList(endpointsA3));
        lattice.addEndpoints("us-east-1b", "1", Arrays.asList(endpointsB1));
        lattice.addEndpoints("us-east-1b", "2", Arrays.asList(endpointsB2));
        lattice.addEndpoints("us-east-1b", "3", Arrays.asList(endpointsB3));

        SimpleSignatureShuffleSharder<String> sharder = new SimpleSignatureShuffleSharder<String>(5353L);

        /*
         * Compute 1,000 different shards and count how often each letter is
         * observed
         */
        Map<String, Integer> countByLetter = new HashMap<String, Integer>();
        for (int i = 0; i < 10000; i++) {
            Lattice<String> shard = sharder.shuffleShard(lattice, new Integer(i).toString().getBytes(), 2);

            /*
             * Check that each shard has 4 endpoints and is itself a 4 cell 1-D
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
            if (shard.getEndpointsForSector(Arrays.asList("us-east-1a", "3")) != null) {
                for (String letter : shard.getEndpointsForSector(Arrays.asList("us-east-1a", "3"))) {
                    assertEquals(true, Arrays.asList(endpointsA3).contains(letter));
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
            if (shard.getEndpointsForSector(Arrays.asList("us-east-1b", "3")) != null) {
                for (String letter : shard.getEndpointsForSector(Arrays.asList("us-east-1b", "3"))) {
                    assertEquals(true, Arrays.asList(endpointsB3).contains(letter));
                }
            }
        }

        /* Check that all 24 letters were seen */
        assertEquals(24, countByLetter.keySet().size());

        /*
         * We computed 10,000 shards with 4 IPs each. And there are 24 IPs total
         * so each is expected to be seen 40,000 / 20 == 1,666.
         * 
         * Check that we're within 10% for each letter.
         */
        for (String letter : countByLetter.keySet()) {
            assertEquals(1.0, countByLetter.get(letter) / 1666.0, 0.1);
        }
    }
}
