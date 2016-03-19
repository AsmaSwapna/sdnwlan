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
package net.bcsw.sdnwlan.flows;

import net.bcsw.sdnwlan.SDNWLANConnectPoint;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.intent.Intent;

import java.util.Objects;

/**
 * Provides common interface for all flow implementations
 * <p>
 * TODO The idea here is to allow for various kinds of northbound and southbound
 * flows to be created and tested so that the best can be determined but have
 * very little of the other code change.  Plenty of work to be done here.
 */
public abstract class Flow {

    protected SDNWLANConnectPoint ingress;
    protected SDNWLANConnectPoint egress;
    protected ApplicationId appId;

    protected Flow(ApplicationId appId, SDNWLANConnectPoint ingress, SDNWLANConnectPoint egress) {

        this.ingress = ingress;
        this.egress = egress;
        this.appId = appId;

        checkCtorParams();
    }

    private void checkCtorParams() {
        if (ingress == null) {
            throw new IllegalArgumentException("An Ingress connection point must be supplied");
        }
        if (egress == null) {
            throw new IllegalArgumentException("An Egress connection point must be supplied");
        }
    }

    /**
     * Get the application ID to use for flows/intents
     *
     * @return Application ID
     */
    protected ApplicationId getApplicationId() {
        return appId;
    }

    /**
     * Return the short name for the implemented Flow
     */
    public abstract String name();

    /**
     * Return the description for the implemented Flow
     */
    public abstract String description();

    /**
     * Get the intent object that represents this flow
     *
     * @param flowPriority Priority for the intent
     * @return Intent ready for submission or 'null' on error
     */
    public abstract Intent getIntent(int flowPriority);

    /**
     * TODO: Should provide some statistics common to all flows
     * <p>
     * - date created
     * - ...
     */

    @Override
    public int hashCode() {
        return Objects.hash(new Object[]{appId, ingress, egress});
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj != null && obj instanceof Flow) {
            Flow other = (Flow) obj;
            return this.ingress.equals(other.ingress) &&
                    this.egress.equals(other.egress);
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.ingress.toString());
        builder.append(" <-> ");
        builder.append(this.egress.toString());

        return builder.toString();
    }
}