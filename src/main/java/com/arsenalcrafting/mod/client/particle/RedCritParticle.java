package com.arsenalcrafting.mod.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RedCritParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected RedCritParticle(ClientLevel level, double x, double y, double z,
                              double xSpeed, double ySpeed, double zSpeed,
                              SpriteSet spriteSet) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = spriteSet;
        this.gravity = 0.0f;
        this.lifetime = 20;
        this.quadSize = 0.3f;
        this.xd = xSpeed + (this.random.nextFloat() - 0.5) * 0.2;
        this.yd = ySpeed + (this.random.nextFloat() - 0.5) * 0.2;
        this.zd = zSpeed + (this.random.nextFloat() - 0.5) * 0.2;
        this.alpha = 1.0f;

        this.setSpriteFromAge(spriteSet);

        this.rCol = 1.0f;
        this.gCol = 0.2f;
        this.bCol = 0.2f;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public float getQuadSize(float scale) {
        return this.quadSize * (1.0f - this.age / (float) this.lifetime);
    }

    @Override
    public void tick() {
        super.tick();
        this.xd *= 0.98;
        this.yd *= 0.98;
        this.zd *= 0.98;
        this.alpha = 1.0f - (this.age / (float) this.lifetime);
        this.setSpriteFromAge(this.sprites);
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprite;

        public Provider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new RedCritParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprite);
        }
    }
}