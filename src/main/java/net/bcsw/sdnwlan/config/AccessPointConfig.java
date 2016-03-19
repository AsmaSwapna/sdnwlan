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
package net.bcsw.sdnwlan.config;

import net.bcsw.sdnwlan.IngressVlans;
import net.bcsw.sdnwlan.IpGatewayAndMask;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.net.ConnectPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by cboling on 12/12/15.
 */
public class AccessPointConfig {
    private static final String NAME = "name";
    private static final String MAC_ADDRESS = "mac";
    private static final String CONNECTIONS = "connections";
    private static final String LONGITUDE = "longitude";
    private static final String LATITUDE = "latitude";
    private static final String ALTITUDE = "altitude";
    private static final String OTHER_VIDS = "otherVids";
    private static final String OTHER_VIDS_GATEWAY = "gateway";
    private static final String OTHER_VIDS_VLANS = "vlans";
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private String name;
    private MacAddress macAddress;
    private IngressVlans ingressVlans;
    private double longitude;
    private double latitude;
    private double altitude;
    private Map<IpGatewayAndMask, List<VlanId>> otherVids;
    private List<ConnectPoint> connections;
    private List<GatewayConfig> defaultGateways;


    protected AccessPointConfig() {
    }

    protected AccessPointConfig(AccessPointConfig rhs) {
        this.name = rhs.getName();
        this.macAddress = rhs.getMacAddress();
        this.ingressVlans = rhs.getIngressVlans();
        this.defaultGateways = rhs.getDefaultGatewayList();
        this.longitude = rhs.getLongitude();
        this.latitude = rhs.getLatitude();
        this.altitude = rhs.getAltitude();
        this.otherVids = rhs.getOtherVids();
        this.connections = rhs.getConnections();

    }

    public AccessPointConfig(String name, MacAddress macAddress,
                             IngressVlans ingressVlans,
                             List<GatewayConfig> defaultGateways,
                             double longitude, double latitude, double altitude,
                             Map<IpGatewayAndMask, List<VlanId>> otherVids,
                             List<ConnectPoint> connections) {
        this.name = name;
        this.macAddress = macAddress;
        this.ingressVlans = ingressVlans;
        this.longitude = longitude;
        this.latitude = latitude;
        this.altitude = altitude;
        this.otherVids = otherVids;
        this.connections = connections;
        this.defaultGateways = Lists.newArrayList(defaultGateways);
    }

    public static AccessPointConfig valueOf(JsonNode accessNode) throws ConfigException {
        try {
            // First some required values
            MacAddress macAddress = MacAddress.valueOf(accessNode.path(MAC_ADDRESS).asText());

            if (macAddress == null || macAddress.equals(MacAddress.ZERO)) {
                // TODO: Could stand a few more checks here...
                throw new IllegalArgumentException("Invalid MAC Address");
            }
            List<ConnectPoint> connections = Lists.newArrayList();

            JsonNode gateways = accessNode.get(CONNECTIONS);
            if (gateways != null) {
                gateways.forEach(jsonNode ->
                        connections.add(ConnectPoint.deviceConnectPoint(jsonNode.asText())));
            }
            if (connections.size() == 0) {
                throw new IllegalArgumentException("Invalid/missing access point connnection");
            }
            List<GatewayConfig> defaultGateways = Lists.newArrayList();

            if (accessNode.has(GatewayConfig.GATEWAY_CONFIG)) {
                ArrayNode gwConfigArray = (ArrayNode) accessNode.get(GatewayConfig.GATEWAY_CONFIG);

                gwConfigArray.forEach(gwNode -> {
                    try {
                        defaultGateways.add(GatewayConfig.valueOf(gwNode));
                    } catch (ConfigException e) {
                        throw new IllegalArgumentException("Error parsing gateway configs", e);
                    }
                });
            }
            if (defaultGateways.size() == 0) {
                throw new IllegalArgumentException("Invalid/missing default gateways");
            }
            ///////////////////////////////////////////////////////////////////////
            // Now other minor or optional values
            // TODO: Some additional error checking would be good idea here

            String name = accessNode.path(NAME).asText("").trim();
            double longitude = accessNode.path(LONGITUDE).asDouble(0.0);
            double latitude = accessNode.path(LATITUDE).asDouble(0.0);
            double altitude = accessNode.path(ALTITUDE).asDouble(0.0);

            Map<IpGatewayAndMask, List<VlanId>> otherGateways = Maps.newHashMap();

            if (accessNode.has(OTHER_VIDS)) {
                ArrayNode vidNodeArray = (ArrayNode) accessNode.path(OTHER_VIDS);

                try {
                    vidNodeArray.forEach(vidNode -> {
                        String dummy = "OtherVid entry: " + vidNode.toString();

                        IpGatewayAndMask gateway = IpGatewayAndMask.valueOf(vidNode.get(OTHER_VIDS_GATEWAY).asText());
                        List<VlanId> otherVlans = Lists.newArrayList();

                        JsonNode otherVids = vidNode.get(OTHER_VIDS_VLANS);
                        if (otherVids != null) {
                            otherVids.forEach(jsonNode ->
                                    otherVlans.add(VlanId.vlanId((short) jsonNode.asInt())));
                        }
                        if (otherGateways.put(gateway, otherVlans) != null) {
                            throw new IllegalArgumentException("Duplicate gateway in othervid list: " +
                                    gateway.toString());
                        }
                    });
                } catch (IllegalArgumentException e) {
                    throw new ConfigException("Error parsing compute node connections", e);
                }
            }
            IngressVlans ingressVlans = new IngressVlans();

            if (accessNode.has(IngressVlans.INGRESS_VLANS)) {
                ingressVlans = new IngressVlans((ArrayNode) accessNode.path(IngressVlans.INGRESS_VLANS));
            }
            return new AccessPointConfig(name, macAddress, ingressVlans, defaultGateways,
                    longitude, latitude, altitude, otherGateways, connections);

        } catch (IllegalArgumentException e) {
            throw new ConfigException("Error parsing compute node connections", e);
        }
    }

    public String getName() {
        return name;
    }

    public MacAddress getMacAddress() {
        return macAddress;
    }

    public IngressVlans getIngressVlans() {
        return ingressVlans;
    }

    public List<GatewayConfig> getDefaultGatewayList() {
        return Collections.unmodifiableList(defaultGateways);
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public Map<IpGatewayAndMask, List<VlanId>> getOtherVids() {
        return Collections.unmodifiableMap(otherVids);
    }

    public List<ConnectPoint> getConnections() {
        return Collections.unmodifiableList(connections);
    }
}
