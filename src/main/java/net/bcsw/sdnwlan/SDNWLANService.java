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

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;

import java.util.Map;

/**
 * Provides service of the sdnWLAN
 */
public interface SDNWLANService
{
    /**
     * Get a map of the current LAN AccessPoints
     *
     * @return collection of access points
     */
    Map<MacAddress, AccessPoint> getAccessPoints();

    /**
     * Get a map of the default gateways
     *
     * @return collection of default gateways
     */
    Map<IpAddress, DefaultGateway> getDefaultGateways();

    /**
     * Creates a sdnWLAN Access Point
     *
     * @param apMac
     * @return The new access point
     */
    AccessPoint createAccessPoint(MacAddress apMac);

    /**
     * Deletes a sdnWLAN Access Point
     *
     * @param accessPoint The access point to delete
     * @return true if the access point was found and removed from the system
     */
    boolean deleteAccessPoint(AccessPoint accessPoint);

    /**
     * Remove a host completely from the system
     *
     * @param host Mobile host to remove
     */
    void purgeHost(MobileHost host);
}
