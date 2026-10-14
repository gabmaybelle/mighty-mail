package com.mrcrayfish.mightymail.client.gui.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/**
 * Author: MrCrayfish
 */
public class IconButton extends Button
{
    private final int iconU;
    private final int iconV;
    private final int iconWidth;
    private final int iconHeight;
    private final Identifier texture;
    private final int sourceWidth;
    private final int sourceHeight;

    public IconButton(int x, int y, int iconU, int iconV, int iconWidth, int iconHeight, Identifier texture, int sourceWidth, int sourceHeight, OnPress onPress)
    {
        super(x, y, 18, 18, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
        this.iconU = iconU;
        this.iconV = iconV;
        this.iconWidth = iconWidth;
        this.iconHeight = iconHeight;
        this.texture = texture;
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks)
    {
        this.extractDefaultSprite(graphics);
        int contentLeft = (this.width - this.iconWidth) / 2;
        int contentTop = (this.height - this.iconHeight) / 2;
        int iconX = this.getX() + contentLeft;
        int iconY = this.getY() + contentTop;
        float brightness = this.active ? 1.0F : 0.5F;
        int color = ARGB.colorFromFloat(this.alpha, brightness, brightness, brightness);
        graphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, iconX, iconY, this.iconU, this.iconV, 10, 10, this.sourceWidth, this.sourceHeight, color);
    }

    /*@Override
    protected ClientTooltipPositioner createTooltipPositioner()
    {
        return DefaultTooltipPositioner.INSTANCE;
    }*/
}
