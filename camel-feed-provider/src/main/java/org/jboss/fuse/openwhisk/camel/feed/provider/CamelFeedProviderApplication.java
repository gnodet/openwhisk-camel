package org.jboss.fuse.openwhisk.camel.feed.provider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CamelFeedProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(CamelFeedProviderApplication.class, args);
    }
}
