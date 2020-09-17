package org.videolan.libvlc;

import java.nio.ByteBuffer;

public interface MediaPlayCallback {
    public void onDisplay(ByteBuffer buffer);
}