/*
 * Copyright 2015-2016 Boling Consulting Solutions, bcsw.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bcsw.sdnwlan;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Describes an edge connection point in a sdnWLAN SDN network.
 *
 * On the southbound edge, this represents the current point of attachment for a
 * Residential Gateway and/or Access Point
 *
 * On the northbound edge, this represents the point of attachment nearest
 * to the default gateway/router.
 */
public class SDNWLANConnectPoint {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ApplicationId appId;
    // NOTE: The location may be null if passed a host location that cannot be resolved
    private ConnectPoint       location;
    private final IngressVlans ingressVlans;


    /**
     * Construct a new sdnWLAN connection point
     *
     *   Typically this is used to describe the connection point closest to the Residential Gateway/
     *   Access Point
     *
     * @param accessPoint The access point
     */
    public SDNWLANConnectPoint(AccessPoint accessPoint) {

        // TODO: Currently we only support a single connection point
        // Later, we may want redundant connection support

        this.location     = accessPoint.getConnections().get(0);
        this.ingressVlans = accessPoint.getIngressVlans();
    }

    /**
     * Construct a new sdnWLAN connection point
     *
     *   Typically this is used to describe the connection point closest to the Residential Gateway/
     *   Access Point
     *
     * @param roamingAccessPoint The access point the host roamed to
     * @param homeAccessPoint The home access point for the host
     */
    public SDNWLANConnectPoint(AccessPoint roamingAccessPoint, AccessPoint homeAccessPoint) {

        // TODO: Currently we only support a single connection point
        // Later, we may want redundant connection support

        this.location     = roamingAccessPoint.getConnections().get(0);
        this.ingressVlans = homeAccessPoint.getIngressVlans();
    }

    /**
     * Construct a new sdnWLAN connection point
     *
     *   Typically this is where a default gateway is connected
     *
     * @param gateway The default gateway
     */
    public SDNWLANConnectPoint(DefaultGateway gateway) {

        // Use the learned host connection point.  Note that this may be HostLocation.NONE until
        // the host monitoring service locates the gateway

        this.location     = gateway.hostInfo.location();
        this.ingressVlans = gateway.getGatewayConfig().getIngressVlans();
    }

    /**
     * Get the connection point for this object, may be null
     *
     * @return Connection point
     */
    public ConnectPoint getLocation() {
        return location;
    }

    public IngressVlans getIngressVlans() {
        return ingressVlans;
    }

    @Override
    public int hashCode() {
        return Objects.hash(new Object[] {
                this.getLocation(),
                this.getIngressVlans()
        });
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if( obj != null && obj instanceof SDNWLANConnectPoint) {
            SDNWLANConnectPoint other = (SDNWLANConnectPoint)obj;

            return  this.getLocation() == other.getLocation() &&
                    this.getIngressVlans().equals(other.getIngressVlans());
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (this.getLocation() == null) {
            builder.append("<unknown location>");
        } else {
            builder.append(this.getLocation().toString());
        }
        builder.append(String.format(" - %s ", ingressVlans.toString()));

        return builder.toString();
    }

    // TODO: Conntains ConnectionPoint
    // TODO: Link status (Active, Inactive, Active-Standby, ...   enum?)
    // TODO: Any subscriptions needed/available ?

}
