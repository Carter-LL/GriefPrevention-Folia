package me.ryanhamshire.GriefPrevention;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import com.normalsmp.Util.FoliaCompat;

// FEATURE: automatically remove claims owned by inactive players which:
// ...aren't protecting much OR
// ...are a free new player claim (and the player has no other claims) OR
// ...because the player has been gone a REALLY long time, and that expiration has been configured in config.yml

// runs every 1 minute using Folia's global region scheduler
class FindUnusedClaimsTask implements Runnable {

    private List<UUID> claimOwnerUUIDs;
    private Iterator<UUID> claimOwnerIterator;

    FindUnusedClaimsTask() {
        refreshUUIDs();
    }

    @Override
    public void run() {
        // don't do anything when there are no claims
        if (claimOwnerUUIDs.isEmpty()) return;

        // wrap search around to beginning
        if (!claimOwnerIterator.hasNext()) {
            refreshUUIDs();
            return;
        }

        UUID nextOwner = claimOwnerIterator.next();

        // Run the cleanup task using Folia's global scheduler (1-tick delay to offload properly)
        FoliaCompat.runGlobalRegion(GriefPrevention.instance, () -> {
            new CleanupUnusedClaimPreTask(nextOwner).run();
        }, 1L); // small delay to yield execution
    }

    public void refreshUUIDs() {
        // Fetch owner UUIDs from list of claims
        claimOwnerUUIDs = GriefPrevention.instance.dataStore.claims.stream()
                .map(claim -> claim.ownerID)
                .distinct()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!claimOwnerUUIDs.isEmpty()) {
            Collections.shuffle(claimOwnerUUIDs);
        }

        GriefPrevention.AddLogEntry("The following UUIDs own a claim and will be checked for inactivity in the following order:", CustomLogEntryTypes.Debug, true);

        for (UUID uuid : claimOwnerUUIDs) {
            GriefPrevention.AddLogEntry(uuid.toString(), CustomLogEntryTypes.Debug, true);
        }

        claimOwnerIterator = claimOwnerUUIDs.iterator();
    }
}
