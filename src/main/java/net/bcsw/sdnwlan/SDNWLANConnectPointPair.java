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

import org.onlab.packet.IpPrefix;

import java.util.Objects;

/**
 * Class to track two connection points as a pair. Used to do intents/flows
 */
public class SDNWLANConnectPointPair {

    private final SDNWLANConnectPoint hostAccessPoint;        // Closest to the host
    private final SDNWLANConnectPoint remoteAccessPoint;      // The other end of the line
    private final IpPrefix subnet;

    // TODO: probably want to include IP Subnet information here.  The conn points above
    //       already account for switch endpoint + vlan.
    public SDNWLANConnectPointPair(SDNWLANConnectPoint hostAccessPoint,
                                   SDNWLANConnectPoint remoteAccessPoint,
                                   IpPrefix          subnet) {
        this.hostAccessPoint   = hostAccessPoint;
        this.remoteAccessPoint = remoteAccessPoint;
        this.subnet            = subnet;
    }
    public SDNWLANConnectPoint getHostAccessPoint()   { return hostAccessPoint; }
    public SDNWLANConnectPoint getRemoteAccessPoint() { return remoteAccessPoint; }
    public IpPrefix          getIpSubnet()          { return subnet; }

    @Override
    public int hashCode() {
        return Objects.hash(new Object[]{ hostAccessPoint, remoteAccessPoint, subnet });
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if( obj != null && obj instanceof SDNWLANConnectPointPair) {
            SDNWLANConnectPointPair other = (SDNWLANConnectPointPair) obj;
            return this.subnet.equals(other.subnet) &&
                   this.hostAccessPoint.equals(other.hostAccessPoint) &&
                   this.remoteAccessPoint.equals(other.remoteAccessPoint);
        }
        return false;
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();

        builder.append(String.format("%s - %s, %s",
                hostAccessPoint.toString(), remoteAccessPoint.toString(),
                subnet.toString()));

        return builder.toString();
    }
}
