/**
 * Copyright 2013-2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazonaws.services.route53.infima.util;

import com.amazonaws.services.route53.model.ResourceRecord;

/**
 * An Comparable Resource Record type for Route 53 Resource Records.
 * 
 * This utility class encapsulates Route 53 ResourceRecords and allows them to
 * be used in Sets and other containers which require Comparable.
 */
@SuppressWarnings("serial")
public class ComparableResourceRecord extends ResourceRecord implements Comparable<ComparableResourceRecord> {

    /**
     * Create a comparable resource record
     * 
     * @param value
     *            The value for the record data
     */
    public ComparableResourceRecord(String value) {
        super(value);
    }

    /**
     * Compare this resource record to another
     * 
     * @param other
     *            the other record to compare to
     */
    public int compareTo(ComparableResourceRecord other) {
        return this.getValue().compareTo(other.getValue());
    }
}
