package forked.godlycow.org.splinecartunlocked.block.entity;

import forked.godlycow.org.splinecartunlocked.block.TrackGeometry;
import forked.godlycow.org.splinecartunlocked.block.TrackTiesBlockEntity;

public class ClientTrackGeometry extends TrackGeometry {
    public ClientTrackGeometry(TrackTiesBlockEntity trackTies) {
        super(trackTies);
    }

    @Override
    public void close() {
        super.close();
    }
}
