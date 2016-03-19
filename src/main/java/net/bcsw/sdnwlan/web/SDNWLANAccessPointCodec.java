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
package net.bcsw.sdnwlan.web;

import static com.google.common.base.Preconditions.checkNotNull;

import net.bcsw.sdnwlan.IngressVlans;
import net.bcsw.sdnwlan.config.AccessPointConfig;
import net.bcsw.sdnwlan.IpGatewayAndMask;
import net.bcsw.sdnwlan.config.GatewayConfig;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.ConnectPoint;

import java.util.List;
import java.util.Map;

/**
 * Codec for encoding an Access Point entry to JSON
 * <p>
 * TODO: Currently we are not fully supporting the REST interface.
 * More work needed here when we do
 */
public class SDNWLANAccessPointCodec extends JsonCodec<AccessPointConfig> {

    // JSON field names
    private static final String NAME = "name";
    private static final String MAC_ADDRESS = "mac";
    private static final String CONNECTIONS = "connections";
    private static final String DEFAULT_GATEWAYS = "defaultGateways";
    private static final String LONGITUDE = "longitude";
    private static final String LATITUDE = "latitude";
    private static final String ALTITUDE = "altitude";
    private static final String OTHER_VIDS = "otherVids";
    private static final String OTHER_VIDS_GATEWAY = "gateway";
    private static final String OTHER_VIDS_VLANS = "vlans";

    @Override
    public ObjectNode encode(AccessPointConfig entry, CodecContext context) {
        checkNotNull(entry, "Access Point Entry cannot be null");

        final ObjectNode result = context.mapper().createObjectNode().put(NAME, entry.getName());
        result.put(MAC_ADDRESS, entry.getMacAddress().toString());

        // TODO: Need to redo this for new DefaultGatway class
        final ArrayNode jsonCIDRs = result.putArray(DEFAULT_GATEWAYS);
        entry.getDefaultGatewayList().forEach(ip -> jsonCIDRs.add(ip.toString()));

        final ArrayNode jsonConns = result.putArray(CONNECTIONS);
        entry.getConnections().forEach(cp -> jsonConns.add(cp.toString()));

        // TODO: Need to redo this for new ingress VLAN descriptor and gateway classes

        result.put(LONGITUDE, entry.getLongitude());
        result.put(LATITUDE, entry.getLatitude());
        result.put(ALTITUDE, entry.getAltitude());

        final ArrayNode jsonOthers = result.putArray(OTHER_VIDS);
        entry.getOtherVids().forEach((k, v) -> {
            result.put(OTHER_VIDS_GATEWAY, k.toString());
            final ArrayNode otherVlans = result.putArray(OTHER_VIDS_VLANS);
            v.forEach(vid -> otherVlans.add(vid.toShort()));
        });
        return result;
    }

    @Override
    public AccessPointConfig decode(ObjectNode json, CodecContext context) {
        String name = json.path(NAME).asText("");
        double longitude = json.path(LONGITUDE).asDouble(0.0);
        double latitude = json.path(LATITUDE).asDouble(0.0);
        double altitude = json.path(ALTITUDE).asDouble(0.0);
        MacAddress mac = MacAddress.valueOf(json.path(MAC_ADDRESS).asText());

        // TODO: Need to implement these
        // TODO: Some range/error checks would also be good

        List<IpGatewayAndMask> cidr = Lists.newArrayList();
        Map<IpGatewayAndMask, List<VlanId>> others = Maps.newHashMap();
        List<VlanId> vlans = Lists.newArrayList();
        List<ConnectPoint> conns = Lists.newArrayList();

        // TODO: This needs work as well
        IngressVlans ingrVlans = new IngressVlans();
        GatewayConfig gwConfig = new GatewayConfig("TODO", IpGatewayAndMask.valueOf("127.0.0.1/24"), ingrVlans);
        List<GatewayConfig> gateways = Lists.newArrayList();
        gateways.add(gwConfig);

        // TODO: JSON support for VLAN descriptor not yet supported

        IngressVlans dummy = new IngressVlans();

        return new AccessPointConfig(name, mac, ingrVlans, gateways, longitude,
                latitude, altitude, others, conns);
    }
}
