package net.zhaiji.ae2feedingcard.client.screen;

import appeng.client.gui.Icon;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.IconButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.zhaiji.ae2feedingcard.AE2FeedingCard;

public class FeedingCardButton extends IconButton {
    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath(AE2FeedingCard.MOD_ID, "textures/guis/feeding_card_icon.png");
    private static final Blitter ICON_BLITTER = Blitter.texture(ICON, 16, 16);

    public FeedingCardButton(OnPress onPress) {
        this(onPress, Component.translatable("gui.ae2feedingcard.feeding_card"));
    }

    public FeedingCardButton(OnPress onPress, Component message) {
        super(onPress);
        setMessage(message);
    }

    @Override
    protected Icon getIcon() {
        return null;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // getIcon返回null，所以super只画背景不画图标，交由后面补上自己的纹理。
        // 不自己重画背景是因为那样会与AE2的IconButton逐行重复，焦点分支当初就是这么漏掉的
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);

        int yOffset = this.isHovered() ? 1 : 0;
        Blitter iconBlitter = ICON_BLITTER.copy().src(0, 0, this.width, this.height);
        if (!this.active) iconBlitter.opacity(0.5f);
        iconBlitter.dest(this.getX(), this.getY() + 1 + yOffset).zOffset(3).blit(guiGraphics);
    }
}
