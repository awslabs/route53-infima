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

import static org.junit.Assert.assertEquals;

import java.net.UnknownHostException;
import java.util.Arrays;

import org.junit.Test;

public class LatticeTests {

    @Test
    public void testSingleCellLattice() throws UnknownHostException {
        SingleCellLattice<String> lattice = new SingleCellLattice<String>();
        lattice.addEndpoint("A");
        lattice.addEndpoints(Arrays.asList("B", "C", "D"));

        /* Check that all of the end points in, with ordering */
        assertEquals(lattice.getAllEndpoints().toString(), "[B, C, D, A]");
    }

    @Test
    public void testOneDimensionalLattice() throws UnknownHostException {
        String[] endpointsA = new String[] { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J" };
        String[] endpointsB = new String[] { "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T" };

        OneDimensionalLattice<String> lattice = new OneDimensionalLattice<String>("AZ");
        lattice.addEndpoints("us-east-1a", Arrays.asList(endpointsA));
        lattice.addEndpoints("us-east-1b", Arrays.asList(endpointsB));

        assertEquals(lattice.getAllEndpoints().size(), 20);
        assertEquals(lattice.simulateFailure("AZ", "us-east-1a").getAllEndpoints().size(), 10);
        assertEquals(lattice.simulateFailure("AZ", "us-east-1b").getAllEndpoints().size(), 10);
    }

    @Test
    public void testTwoDimensionalLattice() throws UnknownHostException {
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

        assertEquals(lattice.getAllEndpoints().size(), 20);
        assertEquals(lattice.simulateFailure("AZ", "us-east-1a").getAllEndpoints().size(), 10);
        assertEquals(lattice.simulateFailure("AZ", "us-east-1b").getAllEndpoints().size(), 10);
        assertEquals(lattice.simulateFailure("Version", "1").getAllEndpoints().size(), 10);
        assertEquals(lattice.simulateFailure("Version", "2").getAllEndpoints().size(), 10);
        assertEquals(lattice.simulateFailure("AZ", "us-east-1a").simulateFailure("Version", "1").getAllEndpoints().size(), 5);
        assertEquals(lattice.simulateFailure("AZ", "us-east-1a").simulateFailure("Version", "2").getAllEndpoints().size(), 5);
        assertEquals(lattice.simulateFailure("AZ", "us-east-1b").simulateFailure("Version", "1").getAllEndpoints().size(), 5);
        assertEquals(lattice.simulateFailure("AZ", "us-east-1b").simulateFailure("Version", "2").getAllEndpoints().size(), 5);
    }
}
