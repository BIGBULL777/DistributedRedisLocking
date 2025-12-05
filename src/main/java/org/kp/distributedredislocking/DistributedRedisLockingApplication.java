package org.kp.distributedredislocking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DistributedRedisLockingApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributedRedisLockingApplication.class, args);
    }

}
