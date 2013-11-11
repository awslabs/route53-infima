package com.amazonaws.services.route53.infima;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import com.amazonaws.services.route53.infima.util.HealthCheckedResourceRecord;
import com.amazonaws.services.route53.model.ResourceRecordSet;

public class RubbeeTreeTests {

    @Test
    public void smallRubberTreeTest() {
        String[] endpoints = new String[] { "A", "B", "C", "D", "E", "F", "G", "H" };

        SingleCellLattice<HealthCheckedResourceRecord> lattice = new SingleCellLattice<HealthCheckedResourceRecord>();

        for (String endpoint : endpoints) {
            HealthCheckedResourceRecord hcrr = new HealthCheckedResourceRecord(endpoint, endpoint);
            lattice.addEndpoint(hcrr);
        }

        List<ResourceRecordSet> rrs = RubberTree.vulcanize("Z124", "www.example.com", "TXT", 60L, lattice, 8);

        /*
         * 1 chain of size 8 = 8 8 chains of size 7 = 56 total = 64
         */
        assertEquals(64, rrs.size());
    }

    @Test
    public void bigRubberTreeTest() {
        String[] endpoints = new String[] { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O",
                "P", "Q", "R", "S", "T" };

        SingleCellLattice<HealthCheckedResourceRecord> lattice = new SingleCellLattice<HealthCheckedResourceRecord>();

        for (String endpoint : endpoints) {
            HealthCheckedResourceRecord hcrr = new HealthCheckedResourceRecord(endpoint, endpoint);
            lattice.addEndpoint(hcrr);
        }

        List<ResourceRecordSet> rrs = RubberTree.vulcanize("Z124", "www.example.com", "TXT", 60L, lattice, 8);

        /* 20 chains, each of depth 8 = 20 x 8 = 160 */
        assertEquals(160, rrs.size());
    }

    @Test
    public void twoDimensionalRubberTreeTest() {
        /* Use a 2-D lattice with 20 endpoints for a very simple test */
        String[] endpointsA1 = new String[] { "A", "B", "C", "D", "E" };
        String[] endpointsA2 = new String[] { "F", "G", "H", "I", "J" };
        String[] endpointsB1 = new String[] { "K", "L", "M", "N", "O" };
        String[] endpointsB2 = new String[] { "P", "Q", "R", "S", "T" };

        TwoDimensionalLattice<HealthCheckedResourceRecord> lattice = new TwoDimensionalLattice<HealthCheckedResourceRecord>(
                "AZ", "Version");
        for (String endpoint : endpointsA1) {
            HealthCheckedResourceRecord hcrr = new HealthCheckedResourceRecord(endpoint, endpoint);
            lattice.addEndpoint("us-east-1a", "1", hcrr);
        }
        for (String endpoint : endpointsA2) {
            HealthCheckedResourceRecord hcrr = new HealthCheckedResourceRecord(endpoint, endpoint);
            lattice.addEndpoint("us-east-1a", "2", hcrr);
        }
        for (String endpoint : endpointsB1) {
            HealthCheckedResourceRecord hcrr = new HealthCheckedResourceRecord(endpoint, endpoint);
            lattice.addEndpoint("us-east-1b", "1", hcrr);
        }
        for (String endpoint : endpointsB2) {
            HealthCheckedResourceRecord hcrr = new HealthCheckedResourceRecord(endpoint, endpoint);
            lattice.addEndpoint("us-east-1b", "2", hcrr);
        }

        List<ResourceRecordSet> rrs = RubberTree.vulcanize("Z124", "www.example.com", "TXT", 60L, lattice, 8);

        /*
         * 20 primary chains, each of depth 8 = 20 x 8 = 160 1 alias to the
         * secondary level = 1 4x secondary aliases = 4 4 x 10 secondary chains
         * of length 8 = 4 x 10 x 8 = 320 total = 485
         */
        assertEquals(485, rrs.size());
    }

}
