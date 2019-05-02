/**
 * Copyright 2013-2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazonaws.services.route53.infima;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.amazonaws.services.route53.infima.util.AnswerSet;
import com.amazonaws.services.route53.infima.util.HealthCheckedResourceRecord;
import com.amazonaws.services.route53.infima.util.IterableSubListGenerator;
import com.amazonaws.services.route53.model.AliasTarget;
import com.amazonaws.services.route53.model.ResourceRecordSet;

public class RubberTree {

    /* Helper class: Compare sublists by size */
    static private class sublistComparator implements Comparator<List<String>> {
        @Override
        public int compare(List<String> arg0, List<String> arg1) {
            return arg0.size() - arg1.size();
        }
    }

    /**
     * Generate a RubberTree of Route 53 record sets from an Infima
     * {@link Lattice}. RubberTree will pre-compute all of the records necessary
     * to handle the failure of any particular endpoint, or the failure of any
     * dimensional value from the Infima {@link Lattice}.
     * 
     * @param hostedZoneId
     *            The Route 53 hosted zone id containing the rubber tree
     * @param name
     *            The DNS name for the root of the rubber tree
     * @param type
     *            The DNS type for the rubber tree
     * @param ttl
     *            The DNS time-to-live for the record sets
     * @param lattice
     *            An Infima Lattice representing the available records
     * @param recordsPerRecordSet
     *            The maximum number of records to include in any record set
     * @return An ordered list of Route 53 record sets, these record sets should
     *         be created using the Route 53 API in the same order that they are
     *         returned by this method.
     */
    public static List<ResourceRecordSet> vulcanize(String hostedZoneId, String name, String type, Long ttl,
            Lattice<HealthCheckedResourceRecord> lattice, int recordsPerRecordSet) {
        /* A one by one lattice is a special case */
        if (lattice.getAllCoordinates().size() == 1) {
            return vulcanize(hostedZoneId, name, type, ttl, lattice.getAllEndpoints(), recordsPerRecordSet);
        }

        List<List<String>> coordinates = new ArrayList<List<String>>();
        coordinates.addAll(lattice.getAllCoordinates());
        Collections.sort(coordinates, new sublistComparator());

        List<HealthCheckedResourceRecord> hcrr = new ArrayList<HealthCheckedResourceRecord>();

        /* Splice in the remaining endpoints */
        for (List<String> coordinate : lattice.getAllCoordinates()) {
            Collection<HealthCheckedResourceRecord> endpointsToSplice = lattice.getEndpointsForSector(coordinate);
            int newSize = hcrr.size() + endpointsToSplice.size();
            float spacing = newSize / endpointsToSplice.size();
            int i = 0;
            for (HealthCheckedResourceRecord endpoint : endpointsToSplice) {
                hcrr.add((int) (i++ * spacing), endpoint);
            }
        }

        List<ResourceRecordSet> vulcanized = vulcanize(hostedZoneId, name, type, ttl, hcrr, recordsPerRecordSet);

        /*
         * If there is more than one cell, add record sets for each anticipated
         * failure mode
         */
        if (lattice.getAllCoordinates().size() > 1) {

            /*
             * Rewrite all of the existing 0-weighted entry nodes to be primary
             * nodes but at the secondary level.
             */
            String secondaryName = "secondary." + name;
            for (ResourceRecordSet rr : vulcanized) {
                if (rr.getWeight() == 0L) {
                    rr.setName(secondaryName);
                    rr.setWeight(1L);
                }
            }

            /* Simulate a failure of each value of each dimension in turn */
            for (String dimension : lattice.getDimensionNames()) {
                for (String value : lattice.getDimensionValues(dimension)) {
                    /* Create a zero weighted alias to this set */
                    String subTreePrefix = dimension.substring(0, Math.min(dimension.length(), 30)) + "-"
                            + value.substring(0, Math.min(value.length(), 30));
                    String subTreeName = subTreePrefix + "." + secondaryName;
                    AliasTarget target = new AliasTarget();
                    target.setDNSName(subTreeName);
                    target.setEvaluateTargetHealth(true);
                    target.setHostedZoneId(hostedZoneId);
                    ResourceRecordSet rr = new ResourceRecordSet();
                    rr.setName(secondaryName);
                    rr.setWeight(0L);
                    rr.setType(type);
                    rr.setAliasTarget(target);
                    rr.setSetIdentifier(subTreePrefix);

                    vulcanized.addAll(vulcanize(hostedZoneId, subTreeName, type, ttl,
                            lattice.simulateFailure(dimension, value).getAllEndpoints(), recordsPerRecordSet));
                    vulcanized.add(rr);
                }
            }

            /* Add an alias to the secondary level */
            AliasTarget target = new AliasTarget();
            target.setDNSName(secondaryName);
            target.setEvaluateTargetHealth(true);
            target.setHostedZoneId(hostedZoneId);
            ResourceRecordSet rr = new ResourceRecordSet();
            rr.setName(name);
            rr.setWeight(0L);
            rr.setType(type);
            rr.setAliasTarget(target);
            rr.setSetIdentifier("secondary for " + name);

            vulcanized.add(rr);
        }

        return vulcanized;
    }

    /**
     * Generate a RubberTree of Route 53 record sets from a list of health
     * checked resource records. RubberTree will pre-compute all of the records
     * necessary to handle the failure of any particular endpoint.
     * 
     * @param hostedZoneId
     *            The Route 53 hosted zone id containing the rubber tree
     * @param name
     *            The DNS name for the root of the rubber tree
     * @param type
     *            The DNS type for the rubber tree
     * @param ttl
     *            The DNS time-to-live for the record sets
     * @param records
     *            A list of health checks resource records
     * @param recordsPerRecordSet
     *            The maximum number of records to include in any record set
     * @return An ordered list of Route 53 record sets, these record sets should
     *         be created using the Route 53 API in the same order that they are
     *         returned by this method.
     */
    public static List<ResourceRecordSet> vulcanize(String hostedZoneId, String name, String type, Long ttl,
            List<HealthCheckedResourceRecord> records, int recordsPerRecordSet) {
        List<ResourceRecordSet> rrs = new ArrayList<ResourceRecordSet>();
        List<HealthCheckedResourceRecord> hcrr = new ArrayList<HealthCheckedResourceRecord>(records);

        if (recordsPerRecordSet > 8) {
            throw new IllegalArgumentException("Rubber Tree supports 8 or fewer records or record set");
        }

        if (hcrr.size() > recordsPerRecordSet) {
            /*
             * We have more records than we need per record set. Construct a
             * pseudo-ring by appending the head of the list to the tail.
             */
            int originalCount = hcrr.size();
            hcrr.addAll(hcrr.subList(0, recordsPerRecordSet - 1));

            /* Pick overlapping slices across the pseudo-ring */
            for (int i = 0; i < originalCount; i++) {
                AnswerSet answer = new AnswerSet();
                answer.addAll(hcrr.subList(i, i + recordsPerRecordSet));
                rrs.addAll(answer.toResourceRecordSets(hostedZoneId, name, type, ttl));
            }

            return rrs;
        }

        /*
         * We have <= recordsPerRecordSet records. Add an answer with all of
         * them, and secondary answers covering each record failure.
         */
        AnswerSet answer = new AnswerSet();
        answer.addAll(hcrr);
        rrs.addAll(answer.toResourceRecordSets(hostedZoneId, name, type, ttl));

        for (List<HealthCheckedResourceRecord> fragment : new IterableSubListGenerator<HealthCheckedResourceRecord>(
                records, recordsPerRecordSet - 1)) {
            answer = new AnswerSet();
            answer.addAll(fragment);
            rrs.addAll(answer.toResourceRecordSets(hostedZoneId, name, type, ttl));
            rrs.get(rrs.size() - 1).setWeight(0L);
        }

        return rrs;
    }
}
