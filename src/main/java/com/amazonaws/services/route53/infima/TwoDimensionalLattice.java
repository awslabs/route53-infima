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
 * A convenience class for a two dimensional {@link Lattice}.
 * 
 * @param <T>
 *            The type of the endpoints in this lattice.
 */
public class TwoDimensionalLattice<T> extends Lattice<T> {

    /**
     * Create a two dimensional Lattice where each dimension represents a
     * meaningful availability axis. For example a List containing the strings
     * "AvailabilityZone", "SoftwareVersion".
     * 
     * @param dimensionXname
     *            name for the first dimension
     * @param dimensionYname
     *            name for the second dimension
     */
    public TwoDimensionalLattice(String dimensionXname, String dimensionYname) {
        super(Arrays.asList(dimensionXname, dimensionYname));
    }

    /**
     * Add endpoints to this lattice
     * 
     * @param dimensionXcoord
     *            the coordinate for the endpoints on the first dimension
     * @param dimensionYcoord
     *            the coordinate for the endpoints on the second dimension
     * @param endpoints
     *            List of endpoints to include in the lattice
     */
    public void addEndpoints(String dimensionXcoord, String dimensionYcoord, Collection<T> endpoints) {
        super.addEndpointsForSector(Arrays.asList(dimensionXcoord, dimensionYcoord), endpoints);
    }

    /**
     * Add an endpoint to this lattice
     * 
     * @param dimensionXcoord
     *            the coordinate for the endpoint on the first dimension
     * @param dimensionYcoord
     *            the coordinate for the endpoint on the second dimension
     * @param endpoint
     *            the endpoints to include in the lattice
     */
    public void addEndpoint(String dimensionXcoord, String dimensionYcoord, T endpoint) {
        addEndpoints(dimensionXcoord, dimensionYcoord, Collections.singleton(endpoint));
    }

    /**
     * Get the endpoints in a given sector of the lattice
     * 
     * @param dimensionXcoord
     *            the coordinate for the sector on the first dimension
     * @param dimensionYcoord
     *            the coordinate for the sector on the second dimension
     * @return The endpoints for the given coordinates
     */
    public Collection<T> getEndpoints(String dimensionXcoord, String dimensionYcoord) {
        return super.getEndpointsForSector(Arrays.asList(dimensionXcoord, dimensionYcoord));
    }

}
