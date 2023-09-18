package mindustry.squirrelModule.modules.tools;

import arc.scene.ui.layout.Table;
import mindustry.arcModule.ui.window.Window;
import mindustry.gen.Icon;
import mindustry.ui.Styles;

public class RendererTools {
    public static void showSettings() {
        Window w = new Window();
        w.setIcon(Icon.eye.getRegion());
        w.setTitle("tools");
        Table t = new Table();
        w.setBody(t);
        t.setBackground(Styles.black3);
    }
}
