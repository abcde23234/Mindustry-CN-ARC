package mindustry.squirrelModule.modules.hack;

import arc.scene.Element;
import mindustry.squirrelModule.ui.MemorySlider;

public class Config {
    public String displayName, internalName;
    public Element[] element;
    public HackFunc func;
    public boolean enabled;
    public Config(String displayName, Element[] element, HackFunc func) {
        this.displayName = displayName;
        this.element = element;
        this.func = func;
        func.config = this;
        if (element != null) {
            for (Element e : element) {
                if (e instanceof MemorySlider m) {
                    m.conf = this;
                }
            }
        }
    }
}
