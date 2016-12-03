package com.github.madbrain.gslb;

import org.apache.directory.server.dns.DnsException;
import org.apache.directory.server.dns.messages.*;
import org.apache.directory.server.dns.service.DnsContext;
import org.apache.directory.server.dns.service.DomainNameService;
import org.apache.directory.server.dns.store.RecordStore;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class DnsProtocolHandler implements IoHandler {
    private static final Logger LOG = LoggerFactory.getLogger(DnsProtocolHandler.class);

    private RecordStore store;
    private String contextKey = "context";


    /**
     * Creates a new instance of DnsProtocolHandler.
     *
     * @param store
     */
    public DnsProtocolHandler(RecordStore store) {
        this.store = store;
    }


    public void sessionCreated(IoSession session) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} CREATED:  {}", session.getRemoteAddress(), session.getTransportMetadata());
        }
    }


    public void sessionOpened(IoSession session) {
        LOG.debug("{} OPENED", session.getRemoteAddress());
    }


    public void sessionClosed(IoSession session) {
        LOG.debug("{} CLOSED", session.getRemoteAddress());
    }


    public void sessionIdle(IoSession session, IdleStatus status) {
        LOG.debug("{} IDLE ({})", session.getRemoteAddress(), status);
    }


    public void exceptionCaught(IoSession session, Throwable cause) {
        LOG.error(session.getRemoteAddress() + " EXCEPTION", cause);
        session.closeNow();
    }


    public void messageReceived(IoSession session, Object message) {
        LOG.debug("{} RCVD:  {}", session.getRemoteAddress(), message);

        DnsMessage request = (DnsMessage) message;

        try {
            DnsContext dnsContext = new DnsContext();
            dnsContext.setStore(store);
            session.setAttribute(getContextKey(), dnsContext);

            DomainNameService.execute(dnsContext, request);

            DnsMessage response = dnsContext.getReply();

            session.write(response);
        } catch (Exception e) {
            LOG.error(e.getLocalizedMessage(), e);

            ResponseCode responseCode = ResponseCode.SERVER_FAILURE;

            if (e instanceof DnsException) {
                responseCode = ResponseCode.convert((byte) ((DnsException) e).getResponseCode());
            }

            DnsMessageModifier modifier = new DnsMessageModifier();

            modifier.setTransactionId(request.getTransactionId());
            modifier.setMessageType(MessageType.RESPONSE);
            modifier.setOpCode(OpCode.QUERY);
            modifier.setAuthoritativeAnswer(false);
            modifier.setTruncated(false);
            modifier.setRecursionDesired(request.isRecursionDesired());
            modifier.setRecursionAvailable(false);
            modifier.setReserved(false);
            modifier.setAcceptNonAuthenticatedData(false);
            modifier.setResponseCode(responseCode);
            modifier.setQuestionRecords(request.getQuestionRecords());
            modifier.setAnswerRecords(new ArrayList<>());
            modifier.setAuthorityRecords(new ArrayList<>());
            modifier.setAdditionalRecords(new ArrayList<>());

            session.write(modifier.getDnsMessage());
        }
    }

    public void messageSent(IoSession session, Object message) {
        LOG.debug("{} SENT:  {}", session.getRemoteAddress(), message);
    }

    protected String getContextKey() {
        return (this.contextKey);
    }


    public void inputClosed(IoSession session) {
    }
}
