/*
 * Copyright 2015 Open Networking Laboratory
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

package net.bcsw.sdnwlan.cli;

import net.bcsw.sdnwlan.AccessPoint;
import net.bcsw.sdnwlan.IpGatewayAndMask;
import net.bcsw.sdnwlan.SDNWLANService;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Argument;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Command to show the list of vBNG IP address mapping entries.
 */
@Command(scope = "bcsw", name = "sdnwlan-aps", description = "Lists all sdnWLAN Access Points")
public class APListCommand extends AbstractShellCommand {

    private static final String FORMAT_HEADER =
            "Name        MAC Address        VLAN  Default Gateway     Connections               Other Gateways-VLAN(s)";
    private static final String FORMAT_EXTRA =
            "                               %4.4s  %-18.18s  %-24.24s  %-18s %s";
    private static final String FORMAT_WLAN = "%-10.10s  %s  %4.4s  %-18.18s  %-24.24s  %-18s %s";
    private final Logger log = LoggerFactory.getLogger(getClass());
    @Argument(index = 0, name = "macAddress", description = "MAC Address of the Access Point",
            required = false, multiValued = false)
    private String macAddress = null;
    // Reference to our service
    private SDNWLANService service;
    private Collection<AccessPoint> list;

    //          1         2         3         4         5         6         7         8         9         1         2
    // 12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
    // Name        MAC Address        VLAN  Default Gateway     Connections              Other Gateways-VLAN(s)
    // ApName7890  01:02:03:04:05:06  1234  123.123.123.123/27  of:a2a2a2a2a2a2a2a2/123  123.123.123.123/27 1234/1234

    /**
     * Execute the sdnwlan CLI command
     */
    @Override
    protected void execute() {

        log.info("Entry");

        service = AbstractShellCommand.get(SDNWLANService.class);
        list = service.getAccessPoints().values();

        print(FORMAT_HEADER);

        list.stream().forEach((entry) -> {
            if ((macAddress == null) || (entry.getMacAddress().toString().equalsIgnoreCase(macAddress))) {
                // Determine maximum lines of output (VLANS, gateways, otherVids, and
                // Connections may be multivalued

                List<VlanId> ingrVids = entry.getIngressVlans().getVlanList();
                Iterator<IpGatewayAndMask> gwItor = entry.getDefaultGateways().keySet().iterator();
                Map<IpGatewayAndMask, List<VlanId>> otherList = entry.getOtherVids();
                Iterator<IpGatewayAndMask> othGwItor = entry.getOtherVids().keySet().iterator();
                Iterator<List<VlanId>> othVidsItor = entry.getOtherVids().values().iterator();
                List<ConnectPoint> connList = entry.getConnections();

                int max = ingrVids.size();
                if (entry.getDefaultGateways().size() > max) max = entry.getDefaultGateways().size();
                if (otherList.size() > max) max = otherList.size();
                if (connList.size() > max) max = connList.size();

                // Print the first full line.  Some could be blank

                String ingrVid = ingrVids.size() > 0 ? ingrVids.get(0).toString() : VlanId.NONE.toString();
                String gateway = gwItor.hasNext() ? gwItor.next().toString() : "";
                String otherGw = othGwItor.hasNext() ? othGwItor.next().toString() : "";
                String otherVid = othVidsItor.hasNext() ? vidListToString(othVidsItor.next()) : "";
                String connPt = "";

                if (connList.size() > 0) {
                    ConnectPoint point = connList.get(0);
                    connPt = String.format("%s/%d", point.deviceId().toString(), point.port().toLong());
                }
                print(FORMAT_WLAN, entry.getName(), entry.getMacAddress().toString(), ingrVid,
                        gateway, connPt, otherGw, otherVid);

                // Now print any continuation lines
                for (int line = 1; line < max; line++) {
                    ingrVid = ingrVids.size() > line ? ingrVids.get(line).toString() : "";
                    gateway = gwItor.hasNext() ? gwItor.next().toString() : "";
                    otherGw = othGwItor.hasNext() ? othGwItor.next().toString() : "";
                    otherVid = othVidsItor.hasNext() ? vidListToString(othVidsItor.next()) : "";
                    connPt = "";

                    if (connList.size() > line) {
                        ConnectPoint point = connList.get(line);
                        connPt = String.format("%s/%d", point.deviceId().toString(), point.port().toLong());
                    }
                    print(FORMAT_EXTRA, ingrVid, gateway, connPt, otherGw, otherVid);
                }
            }
        });
    }

    private String vidListToString(List<VlanId> vidList) {

        String result = "-1";

        if (vidList.size() > 0) {
            result = vidList.get(0).toString();
            for (int idx = 1; idx < vidList.size(); idx++) {
                result = String.format("%s/%d", (int) vidList.get(idx).toShort());
            }
        }
        return result;
    }
}