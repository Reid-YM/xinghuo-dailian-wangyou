package com.xinghuo.mobilevisual.web;

import com.xinghuo.mobilevisual.application.MobileVisualConfigView;
import com.xinghuo.mobilevisual.application.UpdateMobileVisualConfigCommand;

public final class MobileVisualConfigWebMapper {
    private MobileVisualConfigWebMapper() {
    }

    public static UpdateMobileVisualConfigCommand toCommand(int opacity, int blur) {
        return new UpdateMobileVisualConfigCommand(opacity, blur);
    }

    public static Response toResponse(MobileVisualConfigView view) {
        return new Response(view.heroBackgroundMobileOpacity(), view.heroBackgroundMobileBlur());
    }

    public record Response(int heroBackgroundMobileOpacity, int heroBackgroundMobileBlur) {
    }
}
