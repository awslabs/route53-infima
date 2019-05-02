/**
 * Copyright 2013-2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
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
