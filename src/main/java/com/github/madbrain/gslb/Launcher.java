package com.github.madbrain.gslb;

import org.apache.directory.server.dns.protocol.DnsTcpDecoder;
import org.apache.directory.server.dns.protocol.DnsTcpEncoder;
import org.apache.directory.server.dns.protocol.DnsUdpDecoder;
import org.apache.directory.server.dns.protocol.DnsUdpEncoder;
import org.apache.directory.server.dns.store.RecordStore;
import org.apache.directory.server.dns.store.RecordStoreStub;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.AbstractIoAcceptor;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * https://github.com/polaris-gslb/polaris-gslb
 */
public class Launcher {

    public static void main(String[] args) throws IOException {
        new Launcher().run(args);
    }

    private void run(String[] args) throws IOException {
        RecordStoreStub store = new RecordStoreStub();
        run(createTcpAcceptor(10053), store);
        run(createUdpAcceptor(10053), store);
    }

    private void run(AbstractIoAcceptor acceptor, RecordStore store) throws IOException {
        acceptor.setHandler(new DnsProtocolHandler(store));
        acceptor.bind();
    }

    private NioSocketAcceptor createTcpAcceptor(int port) {
        NioSocketAcceptor acceptor = new NioSocketAcceptor(3);
        acceptor.setReuseAddress(true);
        acceptor.setBacklog(50);
        acceptor.getSessionConfig().setTcpNoDelay(true);
        acceptor.setDefaultLocalAddress(new InetSocketAddress(port));

        DefaultIoFilterChainBuilder builder = new DefaultIoFilterChainBuilder();
        builder.addFirst("codec", new ProtocolCodecFilter(new DnsTcpEncoder(), new DnsTcpDecoder()));
        acceptor.setFilterChainBuilder(builder);

        return acceptor;
    }

    private NioDatagramAcceptor createUdpAcceptor(int port) {
        NioDatagramAcceptor acceptor = new NioDatagramAcceptor();
        acceptor.getSessionConfig().setReuseAddress(true);
        acceptor.setDefaultLocalAddress(new InetSocketAddress(port));

        DefaultIoFilterChainBuilder builder = new DefaultIoFilterChainBuilder();
        builder.addFirst("codec", new ProtocolCodecFilter(new DnsUdpEncoder(), new DnsUdpDecoder()));
        acceptor.setFilterChainBuilder(builder);

        return acceptor;
    }
}
