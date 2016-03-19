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

import net.bcsw.sdnwlan.flows.NorthBoundFlow;
import net.bcsw.sdnwlan.flows.NorthBoundMacFlow;
import net.bcsw.sdnwlan.flows.SouthBoundFlow;
import net.bcsw.sdnwlan.flows.SouthBoundMacFlow;
import com.google.common.collect.Sets;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onosproject.net.Host;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contains information specific to a Host that is part of the sdnWLAN network.
 *
 * Note only the MAC address is considered to be unique and constant for a mobile host
 */
public class MobileHost extends SDNWLANHostInfo {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final IntentService intentService;

    private Set<AccessPoint> homeAccessPoints = Sets.newConcurrentHashSet();
    private AccessPoint currentAccessPoint;

    // Our roaming flows for this host
    private Map<SDNWLANConnectPointPair, Intent> northboundIntentKeys = new ConcurrentHashMap<>();
    private Map<SDNWLANConnectPointPair, Intent> southboundIntentKeys = new ConcurrentHashMap<>();

    // TODO: Useful stats would be nice.  Separate interface/class?

    public MobileHost(Host info, Set<AccessPoint> home) {
        super(info);

        homeAccessPoints   = home;
        currentAccessPoint = homeAccessPoints.size() > 0 ? homeAccessPoints.iterator().next() : null;

        ServiceDirectory serviceDirectory = new DefaultServiceDirectory();
        this.intentService = serviceDirectory.get(IntentService.class);
    }

    public MobileHost(Host info, Set<AccessPoint> home, AccessPoint current) {

        homeAccessPoints   = home;
        currentAccessPoint = current;

        ServiceDirectory serviceDirectory = new DefaultServiceDirectory();
        this.intentService = serviceDirectory.get(IntentService.class);
    }

    /**
     * Update host information
     *
     * @param info New information
     */
    @Override
    public void updateHost(Host info) {
        // TODO: Implement
    }

    /**
     * Get the set of home access points this host belongs too
     * @return
     */
    public Set<AccessPoint> getHomeAccessPoints() {
        return homeAccessPoints;
    }

    /**
     * Currently, we will only support a single access point associated with a host.
     * We may want to change that in the future...
     *
     * @return Home AccessPoint
     */
    public AccessPoint getHomeAccessPoint() {
        return homeAccessPoints.size() > 0 ? homeAccessPoints.iterator().next() : null;
    }

    /**
     *
     *
     * @return Current AccessPoint
     */
    public AccessPoint getCurrentAccessPoint() {
        return currentAccessPoint;
    }

    public void setCurrentAccessPoint(AccessPoint point) {
        if (currentAccessPoint != point) {

            // Cleanup old flows (if we were roaming)

            if (currentAccessPoint != getHomeAccessPoint()) {
                currentAccessPoint.removeHost(this);
            }
            currentAccessPoint = point;
            lastMoveTime       = date.getTime();
            timesMoved++;

            // Set new flows if needed

            if (currentAccessPoint != null) {
                currentAccessPoint.addHost(this);
            }
        }
    }

    /**
     * Is this host currently roaming away from its home AP?
     *
     * @return true if roaming
     */
    public boolean isRoaming() {
        return (currentAccessPoint != null) && !homeAccessPoints.contains(currentAccessPoint);
    }

    /**
     * Add roaming unicast flows for this host
     *
     *   TODO: For both default gateway and home AP flows, support learning of these locations later and
     *         have this called again if needed...  Probably we will only care about learning of default
     *         gateway AP since we may need to know the 'home AP' to create a Mobile Host.  Verify this
     *         is (and will be) always true.
     */
    public void addRoamingUnicastFlows() {

        log.info("addRoamingUnicastFlows: {}", this.toString());

        addRoamingUnicastDefaultGatewayFlows();
        addRoamingUnicastHomeAPFlows();
    }

    /**
     * Add unicast traffic flows between the roaming host and its default gateway
     */
    private void addRoamingUnicastDefaultGatewayFlows() {

        log.info("addUnicastDefaultGatewayFlows: {}", this.toString());

        SDNWLANConnectPoint apPoint = new SDNWLANConnectPoint(currentAccessPoint, getHomeAccessPoint());

        for (DefaultGateway gateway : this.getHomeAccessPoint().getDefaultGateways().values()) {

            SDNWLANConnectPoint gwPoint = new SDNWLANConnectPoint(gateway);

            // The location of the default gateway may not yet be known we may only have its
            // IP Address.  If that is true, we must wait to learn the address before we can
            // install the flow.

            if (gwPoint.getLocation() == null) {
                log.info("addDefaultGatewayFlows: unknown default gateway location");
                break;
            }
            SDNWLANConnectPointPair pair = new SDNWLANConnectPointPair(apPoint, gwPoint,
                                                gateway.getGatewayConfig().getGatewayAndMask().getIpPrefix());

            // Insert southbound flows first since we want data to the customer as soon as possible

            SouthBoundFlow sbFlow = new SouthBoundMacFlow(APManager.appId, gwPoint,
                                                          apPoint, getMacAddress());
            Intent southboundIntent = sbFlow.getIntent(APManager.roamingUnicastPriority);

            if (southboundIntentKeys.putIfAbsent(pair, southboundIntent) == null) {
                intentService.submit(southboundIntent);
            }
            NorthBoundFlow nbFlow = new NorthBoundMacFlow(APManager.appId, apPoint, gwPoint,
                                                          gateway.getMacAddress());
            Intent northboundIntent = nbFlow.getIntent(APManager.roamingUnicastPriority);

            if (northboundIntentKeys.putIfAbsent(pair, northboundIntent) == null) {
                intentService.submit(northboundIntent);
            }
        }
    }

    /**
     * Add unicast traffic flows between the roaming host and its home access point
     */
    private void addRoamingUnicastHomeAPFlows() {

        log.info("addUnicastHomeAPFlows: {}", this.toString());
        // TODO: Implement this
    }

    /**
     * Add roaming broadcast/multicast flows for this host
     */
    public void addRoamingBroadcastMulticastFlows() {

        log.info("addBroadcastMulticastFlows: {}", this.toString());

        // TODO: Implement this
    }

    /**
     * Drop any roaming flows for this host
     */
    public void dropRoamingFlows() {

        log.info("dropRoamingFlows: {}", this.toString());

        northboundIntentKeys.forEach((pair, intent) -> intentService.withdraw(intent));
        southboundIntentKeys.forEach((pair, intent) -> intentService.withdraw(intent));
    }

    /**
     * Utility method for shorter 'Host' string output
     *
     * @param host Host to display
     *
     * @return Short string for the host
     */
    public static String hostToString(Host host) {
        if (host == null) {
            return "null";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%s - %s/%d, ip: [", host.id().toString(),
                host.location().deviceId().toString(),
                host.location().port().toLong()));

        Iterator<IpAddress> ipItor = host.ipAddresses().iterator();
        while (ipItor.hasNext()) {
            builder.append(ipItor.next().toString());
            if (ipItor.hasNext()) {
                builder.append(",");
            }
        }
        builder.append("]");
        return builder.toString();
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();

        builder.append(String.format("%s (%s)",
                super.toString(),
                isRoaming() ? "ROAM" : "HOME"));

        return builder.toString();
    }
}
