package com.example.nosqllab2.configuration;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.core.RiakCluster;
import com.basho.riak.client.core.RiakNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class RiakConfig {
    @Bean(destroyMethod = "shutdown")
    RiakClient riakClient(@Value("${riak.host:127.0.0.1}") String host,
                          @Value("${riak.port:8087}") int port,
                          @Value("${riak.secondary-host:${riak.host:127.0.0.1}}") String secondaryHost,
                          @Value("${riak.secondary-port:8089}") int secondaryPort) throws UnknownHostException {
        List<RiakNode> nodes = new ArrayList<>();
        nodes.add(node(host, port));
        if (secondaryPort > 0 && (!host.equals(secondaryHost) || port != secondaryPort)) {
            nodes.add(node(secondaryHost, secondaryPort));
        }
        RiakCluster cluster = new RiakCluster.Builder(nodes).build();
        cluster.start();
        return new RiakClient(cluster);
    }

    private static RiakNode node(String host, int port) throws UnknownHostException {
        return new RiakNode.Builder()
                .withRemoteAddress(host)
                .withRemotePort(port)
                .withConnectionTimeout(1000)
                .build();
    }
}
