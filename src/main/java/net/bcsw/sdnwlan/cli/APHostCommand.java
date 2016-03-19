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
import net.bcsw.sdnwlan.MobileHost;
import net.bcsw.sdnwlan.SDNWLANService;
import com.google.common.collect.Lists;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * List hosts information
 */
@Command(scope = "bcsw", name = "sdnwlan-hosts", description = "Lists sdnWLAN Hosts")
public class APHostCommand extends AbstractShellCommand {

    private static final String FORMAT_HEADER =
            "MAC Address        VLAN  Home Access Point       Current Access Point";
    private static final String FORMAT_HOSTS = "%s %4d  %s/%-4s  %s";
    private final Logger log = LoggerFactory.getLogger(getClass());
    @Argument(index = 0, name = "macAddress", description = "MAC Address of the Host",
            required = false, multiValued = false)
    private String macAddress = null;
    // Reference to our service
    private SDNWLANService service;

    //          1         2         3         4         5         6         7
    // 1234567890123456789012345678901234567890123456789012345678901234567890123456789
    // MAC Address       VLAN  Home Access Point       Current Access Point
    // 01:02:03:04:05:06 1234  01:02:03:04:05:06/1234  01:02:03:04:05:06/1234

    /**
     * Execute the sdnwlan CLI command
     * <p>
     * TODO: Having an option to print hosts that are roaming or 'at home' could be useful
     */
    @Override
    protected void execute() {

        log.info("Entry");

        service = AbstractShellCommand.get(SDNWLANService.class);

        List<MobileHost> hosts = Lists.newArrayList();
        Collection<AccessPoint> list = service.getAccessPoints().values();

        list.forEach(ap -> hosts.addAll(ap.getLocalHosts().values()));

        print(FORMAT_HEADER);

        hosts.forEach(host -> {
            if ((macAddress == null) || host.getMacAddress().equals(macAddress)) {

                // TODO: Currently just output/support one connection

                String current = "<unknown>";
                AccessPoint currentAp = host.getCurrentAccessPoint();
                if (currentAp != null) {
                    current = String.format("%s/%-4.4s", currentAp.getMacAddress().toString(),
                            vidListToString(currentAp.getIngressVlans().getVlanList()));
                }
                print(FORMAT_HOSTS, host.getMacAddress().toString(),
                        (int) host.getHostInfo().vlan().toShort(),
                        host.getHomeAccessPoint().getMacAddress().toString(),
                        vidListToString(host.getHomeAccessPoint().getIngressVlans().getVlanList()),
                        current);
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