package cm.aptoide.pt.spotandshare.socket;

import cm.aptoide.pt.spotandshare.socket.interfaces.SocketBinder;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import lombok.Setter;

/**
 * Created by neuro on 27-01-2017.
 */

public abstract class AptoideClientSocket extends AptoideSocket {

  private static final String TAG = AptoideClientSocket.class.getSimpleName();
  private final String hostName;
  private final int port;
  private String fallbackHostName;
  @Setter private int retries;
  private Socket socket;
  @Setter private SocketBinder socketBinder;

  public AptoideClientSocket(String hostName, String fallbackHostName, int port) {
    this(hostName, port);
    this.fallbackHostName = fallbackHostName;
  }

  public AptoideClientSocket(String hostName, int port) {
    this.hostName = hostName;
    this.port = port;
  }

  public AptoideClientSocket(int bufferSize, String hostName, String fallbackHostName, int port) {
    this(bufferSize, hostName, port);
    this.fallbackHostName = fallbackHostName;
  }

  public AptoideClientSocket(int bufferSize, String hostName, int port) {
    super(bufferSize);
    this.hostName = hostName;
    this.port = port;
  }

  @Override public AptoideSocket start() {
    Print.d(TAG, "start() called with: " + "");
    socket = null;

    String[] hosts = new String[] { hostName, fallbackHostName };

    for (String host : hosts) {
      if (host != null && (socket == null || !socket.isConnected())) {
        retries = 5;

        while ((socket == null || !socket.isConnected()) && retries-- >= 0) {
          try {
            socket = new Socket();
            if (socketBinder != null) {
              socketBinder.bind(socket);
            }
            socket.connect(new InetSocketAddress(host, port));
            Print.d(TAG, "start: Socket connected to " + host + ":" + port);
          } catch (IOException e) {
            Print.d(TAG,
                "start: Failed to connect to " + hostName + ":" + port + ", retries = " + retries);
            if (retries == 0) {
              if (onError != null) {
                onError.onError(e);
              }
            }
            try {
              Thread.sleep(1000);
            } catch (InterruptedException e1) {
              e1.printStackTrace();
            }
          }
        }
      }
    }

    if (socket == null) {
      if (onError != null) {
        onError.onError(new IOException(
            getClass().getSimpleName() + " Couldn't connect to " + hosts + ":" + port));
      }
      return null;
    }

    try {
      onConnected(socket);
    } catch (IOException e) {
      if (onError != null) {
        onError.onError(e);
      }
    } finally {
      try {
        socket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    Print.d(TAG, "start: ShareApps: Thread "
        + Thread.currentThread().getId()
        + " finished "
        + getClass().getSimpleName());
    return this;
  }

  protected abstract void onConnected(Socket socket) throws IOException;

  @Override public void shutdown() {
    super.shutdown();
    try {
      socket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
