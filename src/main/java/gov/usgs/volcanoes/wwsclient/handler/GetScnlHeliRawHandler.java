package gov.usgs.volcanoes.wwsclient.handler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.core.Zip;
import gov.usgs.volcanoes.core.data.HelicorderData;
import gov.usgs.volcanoes.wwsclient.ClientUtils;
import io.netty.buffer.ByteBuf;

/**
 * Receive and process response from a winston GETWAVE request.
 *
 * @author Tom Parker
 */
public class GetScnlHeliRawHandler extends AbstractCommandHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(GetScnlHeliRawHandler.class);

  private final HelicorderData heliData;
  private int length;
  private final boolean isCompressed;
  private ByteArrayOutputStream buf;

  /**
   * Constructor.
   * 
   * @param heliData object to be populated. Existing data will be discarded.
   * @param isCompressed if true,request that data be compressed before sending it over the network.
   */
  public GetScnlHeliRawHandler(HelicorderData heliData, boolean isCompressed) {
    this.heliData = heliData;
    this.isCompressed = isCompressed;
    length = -Integer.MAX_VALUE;
    buf = null;
  }

  @Override
  public void handle(ByteBuf msgBuf) throws IOException {
    if (length < 0) {
      String header = ClientUtils.readResponseHeader(msgBuf);
      if (header == null) {
        LOGGER.debug("Still waiting for full response line.");
        return;
      } else {
        String[] parts = header.split(" ");
        length = Integer.parseInt(parts[1]);
        buf = new ByteArrayOutputStream(length);
        LOGGER.debug("Response length: {}", length);
        LOGGER.debug("" + buf);
      }
    }

    msgBuf.readBytes(buf, msgBuf.readableBytes());
    if (buf.size() == length) {
      LOGGER.debug("Received all bytes.");
      byte[] bytes = buf.toByteArray();
      if (bytes.length > 0) {
        if (isCompressed) {
          bytes = Zip.decompress(bytes);
        }
        heliData.fromBinary(ByteBuffer.wrap(bytes));
      }
      sem.release();
    } else {
      LOGGER.debug("Received {} of {} bytes.", buf.size(), length);
    }

  }

}
