package com.lucarita.yonga.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SangreGoteoParticle extends TextureSheetParticle {

    protected SangreGoteoParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.gravity = 0.25F; // Gravedad exacta de las gotas de agua
        this.xd = 0.0; // Sin movimiento lateral
        this.yd = 0.0;
        this.zd = 0.0;

        // El color rojo oscuro (RGB)
        this.rCol = 0.6F;
        this.gCol = 0.0F;
        this.bCol = 0.0F;

        this.lifetime = (int)(40.0 / (Math.random() * 0.8 + 0.2)); // Lo que tarda en desaparecer
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    // El Proveedor que enlaza la clase con la textura
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            SangreGoteoParticle particle = new SangreGoteoParticle(level, x, y, z);
            particle.pickSprite(this.spriteSet);
            return particle;
        }
    }
}