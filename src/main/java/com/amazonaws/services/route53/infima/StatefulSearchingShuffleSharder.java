/**
 * Copyright 2013-2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazonaws.services.route53.infima;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.amazonaws.services.route53.infima.util.IterableSubListGenerator;

/**
 * A shuffle sharder based on stateful searching.
 * 
 * In traditional sharding, an identifier is sharded to one item out of many. If
 * that item is something that may fail or become contended in some way (e.g. a
 * server, a queue, a rate-limit), then traditional sharding reduces the
 * "blast radius" of any per-identifier problem or burst to a factor of 1/N of
 * the overall number of items.
 * 
 * With Shuffle Sharding we assign each identifier multiple endpoints. If the
 * dependent calling client is tolerant of partial availability, or uses Route
 * 53 healthchecks with a Rubber Tree for endpoint discovery (which is itself
 * tolerant of partial availability), the "blast radius" of any problem is
 * reduced to a factor of 1/(N choose K) where K is the number of items in the
 * shuffle shard.
 * 
 * This shuffle shard implementation uses a datastore to perform a stateful
 * search for a new shuffle shard, with guarantees about the number of items
 * that may overlap between the new shuffle shard and all existing recorded
 * shuffle shards.
 * 
 * @param <T>
 *            The type for items in the shuffle shards
 */
public class StatefulSearchingShuffleSharder<T> {

    @SuppressWarnings("serial")
    public static class NoShardsAvailableException extends Exception {
    };

    /**
     * API for a List-based fragment store
     * 
     * @param <T>
     *            The type for the fragment list entries
     */
    public interface FragmentStore<T> {
        /**
         * Save a fragment.
         * 
         * @param fragment
         *            The fragment to be saved
         */
        public void saveFragment(List<T> fragment);

        /**
         * Has a fragment been used?
         * 
         * @param fragment
         *            The fragment we are curious about
         * @return has the fragment been used
         */
        public boolean isFragmentUsed(List<T> fragment);
    }

    private final FragmentStore<T> store;

    /**
     * Create a Stateful Searching Shuffle Sharder. This shuffle sharder allows
     * you to create shuffle shards while preserving guarantees about the
     * maximum amount of overlap between any two shards. For example with 5
     * endpoints; [ "A", "B", "C", "D", "E" ] the first computed shard of size 3
     * may be [ "A", "B", "C" ]. If called subsequently with a maximumOverlap of
     * 1, then another computabe shard is; [ "A", "D", "E" ]. No other shard is
     * computable as the overlap would be greater than 1.
     * 
     * This class will perform a recursive backtracking search and make every
     * effort to identify any computable shuffle shards.
     * 
     * @param store
     *            A storage engine implementing the FragmentStore API. This
     *            store is used to store and retrieve the fragments associated
     *            with previously assigned shuffle shards to avoid collisions.
     */
    public StatefulSearchingShuffleSharder(FragmentStore<T> store) {
        this.store = store;
    }

    /**
     * Search for a shuffle shard, with a guarantee about the maximum amount of
     * overlap with any other shuffle shard that has been created already and
     * recorded by the fragment store.
     * 
     * @param lattice
     *            The Infima Lattice to use
     * @param identifier
     *            The identifier
     * @param endpointsPerCell
     *            The number of endpoints to choose from each eligible lattice
     *            cell
     * @param maximumOverlap
     *            The maximum overlap permitted between any two shuffle shards
     * 
     * @return An Infima Lattice representing the chosen endpoints in the
     *         shuffle shard
     * @throws NoShardsAvailableException
     */
    public Lattice<T> shuffleShard(Lattice<T> lattice, int endpointsPerCell, int maximumOverlap)
            throws NoShardsAvailableException {
        Lattice<T> shard = shuffleShardRecursiveHelper(lattice, endpointsPerCell, maximumOverlap);

        if (shard.getAllEndpoints().size() == 0) {
            throw new NoShardsAvailableException();
        }

        /* Mark the fragments as used to that overlap can be avoided */
        for (List<T> fragment : new IterableSubListGenerator<T>(shard.getAllEndpoints(), maximumOverlap + 1)) {
            store.saveFragment(fragment);
        }

        return shard;
    }

    private Lattice<T> shuffleShardRecursiveHelper(Lattice<T> lattice, int endpointsPerCell, int maximumOverlap) {
        List<List<String>> allCoordinates = new ArrayList<List<String>>(lattice.getAllCoordinates());

        /*
         * The first cell we pick should be fine, but if we resort to
         * backtracking, be prepared to iterate across all cells.
         */
        Collections.shuffle(allCoordinates);
        for (List<String> coordinate : allCoordinates) {

            /*
             * Create a lattice that is the compliment of the chosen dimension
             * values. E.. if coordinate is [ "us-east-1", "2.1" ] then we
             * remove the "us-east-1" row and "2.1" column from the compliment
             * sub-lattice.
             * 
             * We'll use this sub-lattice shortly when we do a recursive search.
             */
            Lattice<T> compliment = lattice;
            for (int i = 0; i < lattice.getDimensionality().size(); i++) {
                compliment = compliment.simulateFailure(lattice.getDimensionName(i), coordinate.get(i));
            }

            /* Get the end-points in the chosen coordinate */
            List<T> endpoints = new ArrayList<T>(lattice.getEndpointsForSector(coordinate));

            /*
             * Just as with the cells, we need to be prepared to iterate over
             * every combination of end-points within the cell.
             */
            Collections.shuffle(endpoints);
            for (List<T> fragment : new IterableSubListGenerator<T>(endpoints, endpointsPerCell)) {
                /*
                 * If the contents of even one cell can cause a collision, we
                 * need to filter the collisions. There's no sense in a wasteful
                 * recursion.
                 */
                if (fragment.size() >= maximumOverlap && areThereTooManyCollisions(fragment, maximumOverlap)) {
                    continue;
                }

                /*
                 * Now combine the choices we've just made with a recursive
                 * search.
                 */
                Lattice<T> pickedRecursively = shuffleShardRecursiveHelper(compliment, endpointsPerCell, maximumOverlap);
                List<T> combined = new ArrayList<T>(fragment);
                combined.addAll(pickedRecursively.getAllEndpoints());

                if (combined.size() >= maximumOverlap && areThereTooManyCollisions(combined, maximumOverlap)) {
                    continue;
                }

                /*
                 * If we have gotten this far, then we have a valid set of
                 * endpoints to return
                 */
                pickedRecursively.addEndpointsForSector(coordinate, fragment);

                return pickedRecursively;
            }
        }

        /*
         * Every option has been exhausted and we didn't find anything. Return
         * an empty lattice
         */
        return new Lattice<T>(lattice.getDimensionNames());
    }

    private boolean areThereTooManyCollisions(List<T> haystack, int maximumOverlap) {
        if (haystack.size() <= maximumOverlap) {
            return false;
        } else if (haystack.size() == maximumOverlap + 1) {
            return store.isFragmentUsed(haystack);
        }

        for (List<T> fragment : new IterableSubListGenerator<T>(haystack, maximumOverlap + 1)) {
            if (store.isFragmentUsed(fragment)) {
                return true;
            }
        }

        return false;
    }
}
