// Copyright 2019 Oath Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.hosted.controller.persistence;

import com.yahoo.config.provision.ApplicationId;
import com.yahoo.config.provision.HostName;
import com.yahoo.slime.ArrayTraverser;
import com.yahoo.slime.Cursor;
import com.yahoo.slime.Inspector;
import com.yahoo.slime.Slime;
import com.yahoo.vespa.hosted.controller.application.RoutingPolicy;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Serializer and deserializer for a {@link RoutingPolicy}.
 *
 * @author mortent
 */
public class RoutingPolicySerializer {

    private static final String routingPoliciesField = "routingPolicies";
    private static final String aliasesField = "aliases";
    private static final String idField = "id";
    private static final String recordIdField = "recordId";
    private static final String aliasField = "alias";
    private static final String canonicalNameField = "canonicalName";

    public Slime toSlime(Set<RoutingPolicy> routingPolicies) {
        Slime slime = new Slime();
        Cursor root = slime.setObject();
        Cursor policyArray = root.setArray(routingPoliciesField);
        routingPolicies.forEach(policy -> {
            Cursor policyObject = policyArray.addObject();
            policyObject.setString(recordIdField, policy.recordId());
            policyObject.setString(aliasField, policy.alias().value());
            policyObject.setString(canonicalNameField, policy.canonicalName().value());
        });
        return slime;
    }

    public Set<RoutingPolicy> fromSlime(ApplicationId owner, Slime slime) {
        Set<RoutingPolicy> policies = new LinkedHashSet<>();
        Cursor root = slime.get();
        Cursor field = root.field(routingPoliciesField);
        if (!field.valid()) {
            field = root.field(aliasesField); // TODO: Remove after 7.9 has been released
        }
        field.traverse((ArrayTraverser) (i, inspect) -> {
            Inspector recordId = inspect.field(recordIdField);
            if (!recordId.valid()) {
                recordId = inspect.field(idField); // TODO: Remove after 7.9 has been released
            }
            policies.add(new RoutingPolicy(owner,
                                           recordId.asString(),
                                           HostName.from(inspect.field(aliasField).asString()),
                                           HostName.from(inspect.field(canonicalNameField).asString())));
        });
        return Collections.unmodifiableSet(policies);
    }

}
