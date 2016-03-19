package net.bcsw.sdnwlan.flows;

import net.bcsw.sdnwlan.IngressVlans;
import net.bcsw.sdnwlan.SDNWLANConnectPoint;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.PointToPointIntent;

/**
 * A northbound flow that matches on destination MAC address.
 * <p>
 * If present, ingress VLANS are also checked and can optionally be discarded after initial match
 */
public class NorthBoundMacFlow extends NorthBoundFlow {

    private MacAddress macAddress;

    public NorthBoundMacFlow(ApplicationId appId,
                             SDNWLANConnectPoint sbPoint,
                             SDNWLANConnectPoint nbPoint,
                             MacAddress macAddress) {
        super(appId, sbPoint, nbPoint, macAddress);

        this.macAddress = macAddress;
    }

    @Override
    public String name() {
        return "NorthBound Destination MAC Flow";
    }

    @Override
    public String description() {
        return "Northbound flow that matches on the destination MAC address and " +
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
        // Northbound.  Match of Gateway MAC address and any VLAN ID's

        if ((macAddress != MacAddress.ZERO) && (macAddress != MacAddress.NONE)) {

            TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
                    .matchEthDst(macAddress);

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
        return null;
    }
}
