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
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Wraps the JSON configuration data for a gateway
 */
public class GatewayConfig {
    public static final String GATEWAY_CONFIG = "gateways";
    private static final String NAME = "name";
    private static final String SUBNET_AND_ADDRESS = "subnetAndAddress";
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private String name;
    private IpGatewayAndMask gatewayAndMask;
    private IngressVlans ingressVlans;

    protected GatewayConfig() {
    }

    protected GatewayConfig(GatewayConfig rhs) {
        this.name = rhs.getName();
        this.gatewayAndMask = rhs.getGatewayAndMask();
        this.ingressVlans = rhs.getIngressVlans();
    }

    public GatewayConfig(String name, IpGatewayAndMask gatewayAndMask) {
        this.name = name;
        this.gatewayAndMask = gatewayAndMask;
        this.ingressVlans = new IngressVlans();
    }

    public GatewayConfig(String name, IpGatewayAndMask gatewayAndMask,
                         IngressVlans ingressVlans) {
        this.name = name;
        this.gatewayAndMask = gatewayAndMask;
        this.ingressVlans = ingressVlans;
    }

    public static GatewayConfig valueOf(JsonNode gwNode) throws ConfigException {
        try {
            String name = gwNode.path(NAME).asText("").trim();
            IpGatewayAndMask addr = IpGatewayAndMask.valueOf(gwNode.path(SUBNET_AND_ADDRESS).asText().trim());
            IngressVlans ingressVlans = new IngressVlans();

            if (gwNode.has(IngressVlans.INGRESS_VLANS)) {
                ingressVlans = new IngressVlans((ArrayNode) gwNode.path(IngressVlans.INGRESS_VLANS));
            }
            return new GatewayConfig(name, addr, ingressVlans);

        } catch (IllegalArgumentException e) {
            throw new ConfigException("Error parsing compute node connections", e);
        }
    }

    public String getName() {
        return name;
    }

    public IpGatewayAndMask getGatewayAndMask() {
        return gatewayAndMask;
    }

    public IngressVlans getIngressVlans() {
        return ingressVlans;
    }
}
