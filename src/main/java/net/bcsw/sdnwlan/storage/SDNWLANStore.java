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
package net.bcsw.sdnwlan.storage;

import net.bcsw.sdnwlan.config.AccessPointConfig;

import java.util.List;
import java.util.Map;

/**
 * Interface for persistent storage for learned/configured sdnWLAN Access Points and mobile hosts
 * <p>
 * TODO Add thrown exceptions for cases of duplicate entries on 'puts' or notFound on 'removes'
 */
public interface SDNWLANStore {// TODO extends Store<NetworkConfigEvent, NetworkConfigStoreDelegate> {

    /**
     * Save an access Point
     *
     * @param accessPoint TODO: Document
     */
    void putAccessPoint(AccessPointConfig accessPoint);

    /**
     * Remove an access Point
     *
     * @param accessPoint TODO: Document
     */
    void removeAccessPoint(AccessPointConfig accessPoint);

    /**
     * Get a list of current access points saved to the store
     *
     * @return list of access points
     */
    List<AccessPointConfig> getAccessPoints();


    // TODO: Add put/remove/get for mobile hosts as well if needed
}
