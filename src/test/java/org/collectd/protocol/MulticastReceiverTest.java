/*
 * jcollectd
 * Copyright (C) 2009 Hyperic, Inc.
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; only version 2 of the License is applicable.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA  02110-1301 USA
 */

package org.collectd.protocol;

import java.io.IOException;
import java.net.*;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.sun.org.apache.bcel.internal.generic.NEW;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.collectd.api.PluginData;
import org.collectd.api.ValueList;
import sun.jvm.hotspot.tools.ObjectHistogram;

public class MulticastReceiverTest extends ReceiverTest {

    public static Test suite() {
        return new TestSuite(MulticastReceiverTest.class);
    }

    @Override
    protected DatagramSocket createSocket() throws IOException {
        MulticastSocket socket = new MulticastSocket(Network.DEFAULT_PORT);
        String laddr = Network.DEFAULT_V4_ADDR;
        getReceiver().setListenAddress(laddr);
        socket.joinGroup(InetAddress.getByName(laddr));
        return socket;
    }

    interface Generator<T> {
        T next();
    }

    class IncreasingGenerator implements Generator<Integer> {
        Random rnd = new Random();
        Integer val = 0;

        public Integer next() {
            val += rnd.nextInt(100);
            return val;
        }
    }

    class Task implements Runnable {
        Generator<Integer> generator;
        String dest;
        Sender sender;

        Task(Sender sender, String dest, Generator<Integer> generator) {
            this.dest = dest;
            this.sender = sender;
            this.generator = generator;
        }

        public void run() {
            try {
                ValueList vl = newValueList();
                vl.addValue(generator.next());
                sender.write(vl);
                sender.addServer(dest);
                sender.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void testListen() throws Exception {
        DatagramSocket socket = getReceiver().getSocket();
        assertTrue(socket.isBound());
        getLog().info("Bound to LocalPort=" + socket.getLocalPort() +
                ", LocalAddress=" +
                socket.getLocalAddress().getHostAddress());

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        UdpSender udp = new UdpSender();
        getReceiver().setDispatcher(new DispatchLogger());

        String dest = getReceiver().getListenAddress() + ":" + Network.DEFAULT_PORT;
        udp.addServer(dest);
        Runnable sender = new Task(udp, socket.getLocalPort() + ":" + socket.getLocalAddress(), new IncreasingGenerator()) {

        };
        ValueList pd = newValueList();
        pd.addValue(Integer.valueOf(100));
        udp.write(pd);
        udp.write(pd);
        udp.write(pd);
        udp.flush();
        scheduler.scheduleWithFixedDelay(sender, 0, 50, TimeUnit.MILLISECONDS);

        Thread.sleep(4000);
    }

    private ValueList newValueList() {
        ValueList vl = new ValueList();
        vl.setPlugin("junit");
        vl.setPluginInstance("local");
        vl.setInterval(10);
        vl.setType("connections");
        return vl;
    }
}
