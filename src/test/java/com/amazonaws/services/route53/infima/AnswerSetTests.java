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
package com.amazonaws.services.route53.infima;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import com.amazonaws.services.route53.infima.util.AnswerSet;
import com.amazonaws.services.route53.infima.util.ComparableResourceRecord;
import com.amazonaws.services.route53.infima.util.HealthCheckedResourceRecord;
import com.amazonaws.services.route53.model.ResourceRecordSet;

public class AnswerSetTests {

    @Test
    public void testOrdering() {
        AnswerSet answer = new AnswerSet();
        answer.add(new ComparableResourceRecord("3.3.3.3"));
        answer.add(new ComparableResourceRecord("2.2.2.2"));
        answer.add(new ComparableResourceRecord("1.1.1.1"));

        if (answer.add(new ComparableResourceRecord("1.1.1.1"))) {
            fail("Duplicate record accepted");
        }

        ComparableResourceRecord[] records = new ComparableResourceRecord[3];
        answer.toArray(records);
        assertEquals(records[0].getValue(), "1.1.1.1");
        assertEquals(records[1].getValue(), "2.2.2.2");
        assertEquals(records[2].getValue(), "3.3.3.3");
    }

    @Test
    public void testSimpleRRSet() {
        AnswerSet answer = new AnswerSet();
        answer.add(new ComparableResourceRecord("3.3.3.3"));
        answer.add(new ComparableResourceRecord("2.2.2.2"));
        answer.add(new ComparableResourceRecord("1.1.1.1"));

        assertEquals(1, answer.toResourceRecordSets("Z123", "www.example.com", "A", 60L).size());
        ResourceRecordSet rrset = answer.toResourceRecordSets("Z123", "www.example.com", "A", 60L).get(0);
        assertEquals(new Long(60L), rrset.getTTL());
        assertEquals("A", rrset.getType());
        assertEquals("www.example.com", rrset.getName());
        assertEquals("1.1.1.1", rrset.getResourceRecords().get(0).getValue());
        assertEquals("2.2.2.2", rrset.getResourceRecords().get(1).getValue());
        assertEquals("3.3.3.3", rrset.getResourceRecords().get(2).getValue());
    }

    @Test
    public void testHealthCheckedRRSet() {
        AnswerSet answer = new AnswerSet();
        answer.add(new HealthCheckedResourceRecord("hcid1", "3.3.3.3"));
        answer.add(new HealthCheckedResourceRecord("hcid2", "2.2.2.2"));
        answer.add(new HealthCheckedResourceRecord("hcid3", "1.1.1.1"));

        List<ResourceRecordSet> rrsets = answer.toResourceRecordSets("Z123", "www.example.com", "A", 60L);
        assertEquals(3, rrsets.size());

        /* First rrset should be the leafnode */
        assertEquals(new Long(60L), rrsets.get(0).getTTL());
        assertEquals("A", rrsets.get(0).getType());
        assertEquals("1.1.1.1", rrsets.get(0).getResourceRecords().get(0).getValue());
        assertEquals("2.2.2.2", rrsets.get(0).getResourceRecords().get(1).getValue());
        assertEquals("3.3.3.3", rrsets.get(0).getResourceRecords().get(2).getValue());
        assertEquals("hcid1", rrsets.get(0).getHealthCheckId());

        /* Next rrset should be an alias, pointing to the leafnode */
        assertEquals("A", rrsets.get(1).getType());
        assertEquals(rrsets.get(0).getName(), rrsets.get(1).getAliasTarget().getDNSName());
        assertEquals("Z123", rrsets.get(1).getAliasTarget().getHostedZoneId());
        assertEquals(true, rrsets.get(1).getAliasTarget().getEvaluateTargetHealth());

        /* Final rrset should be an alias pointing to the first alias */
        assertEquals("A", rrsets.get(2).getType());
        assertEquals(rrsets.get(1).getName(), rrsets.get(2).getAliasTarget().getDNSName());
        assertEquals("Z123", rrsets.get(2).getAliasTarget().getHostedZoneId());
        assertEquals(true, rrsets.get(2).getAliasTarget().getEvaluateTargetHealth());

        /* Final rrset is also the entry node */
        assertEquals("www.example.com", rrsets.get(2).getName());
    }

}
