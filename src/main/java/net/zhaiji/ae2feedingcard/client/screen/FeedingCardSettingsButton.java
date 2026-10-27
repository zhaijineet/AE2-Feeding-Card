package net.zhaiji.ae2feedingcard.client.screen;

import appeng.client.gui.Icon;
import appeng.client.gui.widgets.IconButton;
import net.minecraft.network.chat.Component;

/**
 * 喂食卡界面左栏的齿轮按钮，打开喂食卡的设置界面。
 */
public class FeedingCardSettingsButton extends IconButton {
    public FeedingCardSettingsButton(OnPress onPress) {
        super(onPress);
        setMessage(Component.translatable("gui.ae2feedingcard.feeding_card_settings"));
    }

    @Override
    protected Icon getIcon() {
        return Icon.COG;
    }
}
