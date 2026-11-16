package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.modulos.fe.Oracion;
import com.lucarita.yonga.modulos.fe.OracionCliente;
import net.minecraft.Util;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * La pose de rezar/blasfemar. La llama PlayerModelMixin justo después de que Minecraft
 * coloque el modelo del jugador (andar, mirar, etc.), así que aquí "sobrescribimos" brazos, piernas y cabeza.
 *
 * Los ángulos están en radianes (π/2 ≈ 1.57 = 90 grados). En xRot, negativo = hacia delante/arriba.
 * Las piernas de Minecraft no tienen rodilla, así que "arrodillarse" es: piernas estiradas hacia atrás
 * en horizontal y todo el cuerpo bajado (eso último se hace en FeClienteEventos).
 */
public final class PoseOracion {

    // Lo que tarda en arrodillarse / levantar los brazos (en milisegundos)
    private static final float DURACION_TRANSICION = 600.0f;

    // Cuánto se baja el cuerpo al arrodillarse (en bloques)
    public static final float BAJADA_CUERPO = 0.58f;

    private PoseOracion() {}

    /** De 0 a 1: cuánto lleva de la animación de arrodillarse. -1 si no está rezando. */
    public static float progreso(LivingEntity entidad) {
        OracionCliente.Estado estado = OracionCliente.estado(entidad.getId());
        if (estado == null) {
            return -1.0f;
        }
        float t = Mth.clamp((Util.getMillis() - estado.inicioMilis()) / DURACION_TRANSICION, 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);   // "smoothstep": empieza y termina suave
    }

    public static void aplicar(PlayerModel<?> modelo, LivingEntity entidad) {
        OracionCliente.Estado estado = OracionCliente.estado(entidad.getId());
        if (estado == null) {
            return;
        }
        float f = progreso(entidad);

        // Piernas: hacia atrás, en horizontal (de rodillas)
        girar(modelo.rightLeg, f, 1.5708f, 0.0f, 0.05f);
        girar(modelo.leftLeg, f, 1.5708f, 0.0f, -0.05f);

        if (estado.direccion() == Oracion.REZAR) {
            // Cabeza un poco agachada, manos juntas delante del pecho
            modelo.head.xRot = Mth.lerp(f, modelo.head.xRot, 0.45f);
            girar(modelo.rightArm, f, -1.05f, -0.45f, 0.0f);
            girar(modelo.leftArm, f, -1.05f, 0.45f, 0.0f);
        } else {
            // Mirando al cielo, brazos en alto y abiertos
            modelo.head.xRot = Mth.lerp(f, modelo.head.xRot, -0.55f);
            girar(modelo.rightArm, f, -2.85f, 0.0f, 0.35f);
            girar(modelo.leftArm, f, -2.85f, 0.0f, -0.35f);
        }
        modelo.body.xRot = Mth.lerp(f, modelo.body.xRot, 0.0f);

        // Las capas exteriores de la skin (sombrero, mangas, pantalones, chaqueta) copian la pose
        modelo.hat.copyFrom(modelo.head);
        modelo.jacket.copyFrom(modelo.body);
        modelo.rightSleeve.copyFrom(modelo.rightArm);
        modelo.leftSleeve.copyFrom(modelo.leftArm);
        modelo.rightPants.copyFrom(modelo.rightLeg);
        modelo.leftPants.copyFrom(modelo.leftLeg);
    }

    // Lleva una parte del cuerpo poco a poco (según f) desde donde está hasta los ángulos indicados
    private static void girar(ModelPart parte, float f, float x, float y, float z) {
        parte.xRot = Mth.lerp(f, parte.xRot, x);
        parte.yRot = Mth.lerp(f, parte.yRot, y);
        parte.zRot = Mth.lerp(f, parte.zRot, z);
    }
}
