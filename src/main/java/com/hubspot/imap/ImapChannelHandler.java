package com.hubspot.imap;

import com.hubspot.imap.imap.Response;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImapChannelHandler extends SimpleChannelInboundHandler<Response> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ImapChannelHandler.class);

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Response response) throws Exception {
    LOGGER.info("IMAP Response: {}", response);
  }
}
