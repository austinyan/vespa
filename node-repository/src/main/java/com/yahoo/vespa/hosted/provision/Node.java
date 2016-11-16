// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.hosted.provision;

import com.yahoo.config.provision.ApplicationId;
import com.yahoo.config.provision.ClusterMembership;
import com.yahoo.config.provision.NodeType;
import com.yahoo.vespa.hosted.provision.node.Allocation;
import com.yahoo.vespa.hosted.provision.node.Flavor;
import com.yahoo.vespa.hosted.provision.node.History;
import com.yahoo.vespa.hosted.provision.node.Status;
import com.yahoo.vespa.hosted.provision.node.Generation;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A node in the node repository. The identity of a node is given by its id.
 * The classes making up the node model are found in the node package.
 * This (and hence all classes referenced from it) is immutable.
 *
 * @author bratseth
 */
public final class Node {

    private final String id;
    private final String ipAddress;
    private final String hostname;
    private final String openStackId;
    private final Optional<String> parentHostname;
    private final Flavor flavor;
    private final Status status;
    private final State state;
    private final NodeType type;

    /** Record of the last event of each type happening to this node */
    private final History history;

    /** The current allocation of this node, if any */
    private Optional<Allocation> allocation;

    /** Creates a node in the initial state (provisioned) */
    public static Node create(String openStackId, String ipAddress, String hostname, Optional<String> parentHostname, Flavor flavor, NodeType type) {
        return new Node(openStackId, ipAddress, hostname, parentHostname, flavor, Status.initial(), State.provisioned,
                        Optional.empty(), History.empty(), type);
    }

    /** Do not use. Construct nodes by calling {@link NodeRepository#createNode} */
    private Node(String openStackId, String ipAddress, String hostname, Optional<String> parentHostname,
                Flavor flavor, Status status, State state, Allocation allocation, History history, NodeType type) {
        this(openStackId, ipAddress, hostname, parentHostname, flavor, status, state, Optional.of(allocation), history, type);
    }

    public Node(String openStackId, String ipAddress, String hostname, Optional<String> parentHostname,
                Flavor flavor, Status status, State state, Optional<Allocation> allocation,
                History history, NodeType type) {
        Objects.requireNonNull(openStackId, "A node must have an openstack id");
        requireNonEmptyString(ipAddress, "A node must have an IP address");
        requireNonEmptyString(hostname, "A node must have a hostname");
        requireNonEmptyString(parentHostname, "A parent host name must be a proper value");
        Objects.requireNonNull(flavor, "A node must have a flavor");
        Objects.requireNonNull(status, "A node must have a status");
        Objects.requireNonNull(state, "A null node state is not permitted");
        Objects.requireNonNull(allocation, "A null node allocation is not permitted");
        Objects.requireNonNull(history, "A null node history is not permitted");
        Objects.requireNonNull(type, "A null node type is not permitted");

        this.id = hostname;
        this.ipAddress = ipAddress;
        this.hostname = hostname;
        this.parentHostname = parentHostname;
        this.openStackId = openStackId;
        this.flavor = flavor;
        this.status = status;
        this.state = state;
        this.allocation = allocation;
        this.history = history;
        this.type = type;
    }

    /**
     * Returns the unique id of this host.
     * This may be the host name or some other opaque id which is unique across hosts
     */
    public String id() { return id; }

    /** Returns the IP address of this node */
    public String ipAddress() { return ipAddress; }

    /** Returns the host name of this node */
    public String hostname() { return hostname; }

    // TODO: Different meaning for vms and docker hosts?
    /** Returns the OpenStack id of this node, or of its docker host if this is a virtual node */
    public String openStackId() { return openStackId; }

    /** Returns the parent hostname for this node if this node is a docker container or a VM (i.e. it has a parent host). Otherwise, empty **/
    public Optional<String> parentHostname() { return parentHostname; }

    /** Returns the flavor of this node */
    public Flavor flavor() { return flavor; }

    /** Returns the known information about the node's ephemeral status */
    public Status status() { return status; }

    /** Returns the current state of this node (in the node state machine) */
    public State state() { return state; }

    /** Returns the type of this node */
    public NodeType type() { return type; }

    /** Returns the current allocation of this, if any */
    public Optional<Allocation> allocation() { return allocation; }

    /** Returns a history of the last events happening to this node */
    public History history() { return history; }

    /**
     * Returns a copy of this node which is retired by the application owning it.
     * If the node was already retired it is returned as-is.
     */
    public Node retireByApplication(Instant retiredAt) {
        if (allocation().get().membership().retired()) return this;
        return with(allocation.get().retire())
               .with(history.with(new History.RetiredEvent(retiredAt, History.RetiredEvent.Agent.application)));
    }

    /** Returns a copy of this node which is retired by the system */
    // We will use this when we support operators retiring a flavor completely from hosted Vespa
    public Node retireBySystem(Instant retiredAt) {
        return with(allocation.get().retire())
               .with(history.with(new History.RetiredEvent(retiredAt, History.RetiredEvent.Agent.system)));
    }

    /** Returns a copy of this node which is not retired */
    public Node unretire() {
        return with(allocation.get().unretire());
    }

    /** Returns a copy of this with the current restart generation set to generation */
    public Node withRestart(Generation generation) {
        final Optional<Allocation> allocation = this.allocation;
        if ( ! allocation.isPresent())
            throw new IllegalArgumentException("Cannot set restart generation for  " + hostname() + ": The node is unallocated");

        return with(allocation.get().withRestart(generation));
    }

    /** Returns a node with the status assigned to the given value */
    public Node with(Status status) {
        return new Node(openStackId, ipAddress, hostname, parentHostname, flavor, status, state, allocation, history, type);
    }

    /** Returns a node with the type assigned to the given value */
    public Node with(NodeType type) {
        return new Node(openStackId, ipAddress, hostname, parentHostname, flavor, status, state, allocation, history, type);
    }

    /** Returns a node with the flavor assigned to the given value */
    public Node with(Flavor flavor) {
        return new Node(openStackId, ipAddress, hostname, parentHostname, flavor, status, state, allocation, history, type);
    }

    /** Returns a copy of this with the current reboot generation set to generation */
    public Node withReboot(Generation generation) {
        return new Node(openStackId, ipAddress, hostname, parentHostname, flavor, status.withReboot(generation), state,
                        allocation, history, type);
    }

    /** Returns a copy of this with a history record saying it was detected to be down at this instant */
    public Node downAt(Instant instant) {
        return with(history.with(new History.Event(History.Event.Type.down, instant)));
    }

    /** Returns a copy of this with any history record saying it has been detected down removed */
    public Node up() {
        return with(history.without(History.Event.Type.down));
    }

    /** Returns a copy of this with allocation set as specified. <code>node.state</code> is *not* changed. */
    public Node allocate(ApplicationId owner, ClusterMembership membership, Instant at) {
        return this.with(new Allocation(owner, membership, new Generation(0, 0), false))
                   .with(history.with(new History.Event(History.Event.Type.reserved, at)));
    }

    /**
     * Returns a copy of this node with the allocation assigned to the given allocation.
     * Do not use this to allocate a node.
     */
    public Node with(Allocation allocation) {
        return new Node(openStackId, ipAddress, hostname, parentHostname, flavor, status, state, allocation, history, type);
    }

    /** Returns a copy of this node with the parent hostname assigned to the given value. */
    public Node withParentHostname(String parentHostname) {
        return new Node(openStackId, ipAddress, hostname, Optional.of(parentHostname), flavor, status, state,
                        allocation, history, type);
    }

    /** Returns a copy of this node with the given history. */
    public Node with(History history) {
        return new Node(openStackId, ipAddress, hostname, parentHostname, flavor, status, state, allocation, history, type);
    }

    private void requireNonEmptyString(Optional<String> value, String message) {
        Objects.requireNonNull(value, message);
        if (value.isPresent())
            requireNonEmptyString(value.get(), message);
    }

    private void requireNonEmptyString(String value, String message) {
        Objects.requireNonNull(value, message);
        if (value.trim().isEmpty())
            throw new IllegalArgumentException(message + ", but was '" + value + "'");
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if ( ! other.getClass().equals(this.getClass())) return false;
        return ((Node)other).id.equals(this.id);
    }

    @Override
    public String toString() {
        return state + " node " +
               (hostname !=null ? hostname : id) +
               (allocation.isPresent() ? " " + allocation.get() : "") +
               (parentHostname.isPresent() ? " [on: " + parentHostname.get() + "]" : "");
    }

    public enum State {

        /** This node has been requested (from OpenStack) but is not yet ready for use */
        provisioned,

        /** This node is free and ready for use */
        ready,

        /** This node has been reserved by an application but is not yet used by it */
        reserved,

        /** This node is in active use by an application */
        active,

        /** This node has been used by an application, is still allocated to it and retains the data needed for its allocated role */
        inactive,

        /** This node is not allocated to an application but may contain data which must be cleaned before it is ready */
        dirty,

        /** This node has failed and must be repaired or removed. The node retains any allocation data for diagnosis. */
        failed,

        /** 
         * This node should not currently be used. 
         * This state follows the same rules as failed except that it will never be automatically moved out of 
         * this state.
         */
        parked;

        /** Returns whether this is a state where the node is assigned to an application */
        public boolean isAllocated() {
            return this == reserved || this == active || this == inactive || this == failed || this == parked;
        }
    }

}
