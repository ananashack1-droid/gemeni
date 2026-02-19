package sky.client.util;

import net.minecraft.util.math.Vec3d;
import sky.client.modules.render.Trails;

import java.util.List;

public interface IEntity {
    List<Trails.Trail> getTrails();
    Vec3d getLastTrailPos();
    void setLastTrailPos(Vec3d pos);
}
