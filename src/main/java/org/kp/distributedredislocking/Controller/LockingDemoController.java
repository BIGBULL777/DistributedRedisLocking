package org.kp.distributedredislocking.Controller;

import org.kp.distributedredislocking.Util.LockUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class LockingDemoController {

    private final LockUtil LockUtil;

    public LockingDemoController(LockUtil lockUtil) {
        LockUtil = lockUtil;
    }


    @GetMapping("/api/resource/{id}/update")
    public String updateResource(@PathVariable String id) {
        // We use the ID from the path as the unique lock key
        return LockUtil.acquireLock(id);
    }
}
