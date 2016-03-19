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
import org.onlab.packet.IpPrefix;

import java.util.Objects;

/**
 * Class to allow coding of default gateway and CIDR in a single string
 */
public class IpGatewayAndMask extends IpPrefix {

    private final IpAddress defaultGateway;

    protected IpGatewayAndMask(IpAddress gwAddress, int prefixLength) {
        super(gwAddress, prefixLength);
        this.defaultGateway = gwAddress;
    }

    public IpAddress gatewayAddress() { return this.defaultGateway; }

    public IpPrefix getIpPrefix() { return IpPrefix.valueOf(this.address(), this.prefixLength()); }

    public static IpGatewayAndMask valueOf(int address, int prefixLength) {
        return new IpGatewayAndMask(IpAddress.valueOf(address), prefixLength);
    }

    public static IpGatewayAndMask valueOf(IpAddress.Version version, byte[] address, int prefixLength) {
        return new IpGatewayAndMask(IpAddress.valueOf(version, address), prefixLength);
    }

    public static IpGatewayAndMask valueOf(IpAddress address, int prefixLength) {
        return new IpGatewayAndMask(address, prefixLength);
    }

    public static IpGatewayAndMask valueOf(String address) {
        String[] parts = address.split("/");
        if(parts.length != 2) {
            String ipAddress1 = "Malformed IP prefix string: " + address + ". " + "Address must take form \"x.x.x.x/y\" or " + "\"xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx:xxxx/y\"";
            throw new IllegalArgumentException(ipAddress1);
        } else {
            IpAddress gwAddress = IpAddress.valueOf(parts[0]);
            int prefixLength = Integer.parseInt(parts[1]);
            return new IpGatewayAndMask(gwAddress, prefixLength);
        }
    }

    public boolean contains(IpPrefix other) {
        return super.contains(other);
    }

    @Override
    public int hashCode() {
        return Objects.hash(new Object[]{this.defaultGateway, (short)this.prefixLength()});
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;
        } else if(obj != null && obj instanceof IpGatewayAndMask) {
            IpGatewayAndMask other = (IpGatewayAndMask)obj;
            return this.prefixLength() == other.prefixLength() && this.defaultGateway.equals(other.defaultGateway);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.defaultGateway.toString());
        builder.append("/");
        builder.append(String.format("%d", this.prefixLength()));
        return builder.toString();
    }
}
