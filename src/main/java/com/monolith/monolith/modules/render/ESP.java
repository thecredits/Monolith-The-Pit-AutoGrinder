package com.monolith.monolith.modules.render;
import com.monolith.monolith.modules.combat.AntiBot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

public class ESP {

    // --- SETTINGS ---
    public static boolean enabled = false;
    public static boolean box = false;
    public static boolean boxFilled = false;
    public static boolean skeleton = false;

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void render(float partialTicks) {
        if (!enabled) return;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            // 1. Ignore self
            if (player == mc.thePlayer) continue;

            // 2. Basic Validity Checks
            if (player.isDead || player.isInvisible()) continue;

            // 3. ANTI-BOT INTEGRATION
            // If the AntiBot says this player is a bot, skip rendering it.
            // This hides Watchdog bots and NPCs from your screen.
            if (AntiBot.isBot(player)) continue;

            GL11.glPushMatrix();

            // Standard GL Setup for 2D/3D overlays
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            // ALWAYS SHOW THROUGH WALLS
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);

            // Interpolate position
            double x = interpolate(player.lastTickPosX, player.posX, partialTicks) - mc.getRenderManager().viewerPosX;
            double y = interpolate(player.lastTickPosY, player.posY, partialTicks) - mc.getRenderManager().viewerPosY;
            double z = interpolate(player.lastTickPosZ, player.posZ, partialTicks) - mc.getRenderManager().viewerPosZ;

            // DRAW BOX
            if (box || boxFilled) {
                drawBox(player, x, y, z);
            }

            // DRAW SKELETON
            if (skeleton) {
                drawSkeleton(player, x, y, z, partialTicks);
            }

            // Restore GL State
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(true);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glPopMatrix();
        }
    }

    private static void drawBox(EntityPlayer player, double x, double y, double z) {
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);

        double width = player.width / 2.0 + 0.1;
        double height = player.height + 0.1;

        AxisAlignedBB bb = new AxisAlignedBB(-width, 0, -width, width, height, width);

        if (boxFilled) {
            setColor(0x40FF0000); // Transparent Red
            drawSolidBox(bb);
        }

        if (box) {
            GL11.glLineWidth(1.5f);
            setColor(0xFFFF0000); // Solid Red
            RenderGlobal.drawSelectionBoundingBox(bb);
        }

        GL11.glPopMatrix();
    }

    private static void drawSkeleton(EntityPlayer player, double x, double y, double z, float pt) {
        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);

        GL11.glLineWidth(1.0f);
        setColor(0xFFFFFFFF); // White

        boolean sneak = player.isSneaking();
        float bodyYaw = interpolateRotation(player.prevRenderYawOffset, player.renderYawOffset, pt);

        double rads = bodyYaw * 0.01745329251; // Degrees to Radians
        double cos = Math.cos(rads);
        double sin = Math.sin(rads);

        // Approximate joint heights
        double hHead = sneak ? 1.5 : 1.75;
        double hNeck = sneak ? 1.3 : 1.5;
        double hHip  = sneak ? 0.5 : 0.7;

        // Spine
        drawLine(0, hHead, 0, 0, hNeck, 0);
        drawLine(0, hNeck, 0, 0, hHip, 0);

        // Shoulders
        double wShoulder = 0.35;
        double xSho1 = -sin * wShoulder; double zSho1 = cos * wShoulder;
        double xSho2 = sin * wShoulder;  double zSho2 = -cos * wShoulder;
        drawLine(0, hNeck, 0, xSho1, hNeck, zSho1);
        drawLine(0, hNeck, 0, xSho2, hNeck, zSho2);

        // Arms
        double hHand = hNeck - 0.7;
        drawLine(xSho1, hNeck, zSho1, xSho1, hHand, zSho1);
        drawLine(xSho2, hNeck, zSho2, xSho2, hHand, zSho2);

        // Legs
        double wHip = 0.15;
        double xHip1 = -sin * wHip; double zHip1 = cos * wHip;
        double xHip2 = sin * wHip;  double zHip2 = -cos * wHip;
        drawLine(0, hHip, 0, xHip1, hHip, zHip1);
        drawLine(0, hHip, 0, xHip2, hHip, zHip2);

        drawLine(xHip1, hHip, zHip1, xHip1, 0, zHip1);
        drawLine(xHip2, hHip, zHip2, xHip2, 0, zHip2);

        GL11.glPopMatrix();
    }

    private static void drawLine(double x1, double y1, double z1, double x2, double y2, double z2) {
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(x1, y1, z1);
        GL11.glVertex3d(x2, y2, z2);
        GL11.glEnd();
    }

    private static void drawSolidBox(AxisAlignedBB bb) {
        GL11.glBegin(GL11.GL_QUADS);
        // Bottom
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ); GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
        // Top
        GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
        // Front
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ);
        // Back
        GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ); GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ);
        // Left
        GL11.glVertex3d(bb.minX, bb.minY, bb.minZ); GL11.glVertex3d(bb.minX, bb.minY, bb.maxZ);
        GL11.glVertex3d(bb.minX, bb.maxY, bb.maxZ); GL11.glVertex3d(bb.minX, bb.maxY, bb.minZ);
        // Right
        GL11.glVertex3d(bb.maxX, bb.minY, bb.minZ); GL11.glVertex3d(bb.maxX, bb.maxY, bb.minZ);
        GL11.glVertex3d(bb.maxX, bb.maxY, bb.maxZ); GL11.glVertex3d(bb.maxX, bb.minY, bb.maxZ);
        GL11.glEnd();
    }

    private static double interpolate(double prev, double now, float pt) { return prev + (now - prev) * pt; }

    private static float interpolateRotation(float prev, float now, float pt) {
        float f = now - prev;
        while (f < -180.0F) f += 360.0F; while (f >= 180.0F) f -= 360.0F;
        return prev + pt * f;
    }

    private static void setColor(int color) {
        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        GL11.glColor4f(r, g, b, a);
    }
}
