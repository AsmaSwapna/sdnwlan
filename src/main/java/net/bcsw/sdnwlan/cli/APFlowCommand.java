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

import net.bcsw.sdnwlan.SDNWLANService;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Argument;
import org.onosproject.cli.AbstractShellCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * List flows used by access point
 */
@Command(scope = "bcsw", name = "sdnwlan-flows", description = "Lists sdnWLAN Access Point flows")
public class APFlowCommand extends AbstractShellCommand {

    // TODO Do we want a header/table format or a list format with embedded tags?
    //private static final String FORMAT_HEADER = "MAC ID: %s -> IP ASSIGNED %s";
    private static final String FORMAT_WLAN = "Hostname: %s -> MAC: %s, TODO...";
    private final Logger log = LoggerFactory.getLogger(getClass());
    @Argument(index = 0, name = "macAddress", description = "MAC Address of the Access Point",
            required = false, multiValued = false)
    private String macAddress = null;
    // Reference to our service
    private SDNWLANService service;

    /**
     * Execute the sdnwlan CLI command
     */
    @Override
    protected void execute() {

        log.info("Entry");

        service = AbstractShellCommand.get(SDNWLANService.class);

//        List<AccessPoint> list = service.getAccessPoints().values();
//
//        // TODO print(FORMAT_HEADER);
//
//        list.stream().forEach((entry) -> {
//            if ((hostName == null) || (entry.getName().equals(hostName))) {
//                print(FORMAT_WLAN, entry.getName(), entry.getMac().toString());
//            }
//        });
    }
}