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

package net.bcsw.sdnwlan.flows;

import net.bcsw.sdnwlan.IngressVlans;
import net.bcsw.sdnwlan.SDNWLANConnectPoint;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpPrefix;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.PointToPointIntent;

import java.util.Objects;

/**
 * A Southbound flow that matches on a destination IP Subnet.
 * <p>
 * Optionally all incoming VLAN tags are stripped and traffic within
 * the flow is untagged.
 */
public class SouthBoundIpSubnetFlow extends SouthBoundFlow {

    private IpPrefix ipPrefix;

    public SouthBoundIpSubnetFlow(ApplicationId appId,
                                  SDNWLANConnectPoint nbPoint,
                                  SDNWLANConnectPoint sbPoint,
                                  IpPrefix ipPrefix) {
        super(appId, nbPoint, sbPoint, ipPrefix);

        this.ipPrefix = ipPrefix;
    }

    /**
     * Return the short name for the implemented Flow
     */
    @Override
    public String name() {
        return "SouthBound IpSubnet Flow";
    }

    /**
     * Return the description for the implemented Flow
     */
    @Override
    public String description() {
        return "Southbound flow that matches on IP Subnet destination and " +
                "any optional VLAN tags ";
    }

    /**
     * Get the intent object that represents this flow
     *
     * @param flowPriority Priority for the intent
     * @return Intent ready for submission or 'null' on error
     */
    @Override
    public Intent getIntent(int flowPriority) {

        ////////////////////////////////////////////////
        // Southbound.  Match of Egress AP IpPrefix

        short ipType = ipPrefix.isIp6() ? Ethernet.TYPE_IPV6 : Ethernet.TYPE_IPV4;

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
                .matchEthType(ipType)
                .matchIPDst(ipPrefix);

        TrafficTreatment.Builder treatment = IngressVlans.vlanMatchAndTreatment(selector, ingress, egress);

        return PointToPointIntent.builder()
                .appId(getApplicationId())
                .selector(selector.build())
                .ingressPoint(ingress.getLocation())
                .egressPoint(egress.getLocation())
                .treatment(treatment.build())
                .priority(flowPriority)
                .build();
    }

    @Override
    public int hashCode() {
        return Objects.hash(new Object[]{super.hashCode(), ipPrefix});
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj != null && obj instanceof SouthBoundIpSubnetFlow) {
            SouthBoundIpSubnetFlow other = (SouthBoundIpSubnetFlow) obj;

            return super.equals(other) &&
                    this.ipPrefix.equals(other.ipPrefix);
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(super.toString()).append(" / ");

        builder.append(this.ipPrefix.toString());

        return builder.toString();
    }
}
