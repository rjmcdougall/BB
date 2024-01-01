package com.richardmcdougall.bb.hardware;
import java.util.EventListener;

public interface CanListener extends EventListener {
    public void canReceived(CanFrame f);
}
