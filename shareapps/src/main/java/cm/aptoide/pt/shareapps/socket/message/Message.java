package cm.aptoide.pt.shareapps.socket.message;

import cm.aptoide.pt.shareapps.socket.entities.Host;
import java.io.Serializable;
import lombok.Getter;
import lombok.ToString;

@ToString public abstract class Message implements Serializable {

  @Getter protected final Host localHost;

  protected Message(Host localHost) {
    this.localHost = localHost;
  }
}