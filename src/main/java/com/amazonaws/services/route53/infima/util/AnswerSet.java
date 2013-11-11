/**
 * Copyright 2013-2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License"). 
 * You may not use this file except in compliance with the License. 
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is distributed 
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, express 
 * or implied. See the License for the specific language governing permissions 
 * and limitations under the License. 
 */
package com.amazonaws.services.route53.infima.util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import com.amazonaws.services.route53.model.AliasTarget;
import com.amazonaws.services.route53.model.ResourceRecord;
import com.amazonaws.services.route53.model.ResourceRecordSet;

/**
 * An Answer Set type for Route 53 Resource Records.
 * 
 * DNS record sets can consist of multiple unique resource records, e.g. an "A"
 * record set may contain 4 IP addresses. The Route 53 API supports one health
 * check per ResourceRecordSet. The AnswerSet class allows you to compose
 * Resource Record Sets that use a mix of regular resource records and
 * health-checked resource records.
 * 
 * If there are multiple health checks within the answer set, the
 * {@code toResourceRecordSets} method will create a chain of Aliases records,
 * each dependent on a different Route 53 health check, to form a logical AND
 * series.
 * 
 * As Route 53 supports full backtracking when answering queries, these alias
 * chains may be used in place of any regular resource record set.
 */
@SuppressWarnings("serial")
public class AnswerSet extends TreeSet<ComparableResourceRecord> {

    /**
     * Convert an answer set to a list of Route53 Resource Record Sets,
     * including any chained alias record sets needed to use multiple
     * healthchecks.
     * 
     * @param hostedZoneId
     *            The hosted zone id for the zone containing the record
     * @param name
     *            The DNS name for the record sets
     * @param type
     *            The DNS type for the record sets
     * @param ttl
     *            The time-to-live for the record sets
     * @return An ordered list of Route 53 record sets, these record sets should
     *         be created using the Route 53 API in the same order that they are
     *         returned by this method.
     */
    public List<ResourceRecordSet> toResourceRecordSets(String hostedZoneId, String name, String type, long ttl) {
        ArrayList<ResourceRecordSet> rrsList = new ArrayList<ResourceRecordSet>();
        TreeSet<String> healthcheckIds = new TreeSet<String>();

        List<ResourceRecord> rrs = new ArrayList<ResourceRecord>();
        for (ComparableResourceRecord crr : this) {
            /* Get all of the healthchecks associated with the answer set */
            if (crr instanceof HealthCheckedResourceRecord) {
                healthcheckIds.addAll(((HealthCheckedResourceRecord) crr).getHealthcheckIds());
            }

            /* Convert to the baseclass */
            rrs.add(crr);
        }

        ResourceRecordSet leafNode = new ResourceRecordSet();

        leafNode.setType(type);
        leafNode.setTTL(ttl);
        leafNode.setResourceRecords(rrs);

        /* Set a nominal weight so that we can set an identifier */
        leafNode.setWeight(1L);
        leafNode.setSetIdentifier("leafnode");
        leafNode.setName(name);

        List<String> healthchecksRemaining = new ArrayList<String>(healthcheckIds);
        if (healthchecksRemaining.size() > 0) {
            leafNode.setHealthCheckId(healthchecksRemaining.remove(0));
        }

        rrsList.add(leafNode);

        if (healthchecksRemaining.isEmpty()) {
            return rrsList;
        }

        /*
         * If more than one health-check is associated with the answer set, we
         * can use a chain of ALIAS records, each dependent on a different
         * health-check to form a logical AND of the health-checks.
         */
        ResourceRecordSet entryNode = leafNode;
        while (!healthchecksRemaining.isEmpty()) {
            ResourceRecordSet alias = new ResourceRecordSet();
            AliasTarget aliasTarget = new AliasTarget();

            String checksum = checksumResourceRecordSetData(entryNode);

            /* The alias will be the new entry node, inherit its properties */
            alias.setName(entryNode.getName());
            alias.setType(entryNode.getType());
            alias.setWeight(entryNode.getWeight());

            /* Consume a health-check */
            alias.setHealthCheckId(healthchecksRemaining.remove(0));

            /* Rename the previous entry node */
            String targetName = checksum + "." + entryNode.getName();
            entryNode.setName(targetName);
            aliasTarget.setDNSName(targetName);
            aliasTarget.setEvaluateTargetHealth(true);
            aliasTarget.setHostedZoneId(hostedZoneId);

            /* Associate the alias and the alias target */
            alias.setSetIdentifier("Alias to " + checksum);
            alias.setAliasTarget(aliasTarget);

            /* Add the alias and make it the new entry node */
            rrsList.add(alias);
            entryNode = alias;
        }

        return rrsList;
    }

    /**
     * Compute a checksum of the contents of a resource record set data and
     * return it in string form.
     * 
     * @param rr
     *            the record set to checksum
     * @return the checksum
     */
    private String checksumResourceRecordSetData(ResourceRecordSet rr) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(rr.getType().getBytes("UTF-8"));

            if (rr.getAliasTarget() != null) {
                digest.update(rr.getAliasTarget().getHostedZoneId().getBytes("UTF-8"));
                digest.update(rr.getAliasTarget().getDNSName().getBytes("UTF-8"));
                digest.update(rr.getAliasTarget().getEvaluateTargetHealth().toString().getBytes("UTF-8"));
            } else {
                digest.update(rr.getResourceRecords().toString().getBytes("UTF-8"));
                digest.update(rr.getTTL().toString().getBytes("UTF-8"));
            }

            /* Return the digest in base 36 */
            BigInteger bi = new BigInteger(digest.digest());
            return bi.toString(36);
        } catch (NoSuchAlgorithmException e) {
            /* Unreachable: MD5 is built in */
            throw new RuntimeException("MD5 is not supported");
        } catch (UnsupportedEncodingException e) {
            /* Unreachable: UTF-8 is built in */
            throw new RuntimeException("UTF-8 is not supported");
        }
    }
}
