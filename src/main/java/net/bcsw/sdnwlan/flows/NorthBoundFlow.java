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

import java.util.Objects;

// TODO: Do we need a common base?

/**
 * Provides common interface for Northbound Flow implementations
 */
public abstract class NorthBoundFlow extends Flow {

    protected Object match = null;

    public NorthBoundFlow(ApplicationId appId,
                          SDNWLANConnectPoint sbPoint,
                          SDNWLANConnectPoint nbPoint,
                          Object match) {
        super(appId, sbPoint, nbPoint);
        this.match = match;
    }

    @Override
    public int hashCode() {
        return Objects.hash(new Object[]{super.hashCode(), match});
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj != null && obj instanceof NorthBoundFlow) {
            NorthBoundFlow other = (NorthBoundFlow) obj;

            return super.equals(other) &&
                    this.match.equals(other.match);
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(super.toString()).append(" / ");

        builder.append(this.match.toString());

        return builder.toString();
    }

    /**
     * TODO: Should provide some statistics common to this direction (if any)
     */
}
