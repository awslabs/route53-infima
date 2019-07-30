/**
 * Copyright 2013-2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazonaws.services.route53.infima;

import java.util.Collection;
import java.util.Collections;

/**
 * A convenience class for a single-cell {@link Lattice}. Every endpoint is in a
 * single compartment.
 * 
 * @param <T>
 *            The type of the endpoints in this lattice.
 */
public class SingleCellLattice<T> extends OneDimensionalLattice<T> {

    /*
     * Single Cells don't have any dimensions, so use dimension X
     */
    private static String DIMENSION = "DimensionX";

    /**
     * Create a single cell lattice. All of the elements in this lattice are in
     * the same availability "bucket", e.g. "availability zone".
     */
    public SingleCellLattice() {
        super(DIMENSION);
    }

    /**
     * Create a single cell lattice. All of the elements in this lattice are in
     * the same availability "bucket", e.g. "availability zone".
     * 
     * @param endpoints
     *            List of endpoints to include in the lattice
     */
    public SingleCellLattice(Collection<T> endpoints) {
        super(DIMENSION);
        this.addEndpoints(DIMENSION, endpoints);
    }

    /**
     * Add endpoints to this lattice
     * 
     * @param endpoints
     *            List of endpoints to include in the lattice
     */
    public void addEndpoints(Collection<T> endpoints) {
        super.addEndpoints(DIMENSION, endpoints);
    }

    /**
     * Add an endpoint to this lattice
     * 
     * @param endpoint
     *            endpoint to include in the lattice
     */
    public void addEndpoint(T endpoint) {
        this.addEndpoints(Collections.singleton(endpoint));
    }

    /**
     * Get all endpoints in this lattice
     * 
     * @return all endpoints in the lattice
     */
    public Collection<T> getEndpoints() {
        return super.getEndpoints(DIMENSION);
    }
}
