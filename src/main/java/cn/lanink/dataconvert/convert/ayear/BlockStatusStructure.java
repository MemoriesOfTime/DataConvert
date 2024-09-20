package cn.lanink.dataconvert.convert.ayear;

import java.util.List;

public class BlockStatusStructure {
    private int blockStateHash;
    private int burnOdds;
    private boolean canContainLiquid;
    private List<Double> collisionShape;
    private double explosionResistance;
    private int flameOdds;
    private double friction;
    private double hardness;
    private int light;
    private int lightEmission;
    private String mapColor;
    private String name;
    private double thickness;

    // Getters and Setters
    public int getBlockStateHash() {
        return blockStateHash;
    }

    public void setBlockStateHash(int blockStateHash) {
        this.blockStateHash = blockStateHash;
    }

    public int getBurnOdds() {
        return burnOdds;
    }

    public void setBurnOdds(int burnOdds) {
        this.burnOdds = burnOdds;
    }

    public boolean isCanContainLiquid() {
        return canContainLiquid;
    }

    public void setCanContainLiquid(boolean canContainLiquid) {
        this.canContainLiquid = canContainLiquid;
    }

    public List<Double> getCollisionShape() {
        return collisionShape;
    }

    public void setCollisionShape(List<Double> collisionShape) {
        this.collisionShape = collisionShape;
    }

    public double getExplosionResistance() {
        return explosionResistance;
    }

    public void setExplosionResistance(double explosionResistance) {
        this.explosionResistance = explosionResistance;
    }

    public int getFlameOdds() {
        return flameOdds;
    }

    public void setFlameOdds(int flameOdds) {
        this.flameOdds = flameOdds;
    }

    public double getFriction() {
        return friction;
    }

    public void setFriction(double friction) {
        this.friction = friction;
    }

    public double getHardness() {
        return hardness;
    }

    public void setHardness(double hardness) {
        this.hardness = hardness;
    }

    public int getLight() {
        return light;
    }

    public void setLight(int light) {
        this.light = light;
    }

    public int getLightEmission() {
        return lightEmission;
    }

    public void setLightEmission(int lightEmission) {
        this.lightEmission = lightEmission;
    }

    public String getMapColor() {
        return mapColor;
    }

    public void setMapColor(String mapColor) {
        this.mapColor = mapColor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getThickness() {
        return thickness;
    }

    public void setThickness(double thickness) {
        this.thickness = thickness;
    }

    @Override
    public String toString() {
        return "BlockStatusStructure{" +
                "blockStateHash=" + blockStateHash +
                ", burnOdds=" + burnOdds +
                ", canContainLiquid=" + canContainLiquid +
                ", collisionShape=" + collisionShape +
                ", explosionResistance=" + explosionResistance +
                ", flameOdds=" + flameOdds +
                ", friction=" + friction +
                ", hardness=" + hardness +
                ", light=" + light +
                ", lightEmission=" + lightEmission +
                ", mapColor='" + mapColor + '\'' +
                ", name='" + name + '\'' +
                ", thickness=" + thickness +
                '}';
    }
}
