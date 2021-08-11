
package xyz.coolsa.biosphere;

public class BiosphereConfig {
    public int sphereDistance;
    public int sphereRadius;
    public int shoreRadius;
    public int lakeRadius;

    public BiosphereConfig(int spr, int shr, int lr) {
        this.sphereDistance = spr * 4;
        this.sphereRadius = spr;
        this.shoreRadius = shr;
        this.lakeRadius = lr;
    }
}