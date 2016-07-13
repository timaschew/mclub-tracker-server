package mclub.tracker.protocol;

import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.channel.ChannelPipelineFactory;

public class MockNettyBootstrap extends Bootstrap{
    public void setPipelineFactory(ChannelPipelineFactory pipelineFactory) {
        // noop;
    }
}
