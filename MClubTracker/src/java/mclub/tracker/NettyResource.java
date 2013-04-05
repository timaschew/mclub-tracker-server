/**
 * Project: CarTracServer
 * 
 * File Created at Apr 5, 2013
 * $id$
 * 
 * Copyright 2013, Shawn Chain (shawn.chain@gmail.com).
 * All rights reserved.
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
package mclub.tracker;

import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.DatagramChannelFactory;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

/**
 * @author shawn
 * 
 */
public class NettyResource {

	private static Timer timer = null;
	private static ChannelFactory channelFactory = null;
	private static DatagramChannelFactory datagramChannelFactory = null;

	private NettyResource() {
	}

	public static ChannelFactory getChannelFactory() {
		if (channelFactory == null) {
			channelFactory = new NioServerSocketChannelFactory();
		}
		return channelFactory;
	}

	public static DatagramChannelFactory getDatagramChannelFactory() {
		if (datagramChannelFactory == null) {
			datagramChannelFactory = new NioDatagramChannelFactory();
		}
		return datagramChannelFactory;
	}

	public static void release() {
		if (timer != null) {
			timer.stop();
			timer = null;
		}

		if (channelFactory != null) {
			channelFactory.releaseExternalResources();
			channelFactory = null;
		}
		if (datagramChannelFactory != null) {
			datagramChannelFactory.releaseExternalResources();
			datagramChannelFactory = null;
		}
	}

	public static Timer getTimer() {
		if (timer == null) {
			timer = new HashedWheelTimer();
		}
		return timer;
	}
}
