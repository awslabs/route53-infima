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
