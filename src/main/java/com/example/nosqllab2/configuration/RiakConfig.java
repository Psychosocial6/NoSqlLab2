package com.example.nosqllab2.configuration;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.core.RiakCluster;
import com.basho.riak.client.core.RiakNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.UnknownHostException;
import java.util.Arrays;

@Configuration
public class RiakConfig {

    @Value("${riak.host:127.0.0.1}")
    private String host;

    @Value("${riak.port:8087}")
    private int port1;

    @Value("${riak.port:8089}")
    private int port2;

    @Bean
    public RiakClient riakClient() throws UnknownHostException {
        RiakNode node1 = new RiakNode.Builder()
                .withRemoteAddress(host)
                .withRemotePort(port1)
                .build();

        RiakNode node2 = new RiakNode.Builder()
                .withRemoteAddress(host)
                .withRemotePort(port2)
                .build();


        RiakCluster cluster = new RiakCluster.Builder(Arrays.asList(node1, node2)).build();

        cluster.start();
        return new RiakClient(cluster);
    }

}