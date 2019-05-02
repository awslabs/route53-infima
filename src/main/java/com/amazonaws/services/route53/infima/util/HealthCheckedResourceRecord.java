/**
 * Copyright 2013-2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazonaws.services.route53.infima.util;

import java.util.Collections;
import java.util.List;

/**
 * An health checked Resource Record type for Route 53 Resource Records.
 * 
 * This utility class encapsulates Route 53 ResourceRecords and allows them to
 * be directly associated with Route 53 health checks.
 */
@SuppressWarnings("serial")
public class HealthCheckedResourceRecord extends ComparableResourceRecord {
    private final List<String> healthcheckIds;

    /**
     * Create a HealthCheckedResourceRecord
     * 
     * @param healthcheckIds
     *            The list of Route 53 health check ids to associate
     * @param recordData
     *            The data for the Resource Record
     */
    public HealthCheckedResourceRecord(List<String> healthcheckIds, String recordData) {
        super(recordData);
        this.healthcheckIds = healthcheckIds;
    }

    /**
     * Create a HealthCheckedResourceRecord
     * 
     * @param healthcheckId
     *            The Route 53 health check id to associate
     * @param recordData
     *            The data for the resource record
     */
    public HealthCheckedResourceRecord(String healthcheckId, String recordData) {
        super(recordData);
        this.healthcheckIds = Collections.singletonList(healthcheckId);
    }

    /**
     * Retrieve the list of Route 53 health check ids associated with this
     * record
     * 
     * @return the list of Route 53 health check ids associated with this record
     */
    public List<String> getHealthcheckIds() {
        return healthcheckIds;
    }
}
