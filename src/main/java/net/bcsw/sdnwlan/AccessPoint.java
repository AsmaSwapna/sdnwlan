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

import net.bcsw.sdnwlan.config.AccessPointConfig;
import com.google.common.collect.Maps;
import net.bcsw.sdnwlan.flows.NorthBoundFlow;
import net.bcsw.sdnwlan.flows.NorthBoundMacFlow;
import net.bcsw.sdnwlan.flows.SouthBoundFlow;
import net.bcsw.sdnwlan.flows.SouthBoundIpSubnetFlow;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.HostLocation;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contains information for a specific sdnWLAN access point
 *
 * At this time, this is the first point of entry into the SDN network where a BSAP is connected
 */
public class AccessPoint extends AccessPointConfig
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final IntentService intentService;

    private Map<SDNWLANConnectPointPair, Intent> northboundDefaultGatewayIntentKeys = new ConcurrentHashMap<>();
    private Map<SDNWLANConnectPointPair, Intent> southboundDefaultGatewayIntentKeys = new ConcurrentHashMap<>();

    // Default gateways for this access point

    private Map<IpGatewayAndMask, DefaultGateway> defaultGateways = Maps.newConcurrentMap();

    // List of mobile hosts (separate for current and 'home' hosts?)

    private Map<MacAddress, MobileHost> apHosts      = Maps.newConcurrentMap();
    private Map<MacAddress, MobileHost> roamingHosts = Maps.newConcurrentMap();

    // TODO: Any 802.1x credentials needed for connection to network?
    // TODO: Should we support the new Network configuration subsystem for the AP itself?
    // TODO: Useful stats would be nice.  Separate interface/class?

    public AccessPoint(AccessPointConfig configEntry) {
        super(configEntry);

        ServiceDirectory serviceDirectory = new DefaultServiceDirectory();
        this.intentService = serviceDirectory.get(IntentService.class);

        // Construct default gateways for this AP

        getDefaultGatewayList().forEach(gwConfig ->
                this.defaultGateways.put(gwConfig.getGatewayAndMask(),
                                         new DefaultGateway(gwConfig)));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        if (!this.getName().isEmpty()) {
            builder.append(String.format("[%s]: ", this.getName()));
        }
        builder.append(String.format("%s/", this.getMacAddress().toString()));

        builder.append(String.format("%s/", getIngressVlans().toString()));

        builder.append(", gw: ");
        if (getDefaultGateways().size() == 0) {
            builder.append("none");
        } else {
            Iterator<IpGatewayAndMask> gateways = getDefaultGateways().keySet().iterator();

            while (gateways.hasNext()) {
                builder.append(String.format("%s", gateways.next().toString()));
                if (gateways.hasNext()) builder.append(", ");
            }
        }
        List<ConnectPoint> points = this.getConnections();

        builder.append(", cp: ");
        if (points.size() == 0) {
            builder.append("none");
        } else {
            for (int pos = 0; pos < points.size(); pos++) {
                ConnectPoint point = points.get(pos);
                builder.append(String.format("%s/%d", point.deviceId().toString(),
                        point.port().toLong()));
                if (pos != points.size() -1) builder.append(",");
            }
        }
        return builder.toString();
    }

    /**
     * Get the collection of default gateways for this AP
     *
     * @return default gateway collection
     */
    public Map<IpGatewayAndMask, DefaultGateway> getDefaultGateways() {
        return Collections.unmodifiableMap(defaultGateways);
    }

    /**
     * Update the configuration for this Access point
     *
     * @param config new configuration
     */
    public void updateConfig(AccessPointConfig config) {

        log.info("updateConfig: {}", this.getMacAddress().toString());

        // TODO: Implement this.  Compare new config with old config...
    }

    /**
     * Process startup work for this access point.
     *
     *  o Called when access point added to network
     *
     *  o Called when an IP address for any of the default gateways is discovered (HOST_ADDED event).
     */
    public void onStartup() {

        log.info("onStartup: {}", this.toString());

        // Set up the default gateway northbound & Southbound flows

        getDefaultGateways().values().stream().forEach(gw -> addDefaultGatewayFlows(gw));

        // TODO: Anything else


        // TODO: For many of out 'stream' operations on gateways, access points, and mobile hosts, see if
        // it converting some of them to parallel streams would be of help.  This is not just hear but in
        // the whole application.
    }

    /**
     * Process shutdown for this access point
     *
     *   Called when access point removed from the network
     */
    public void onShutdown() {
        log.info("onShutdown: {}", this.toString());

        dropAllFlows();

        // TODO: Anything else
    }

    /**
     * Add the default flows for a specific AP connection point and an upstream
     * default gateway.
     *
     *   The method should check for the gateway connection point existence.  It may
     *   not have been found yet.
     *
     * @param gateway      The default gateway
     */
    private void addDefaultGatewayFlows(DefaultGateway gateway) {

        // TODO: Should this be moved to the default gateway object?

        log.info("addDefaultGatewayFlows: {} / {}", this.getMacAddress().toString(),
                gateway.toString());

        SDNWLANConnectPoint apPoint = new SDNWLANConnectPoint(this);
        SDNWLANConnectPoint gwPoint = new SDNWLANConnectPoint(gateway);

        // The location of the default gateway may not yet be known we may only have its
        // IP Address.  If that is true, we must wait to learn the address before we can
        // install the flow.

        if (gwPoint.getLocation() == HostLocation.NONE) {
            log.info("addDefaultGatewayFlows: unknown default gateway location");
            return;

        } else if (gateway.setDefaultFlowsInstalled(true)) {

            ////////////////////////////////////////////////
            // Northbound.  Match of Gateway MAC address and any VLAN ID's, preserve any
            //              vlan(s) after matching them
            //
            // Southbound.  Match on the IP subnet and any VLAN ID's.
            //
            // Calculate the flows and see if they already exist, if not, add them.  This will
            // allow this routine to be called at any time to add in any missing or new flows
            // to the default gateway

            SDNWLANConnectPointPair pair = new SDNWLANConnectPointPair(apPoint, gwPoint,
                    gateway.getGatewayConfig().getGatewayAndMask().getIpPrefix());

            // Insert southbound flows first since we want data to the customer as soon as possible

            SouthBoundFlow sbFlow = new SouthBoundIpSubnetFlow(APManager.appId, gwPoint, apPoint,
                    gateway.getGatewayConfig().getGatewayAndMask().getIpPrefix());

            Intent southboundIntent = sbFlow.getIntent(APManager.gatewayFlowPriority);

            if (southboundDefaultGatewayIntentKeys.putIfAbsent(pair, southboundIntent) == null) {
                intentService.submit(southboundIntent);
            }
            NorthBoundFlow nbFlow = new NorthBoundMacFlow(APManager.appId, apPoint,
                    gwPoint, gateway.getMacAddress());

            Intent northboundIntent = nbFlow.getIntent(APManager.gatewayFlowPriority);

            if (northboundDefaultGatewayIntentKeys.putIfAbsent(pair, northboundIntent) == null) {
                intentService.submit(northboundIntent);
            }
        }
    }

    /**
     * Drop northbound and southbound flows from this Access Point with the specified gateway
     *
     * @param gateway Gateway to drop flows from/to.
     */
    private void dropDefaultGatewayFlows(DefaultGateway gateway) {

        log.info("dropDefaultGatewayFlows: {} / {} - Installed: {}", this.getMacAddress().toString(),
                gateway.toString(), gateway.isDefaultFlowsInstalled() ? "True" : "False");

        if (gateway.setDefaultFlowsInstalled(false)) {
            SDNWLANConnectPointPair pair =
                    new SDNWLANConnectPointPair(new SDNWLANConnectPoint(this),
                                              new SDNWLANConnectPoint(gateway),
                                              gateway.getGatewayConfig().getGatewayAndMask().getIpPrefix());

            Intent northBoundIntent = northboundDefaultGatewayIntentKeys.remove(pair);

            if (northBoundIntent != null) {
                intentService.withdraw(northBoundIntent);
            }
            Intent southBoundIntent = southboundDefaultGatewayIntentKeys.remove(pair);

            if (northBoundIntent != null) {
                intentService.withdraw(southBoundIntent);
            }
        }
    }

    /**
     * Add the roaming flows for a mobile host
     *
     * TODO: Support detection of a host without an IP address and then a following HOST_UPDATE with IP
     * TODO: Support a host loosing it's IP address or it completely changing (but assume same subnet?)
     *
     * @param host Host to add flows for
     */
    public void addRoamingFlows(MobileHost host) {

        log.info("addRoamingFlows: {}", host.toString());

        host.addRoamingUnicastFlows();
        host.addRoamingBroadcastMulticastFlows();
    }

    public void dropRoamingFlows(MobileHost host) {

        log.info("dropRoamingFlows: {}", host.toString());
        host.dropRoamingFlows();
    }

    /**
     * Drop all flows related to this AP.
     *
     *   This includes default flows, flows owned by this AP's roaming hosts, and any
     *   hosts from other APs that have accessing the network via this AP.
     */
    public void dropAllFlows() {

        log.info("dropAllFlows: {}", this.getMacAddress().toString());

        // Tear down the default gateway northbound & Southbound flows

        getDefaultGateways().values().stream().forEach(gw -> dropDefaultGatewayFlows(gw));
        getRoamingHosts().values().stream().forEach(host -> dropRoamingFlows(host));
    }

    /**
     * Add a host (local or roaming) to this access point.
     *
     *   When a host is added to an AP (mainly if roaming), it will set up any roaming flows
     *   at this point.  The teardown of any roaming flows is done when the AP is removed.
     *
     * @param host Host to add
     */
    public void addHost(MobileHost host) {

        MacAddress mac = host.getMacAddress();

        log.info("addHost({}): host: {} {}roaming",
                this.getMacAddress().toString(), mac.toString(),
                host.isRoaming() ? "" : "is not ");

        if (host.isRoaming()) {
            // See if already in roaming host...

            if (!roamingHosts.containsKey(mac)) {
                roamingHosts.putIfAbsent(mac, host);

                addRoamingFlows(host);
            }

        } else {
            // Add to local hosts if not already present. This method is called even for
            // the 'home' AP when we go back, so that is why we check to see if it is
            // already present.

            if (!apHosts.containsKey(mac)) {

                apHosts.put(mac, host);

                // Default gateway flows already exist for all 'homed' hosts.  So nothing
                // else to do here.
            }
        }
    }

    /**
     * Remove a host (local or roaming) from this access point.
     *
     *   When a host is added to an AP (mainly if roaming), it will set up any roaming flows
     *   at this point.  The teardown of any roaming flows is done when the AP is removed.
     *
     * @param host Host to remove
     */
    public void removeHost(MobileHost host) {

        MacAddress mac = host.getMacAddress();

        if (roamingHosts.containsKey(mac)) {
            log.info("removeHost: remove roaming host {} from roamed AP {}",
                    mac.toString(), this.getMacAddress().toString());

            dropRoamingFlows(host);

            roamingHosts.remove(mac);
        }
        if (apHosts.containsKey(mac)) {
            log.info("removeHost: remove host {} from home AP {}",
                    mac.toString(), this.getMacAddress().toString());

            apHosts.remove(mac);
        }
    }

    /**
     * Get a collection of local hosts for this AP
     *
     * @return Map of local hosts
     */
    public Map<MacAddress, MobileHost> getLocalHosts() {
        return Collections.unmodifiableMap(apHosts);
    }

    /**
     * Get a collection of roaming hosts for this AP
     *
     * @return Map of roaming hosts
     */
    public Map<MacAddress, MobileHost> getRoamingHosts() {
        return Collections.unmodifiableMap(roamingHosts);
    }
}
