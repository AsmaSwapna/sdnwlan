package net.bcsw.sdnwlan.cli;

import net.bcsw.sdnwlan.AccessPoint;
import net.bcsw.sdnwlan.SDNWLANService;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.onosproject.cli.AbstractShellCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

/**
 * Created by cboling on 12/12/15.
 */
public class MacAddressCompleter implements Completer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private SDNWLANService service;

    /**
     * Delegate string completer
     *
     * @param buffer     TODO Look this up
     * @param cursor     TODO Look this up
     * @param candidates TODO Look this up
     * @return TODO Look this up
     */
    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {

        log.debug("Entry");

        service = AbstractShellCommand.get(SDNWLANService.class);

        // Delegate string completer

        StringsCompleter delegate = new StringsCompleter();

        Iterator<AccessPoint> it = service.getAccessPoints().values().iterator();
        SortedSet<String> strings = delegate.getStrings();

        while (it.hasNext()) {
            strings.add(it.next().getMacAddress().toString());
        }
        // Now let the completer do the work for figuring out what to offer.

        return delegate.complete(buffer, cursor, candidates);
    }
}
