package org.kp.distributedredislocking.Controller;

import org.kp.distributedredislocking.Util.LockUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class LockingDemoController {

    private final LockUtil lockUtil;

    public LockingDemoController(LockUtil lockUtil) {
        this.lockUtil = lockUtil;
    }


    @GetMapping("/api/resource/{id}/update")
    public String updateResource(@PathVariable String id) {
        // We use the ID from the path as the unique lock key
        return lockUtil.acquireLock(id);
    }
    /**
     * New endpoint using the explicitly defined 'Cluster Mode' lock registry.
     * @param resourceId The ID to lock (e.g., user123).
     * @return Status message.
     */
    @GetMapping("/api/cluster-lock/{resourceId}")
    public String acquireClusterLock(@PathVariable String resourceId) {
        return lockUtil.acquireLockInClusterMode(resourceId);
    }
}
