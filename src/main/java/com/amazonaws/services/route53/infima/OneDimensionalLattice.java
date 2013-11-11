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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * A convenience class for a one dimensional {@link Lattice}.
 * 
 * @param <T>
 *            The type of the endpoints in this lattice.
 */
public class OneDimensionalLattice<T> extends Lattice<T> {

    /**
     * Create a one dimensional Lattice where the dimension represents a
     * meaningful availability axis. For example "AvailabilityZone".
     * 
     * @param dimensionXname
     *            name for the dimension
     */
    public OneDimensionalLattice(String dimensionXname) {
        super(Arrays.asList(dimensionXname));
    }

    /**
     * Create a one dimensional Lattice with "AvailabilityZone" as the
     * dimension.
     */
    public OneDimensionalLattice() {
        this("AvailabilityZone");
    }

    /**
     * Add endpoints to this lattice
     * 
     * @param dimensionXcoord
     *            the coordinate for the endpoints
     * @param endpoints
     *            List of endpoints to include in the lattice
     */
    public void addEndpoints(String dimensionXcoord, Collection<T> endpoints) {
        super.addEndpointsForSector(Arrays.asList(dimensionXcoord), endpoints);
    }

    /**
     * Get the endpoints for a coordinate
     * 
     * @param dimensionXcoord
     *            the coordinate for the endpoints
     * @return the endpoints for the given coordinate
     */
    public Collection<T> getEndpoints(String dimensionXcoord) {
        return super.getEndpointsForSector(Arrays.asList(dimensionXcoord));
    }

    /**
     * Add an endpoint to this lattice
     * 
     * @param dimensionXcoord
     *            the coordinate for the endpoints
     * @param endpoint
     *            The endpoint to include in the lattice
     */
    public void addEndpoint(String dimensionXcoord, T endpoint) {
        this.addEndpoints(dimensionXcoord, Collections.singleton(endpoint));
    }

}
