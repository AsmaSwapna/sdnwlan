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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import org.onlab.packet.VlanId;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Class to support describing the VLAN headers on packets ingressing the SDN network
 * from either an Access Point or Default Gateway connect point.  By itself, it will
 * describe the VLAN matching criteria (if any) for a specific point.  When combined
 * with the other ingress endpoint, the VLAN actions to perform (n/a, push, pop, swap, ...)
 * can be determined.
 */
public class IngressVlans {

    public  static final String INGRESS_VLANS = "ingressVlans";

    private final List<VlanId> vlanList = Lists.newArrayList();

    public IngressVlans() {
    }

    /**
     * Create new ingress VLAN object
     *
     *   The input VLAN list is arranged with outermost tag first
     *
     * @param vids List of VLANS
     */
    public IngressVlans(List<VlanId> vids) {
        // TODO: Range check on VLAN would be good here...
        if (vids != null) {
            vids.forEach(vlan -> {
                if (!vlan.equals(VlanId.NONE)) {
                    vlanList.add(vlan);
                }
            });
        }
    }

    public IngressVlans(VlanId vlan) {
        // TODO: Range check on VLAN would be good here...

        if (!vlan.equals(VlanId.NONE)) { vlanList.add(vlan); }
    }

    /**
     * Create Ingress VLAN list given an STag & CTag
     *
     *   The input VLAN list is arranged with outermost tag first
     *
     * @param stag STAG
     * @param ctag CTAG
     */
    public IngressVlans(VlanId stag, VlanId ctag) {
        // TODO: Range check on VLAN would be good here...
        // TODO: what about STPID ?

        if (!stag.equals(VlanId.NONE)) { vlanList.add(stag); }
        if (!ctag.equals(VlanId.NONE)) { vlanList.add(ctag); }
    }

    public IngressVlans(ArrayNode vids) {
        if (vids != null) {
            vids.forEach(jsonNode ->
                    vlanList.add(VlanId.vlanId((short) jsonNode.asInt())));
        }
    }

    /**
     * Build up the traffic treatment and selector for a flow's vlans
     *
     * @param selector
     * @param ingress
     * @param egress
     * @return
     */
    public static TrafficTreatment.Builder vlanMatchAndTreatment(TrafficSelector.Builder selector,
                                                                 SDNWLANConnectPoint ingress,
                                                                 SDNWLANConnectPoint egress) {

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        // Always match any ingress vlans. The input VLAN list is arranged with outermost tag first

        List<VlanId> ingressVlans = ingress.getIngressVlans().getVlanList();

        ingressVlans.forEach(vid -> selector.matchVlanId(vid));

        // If egress vlans not equal to ingress, some type of action is required

        List<VlanId> egressVlans = egress.getIngressVlans().getVlanList();

        if (!egressVlans.equals(ingressVlans)) {

            // Do simple push/pop all checks cases first

            if (ingressVlans.size() == 0) {

                // Push all egress vlans

                egressVlans.forEach(vid -> treatment.pushVlan().setVlanId(vid));

            } else if (egressVlans.size() == 0) {

                // Pop all ingress vlans

                ingressVlans.forEach(vid -> treatment.popVlan());

            } else {
                // TODO: Implement this more complex case
            }
        }
        return treatment;
    }

    /**
     * Get the list of vlan tags for this descriptor
     *
     * @return List of VIDs
     */
    public List<VlanId> getVlanList()  { return Collections.unmodifiableList(vlanList); }

    @Override
    public int hashCode() {
        return Objects.hash(new Object[] { vlanList });
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if( obj != null && obj instanceof IngressVlans) {
            IngressVlans other = (IngressVlans)obj;

            return this.vlanList.equals(other.vlanList);
        }
        return false;
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();

        for (int pos = 0; pos < vlanList.size(); pos++) {
            builder.append(String.format("%d", vlanList.get(pos).toShort()));
            if (pos != vlanList.size() -1) builder.append("/");
        }
        return builder.toString();
    }
}
