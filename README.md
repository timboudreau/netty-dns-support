Netty DNS Support
=================

This is an additional library, which hopefully can be contributed to Netty, which
makes it easy to work with DNS records programmatically.

It builds on the patches in [this fork of Netty (pull request pending)](https://github.com/timboudreau/netty/tree/dns-server-41) -
[diff here](https://github.com/netty/netty/compare/4.1...timboudreau:dns-server-41), which
refactors a few things, adds mDNS support (Rendezvous/Avahi) and flexible writing of DNS
names supporting pointer compression, punycode and mDNS's UTF-8 encoding.

In particular, it adds a `TypedDnsRecordDecoder` and `TypedDnsRecordEncoder` which encode and decode records
using a `CodecRegistry` which can be added to at runtime.  So records can be decoded into
Java objects of whatever type you'd like; there is built-in support for the record types
A, AAAA, NS, PTR, CNAME, DNAME, SOA, TXT, LOC, OPT, SRV, MX, NSEC and URI.

Additionally, a similar codec registry is used for the sub-records in OPT pseudo-records,
and DnsRecordCodec instances are used to encode and decode those (ECS and COOKIE are
supported out-of-the-box).

Adding a DnsRecordCodec
=======================

Record codecs are simple (where what they encode and decode is, anyway):

```java

final class MailExchangerRecordCodec extends DnsRecordCodec<MailExchanger> {

    MailExchangerRecordCodec() {
        super(MailExchanger.class);
    }

    @Override
    public MailExchanger read(ByteBuf buf, NameCodec forReadingNames, int length) throws DnsDecoderException,
            UnmappableCharacterException {
        int pref = buf.readShort();
        CharSequence mx = forReadingNames.readName(buf);
        return new MailExchanger(pref, mx);
    }

    @Override
    public void write(MailExchanger value, NameCodec names, ByteBuf into) throws IOException {
        into.writeShort(value.pref());
        names.writeName(value.mx(), into);
    }
}
```

Using a Record Codec
====================

Creating a server or client starts with a message encoder and decoder.  Using the codec
above could be as simple as:

```java
        DnsMessageEncoder x = DnsMessageEncoder.builder()
                .withRecordEncoder(new TypedDnsRecordEncoder(DnsRecordCodecRegistry
                        .builderWithDefaultCodecs()
                        .add(new MailExchangerRecordCodec(), DnsRecordType.MX)))
                .mDNS()
                .withIllegalRecordPolicy(THROW)
                .withNameFeatures(MDNS_UTF_8, COMPRESSION, WRITE_TRAILING_DOT)
                .build();
```

Then decoded MX records will have a payload of this type.  The code is nearly identical
for decoding.

Building
========

The Netty fork has its own version number, `4.1-dns-dev` which this code
depends on, so for now you will need to build it.

Example
=======

For a brief example that does something useful, here is a program that lists
Avahi/Rendevous mDNS service messages going by on the local network, after
issuing a query to all hosts on that network to enumerate their services:

```java
public class MdnsTest {
    private final EventLoopGroup group;
    private final DnsClientHandler clientHandler;
    private final ChannelFuture closeFuture;
    private MdnsTest(NetworkInterface iface) throws Exception {
        this.group = new NioEventLoopGroup();
        this.clientHandler = new DnsClientHandler();
        Bootstrap b = new Bootstrap();
        b.group(group)
            .channel(NioDatagramChannel.class)
            .option(ChannelOption.IP_MULTICAST_IF, iface)
            .option(ChannelOption.SO_REUSEADDR, true)
            .option(ChannelOption.IP_MULTICAST_LOOP_DISABLED, false)
            .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .handler(DnsMessageDecoder.builder()
                    .withRecordDecoder(
                            new TypedDnsRecordDecoder(true))
                    .mDNS()
                    .buildUdpQueryAndResponseDecoder());

        Channel channel = b.bind(5353)
                .sync()
                .channel();
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast("responseDecoder", clientHandler);
        pipeline.addLast("queryEncoder", DnsMessageEncoder
                .builder().mDNS().buildUdpQueryEncoder());
        this.closeFuture = channel.closeFuture();

        // Ask all mDNS devices on the local network to enumerate their services
        InetSocketAddress localMulticast6
          = new InetSocketAddress(Inet6Address.getByName("FF02::FB"), 5353);

        InetSocketAddress localMulticast4
          = new InetSocketAddress(Inet4Address.getByName("224.0.0.251"), 5353);

        DatagramDnsQuery query = new DatagramDnsQuery(null, localMulticast6, 1);
        DefaultDnsQuestion question = new DefaultDnsQuestion(
                "_services._dns-sd._udp.local", DnsRecordType.PTR);

        query.addRecord(DnsSection.QUESTION, question);
        channel.writeAndFlush(query);

        query = new DatagramDnsQuery(null, localMulticast4, 1);
        query.addRecord(DnsSection.QUESTION, question);
        channel.writeAndFlush(query);
    }

    class DnsClientHandler extends SimpleChannelInboundHandler<DnsResponse> {
        DnsClientHandler() {
            super(DnsResponse.class);
        }
        @Override
        protected void channelRead0(ChannelHandlerContext chc,
                DnsResponse msg) throws Exception {
            
            System.out.println("\n**************************\n");
            System.out.println(msg);
        }
    }

    public static void main(String[] args) throws Exception {
        NetworkInterface iface = null;
        Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
        while (en.hasMoreElements()) {
            NetworkInterface in = en.nextElement();
            if (in.isUp() && !in.isLoopback() && !in.isVirtual()) {
                iface = in;
                break;
            }
        }
        if (iface == null) {
            throw new Error("Could not find a network interface");
        }
        System.out.println("Using interface " + iface.getDisplayName());
        MdnsTest test = new MdnsTest(iface);
        test.closeFuture.await();
    }
}
```
