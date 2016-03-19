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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Maps;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.net.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

/**
 * sdnwlan application configuration class
 * <p>
 * Used to parse ONOS Application Configuration data from REST post and/or network-cfg.json file
 */
public class SDNWLANConfig extends Config<ApplicationId> {

    ////////////////////////////////////////////////////////////////////////////////
    // top-level application keys
    private static final String GATEWAY_FLOW_PRIORITY = "defGwFlowPriority";
    private static final String UNICAST_FLOW_PRIORITY = "roamingUnicastPriority";
    private static final String REMOVED_HOST_TIMEOUT = "removedHostTimeout";
    private static final String ACCESS_POINT_INFO = "accessPoints";

    /////////////////////////////////////////////////////////////////////////////////
    // more complex keys
    public static int DEFAULT_GATEWAY_FLOW_PRIORITY = 10000;

    /////////////////////////////////////////////////////////////////////////////////
    // Default values
    public static int DEFAULT_ROAMING_UNICAST_FLOW_PRIORITY = 20000;
    public static int DEFAULT_REMOVED_HOST_TIMEOUT_SECONDS = 30;
    public static int DEFAULT_REMOVED_HOST_DELAY_GRANULARITY = 5;
    private final Logger log = LoggerFactory.getLogger(getClass());

    /////////////////////////////////////////////////////////////////////////////////
    // Simple top-level properties

    /**
     * The priority for flows between the home access point and its default gateway
     * connection point
     *
     * @return flow priority (1-65535)
     */
    public int getDefaultGatewayFlowPriority() {
        // TODO: Bounds checking would be nice here, throw a ConfigException on error
        return get(GATEWAY_FLOW_PRIORITY, DEFAULT_GATEWAY_FLOW_PRIORITY);
    }

    /**
     * The priority for unicast data flows between the a roaming mobile host and
     * and its default gateway and home access point/subnet
     *
     * @return flow priority (1-65535)
     */
    public int getRoamingUnicastFlowPriority() {
        // TODO: Bounds checking would be nice here, throw a ConfigException on error
        return get(UNICAST_FLOW_PRIORITY, DEFAULT_ROAMING_UNICAST_FLOW_PRIORITY);
    }

    /**
     * The number of seconds after a HOST_REMOVED event until the host is deleted.  This
     * allows for a move to be a 'REMOVE' followed by an 'ADD'
     *
     * @return timeout in seconds
     */
    public int getDefaultRemovedHostTimeout() {
        // TODO: Bounds checking would be nice here, throw a ConfigException on error
        return get(REMOVED_HOST_TIMEOUT, DEFAULT_REMOVED_HOST_TIMEOUT_SECONDS);
    }
    /////////////////////////////////////////////////////////////////////////////////
    // more complex keys

    /**
     * Get the access point configuration map
     *
     * @return map of access points with the sdnWLAN Access Point MAC address as the key
     * @throws ConfigException
     */
    public Map<MacAddress, AccessPointConfig> getAccessPoints() {
        Map<MacAddress, AccessPointConfig> accessPoints = Maps.newHashMap();

        if (object.has(ACCESS_POINT_INFO)) {
            ArrayNode nodeArray = (ArrayNode) object.path(ACCESS_POINT_INFO);

            nodeArray.forEach(accessNode -> {
                try {
                    AccessPointConfig point = AccessPointConfig.valueOf(accessNode);

                    if (accessPoints.put(point.getMacAddress(), point) != null) {
                        throw new IllegalArgumentException("Duplicate access point entry");
                    }
                } catch (ConfigException e) {
                    log.warn("AccessPoint Configuration Exception", e.toString());
                } catch (IllegalArgumentException e) {
                    log.warn("AccessPoint Illegal Argument Exception", e.toString());
                }
            });
        }
        return Collections.unmodifiableMap(accessPoints);
    }
}
