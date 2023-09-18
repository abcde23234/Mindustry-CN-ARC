package mindustry.squirrelModule.ui;

import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.g2d.NinePatch;
import arc.scene.style.Drawable;
import arc.scene.style.NinePatchDrawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.TextButton;
import mindustry.gen.Tex;

import static mindustry.gen.Tex.underlineWhite;
import static mindustry.ui.Styles.fullTogglet;
import static mindustry.ui.Styles.none;

public class SStyles {
    static final int cornerRadius = 50, size = 512;
    static final int cornerColor = new Color(60, 60, 60).rgba8888();
    public static Drawable cornerDrawable;
    public static TextButton.TextButtonStyle clearTogglet;

    static {
        Pixmap pixmap = new Pixmap(size, size);
        pixmap.fillRect(cornerRadius, 0, size - 2 * cornerRadius, size, cornerColor);
        pixmap.fillRect(0, cornerRadius, size, size - 2 * cornerRadius, cornerColor);
        pixmap.fillCircle(cornerRadius, cornerRadius, cornerRadius, cornerColor);
        pixmap.fillCircle(cornerRadius, size - cornerRadius, cornerRadius, cornerColor);
        pixmap.fillCircle(size - cornerRadius, cornerRadius, cornerRadius, cornerColor);
        pixmap.fillCircle(size - cornerRadius, size - cornerRadius, cornerRadius, cornerColor);
        NinePatch borderPatch = new NinePatch(new Texture(pixmap), cornerRadius, cornerRadius, cornerRadius, cornerRadius);
        pixmap.dispose();
        cornerDrawable = new NinePatchDrawable(borderPatch);

        clearTogglet = new TextButton.TextButtonStyle(fullTogglet) {{
            up = none;
            over = none;
            down = none;
            checked = underlineWhite;
            disabledFontColor = Color.white;
        }};

    }

    public static Drawable tint(float r, float g, float b, float a) {
        return ((TextureRegionDrawable) Tex.whiteui).tint(r, g, b, a);
    }

    public static Drawable tint(Color c) {
        return ((TextureRegionDrawable) Tex.whiteui).tint(c);
    }

}
