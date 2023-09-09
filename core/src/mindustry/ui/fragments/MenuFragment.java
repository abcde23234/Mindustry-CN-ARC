package mindustry.ui.fragments;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Interp;
import arc.math.Mathf;
import arc.math.Rand;
import arc.scene.Group;
import arc.scene.actions.Actions;
import arc.scene.event.Touchable;
import arc.scene.style.Drawable;
import arc.scene.ui.Button;
import arc.scene.ui.Image;
import arc.scene.ui.ImageButton.ImageButtonStyle;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.scene.ui.layout.WidgetGroup;
import arc.struct.Seq;
import arc.util.*;
import mindustry.Vars;
import mindustry.arcModule.ui.window.Window;
import mindustry.core.Version;
import mindustry.game.EventType;
import mindustry.game.EventType.ResizeEvent;
import mindustry.gen.Icon;
import mindustry.graphics.MenuRenderer;
import mindustry.graphics.Pal;
import mindustry.service.GameService;
import mindustry.ui.Fonts;
import mindustry.ui.MobileButton;
import mindustry.ui.Styles;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static mindustry.Vars.*;
import static mindustry.gen.Tex.discordBanner;

public class MenuFragment{
    private Table container, submenu;
    private Button currentMenu;
    private MenuRenderer renderer;
    private Seq<MenuButton> customButtons = new Seq<>();
    Label textLabel;
    float tx, ty, base;
    String[] labels = { "[yellow]学术端!" };
    float period = 75f;
    float varSize = 0.8f;
    String text = labels[0];
    public void build(Group parent){
        renderer = new MenuRenderer();

        if (!Core.settings.getBool("arcDisableModWarning")){
            ui.aboutcn_arc.show();
        }
        Core.settings.put("locale", "zh_CN");

        Group group = new WidgetGroup();
        group.setFillParent(true);
        group.visible(() -> !ui.editor.isShown());
        parent.addChild(group);

        WidgetGroup textGroup = new WidgetGroup();
        parent.addChild(textGroup);

        parent = group;

        parent.fill((x, y, w, h) -> renderer.render());

        parent.fill(c -> {
            c.pane(Styles.noBarPane, cont -> {
                container = cont;
                cont.name = "menu container";

                if(!mobile){
                    c.left();
                    buildDesktop();
                    Events.on(ResizeEvent.class, event -> buildDesktop());
                }else{
                    buildMobile();
                    Events.on(ResizeEvent.class, event -> buildMobile());
                }
            }).with(pane -> {
                pane.setOverscroll(false, false);
            }).grow();
        });

        parent.fill(c -> c.bottom().right().button(Icon.discord, new ImageButtonStyle(){{
            up = discordBanner;
        }}, ui.discord::show).marginTop(9f).marginLeft(10f).tooltip("@discord").size(84, 45).name("discord"));

        parent.fill(c -> c.bottom().right().button("检查更新", Icon.refresh, () -> {
            ui.loadfrag.show();
            becontrol.checkUpdate(result -> {
                ui.loadfrag.hide();
                becontrol.BeControlTable();
            });
        }).size(200, 60).name("检查更新").update(t -> {
            t.getLabel().setColor(becontrol.isUpdateAvailable() ? Tmp.c1.set(Color.white).lerp(Pal.accent, Mathf.absin(5f, 1f)) : Color.white);
        }));


        String versionText = ((Version.build == -1) ? "[#fc8140aa]" : "[cyan]") + Version.combined();
        String arcversionText = "\n[cyan]ARC version:" + Version.arcBuild;
        parent.fill((x, y, w, h) -> {
            TextureRegion logo = Core.atlas.find("logo");
            float width = Core.graphics.getWidth(), height = Core.graphics.getHeight() - Core.scene.marginTop;
            float logoscl = Scl.scl(1) * logo.scale;
            float logow = Math.min(logo.width * logoscl, Core.graphics.getWidth() - Scl.scl(20));
            float logoh = logow * (float)logo.height / logo.width;

            float fx = (int)(width / 2f);
            float fy = (int)(height - 6 - logoh) + logoh / 2 - (Core.graphics.isPortrait() ? Scl.scl(30f) : 0f);
            if(Core.settings.getBool("macnotch") ){
                fy -= Scl.scl(macNotchHeight);
            }

            tx = width / 2f + logow * 0.35f;
            ty = fy - logoh / 2f - Scl.scl(2f) + logoh * 0.15f;
            base = logoh * 0.03f;

            Draw.color();
            Draw.rect(logo, fx, fy, logow, logoh);

            Fonts.outline.setColor(Color.white);
            Fonts.outline.draw(versionText+arcversionText, fx, fy - logoh/2f - Scl.scl(2f), Align.center);
        }).touchable = Touchable.disabled;

        textGroup.setTransform(true);
        textGroup.setRotation(20);
        textGroup.addChild(textLabel = new Label(""));
        textGroup.visible(() -> Core.settings.getBool("menuFloatText", true));
        textLabel.setAlignment(Align.center);
        textGroup.update(() -> {
            textGroup.x = tx;
            textGroup.y = ty;
            textLabel.setFontScale((base == 0 ? 1f : base) * Math.abs(Time.time % period / period - 0.5f) * varSize + 1);
            textLabel.setText(text);
        });
        Events.on(EventType.ClientLoadEvent.class, event -> Time.run(10f, () -> Http.get("https://cn-arc.github.io/labels?t=" + Time.millis())
                .error(e -> {
                    Log.err("获取最新主页标语失败!加载本地标语", e);
                    labels = Core.files.internal("labels").readString("UTF-8").replace("\r", "").replace("\\n", "\n").replace("/n", "\n").split("\n");
                    Core.app.post(this::randomLabel);
                })
                .submit(result -> {
                    labels = result.getResultAsString().replace("\r", "").replace("\\n", "\n").replace("/n", "\n").split("\n");
                    Core.app.post(this::randomLabel);
                })
        ));


        Events.on(EventType.ClientLoadEvent.class, event -> {
            if (Core.settings.getBool("arcNews", true)) {
                Time.run(10f, () -> Http.get("https://cn-arc.github.io/news?t=" + Time.millis(), result -> {
                    String s = result.getResultAsString();
                    Core.app.post(() -> {
                        Pattern imgPattern = Pattern.compile("\\{image:(.*?)\\}");//dalvik特性 花括号必须转义
                        String[] news = s.replace("\r", "").split("\n");
                        boolean haveNews = false;
                        Table t = new Table();
                        ScrollPane p = new ScrollPane(t);
                        for (int i = 0; i < news.length; i += 3) {
                            if (Time.millis() - Long.parseLong(news[0]) < 86400000 * 14) {//14天
                                haveNews = true;
                                t.table(t2 -> {
                                    t2.image().color(Vars.getThemeColor()).height(3).growX().row();
                                    t2.add(formatTimeElapsed(Time.millis() - Long.parseLong(news[0]))).align(Align.left).row();
                                    t2.table(t3 -> {
                                        String n = news[1].replace("\\n", "\n");
                                        Matcher matcher = imgPattern.matcher(n);
                                        int previousEnd = 0;
                                        while (matcher.find()) {
                                            int start = matcher.start();
                                            int end = matcher.end();
                                            if (previousEnd < start) {
                                                t3.add(n.substring(previousEnd, start)).row();
                                            }
                                            Image img = t3.image(Icon.refresh.getRegion()).size(128).get();
                                            t3.row();
                                            Http.get(matcher.group(1), r -> {
                                                byte[] b = r.getResult();
                                                Core.app.post(() -> {
                                                    Pixmap pix = new Pixmap(b);
                                                    t3.getCell(img).size(pix.width, pix.height);
                                                    img.setDrawable(new TextureRegion(new Texture(pix)));
                                                    pix.dispose();
                                                });
                                            });
                                            previousEnd = end;
                                        }
                                        if (previousEnd < n.length()) {
                                            t3.add(n.substring(previousEnd));
                                        }
                                    }).growX().row();
                                    t2.add(news[2]).align(Align.right);
                                }).pad(5).growX().row();
                            }
                        }
                        if (!haveNews) {
                            t.add("这里什么都没有");
                        }
                        Window w = new Window("学术日报", 600, 400, Icon.book.getRegion(), ui.WindowManager);
                        w.setBody(new Table(t2 -> t2.add(p).grow()) {{
                            setBackground(Styles.black3);
                        }});
                        w.add();
                    });
                }));
            }
        });
    }

    public static String formatTimeElapsed(long milliseconds) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        long days = TimeUnit.MILLISECONDS.toDays(milliseconds);

        if (days > 0) {
            return days + "天前";
        } else if (hours > 0) {
            return hours + "小时前";
        } else if (minutes > 0) {
            return minutes + "分钟前";
        } else {
            return seconds + "秒前";
        }
    }

    private void randomLabel(){
        Timer.schedule(() -> text = "[yellow]" + labels[new Rand().random(0, labels.length - 1)], 0.11f);
    }

    private void buildMobile(){
        container.clear();
        container.name = "buttons";
        container.setSize(Core.graphics.getWidth(), Core.graphics.getHeight());

        float size = 120f;
        container.defaults().size(size).pad(5).padTop(4f);

        initAchievement();

        MobileButton
            play = new MobileButton(Icon.play, "@campaign", () -> checkPlay(ui.planet::show)),
            custom = new MobileButton(Icon.rightOpenOut, "@customgame", () -> checkPlay(ui.custom::show)),
            maps = new MobileButton(Icon.download, "@loadgame", () -> checkPlay(ui.load::show)),
            join = new MobileButton(Icon.add, "@joingame", () -> checkPlay(ui.join::show)),
            editor = new MobileButton(Icon.terrain, "@editor", () -> checkPlay(ui.maps::show)),
            tools = new MobileButton(Icon.settings, "@settings", ui.settings::show),
            mods = new MobileButton(Icon.book, "@mods", ui.mods::show),
            exit = new MobileButton(Icon.exit, "@quit", () -> Core.app.exit()),
            cn_arc = new MobileButton(Icon.info,"@aboutcn_arc.button",  ui.aboutcn_arc::show),
            //mindustrywiki = new MobileButton(Icon.book, "@mindustrywiki.button", ui.mindustrywiki::show),
            updatedialog = new MobileButton(Icon.info,"@updatedialog.button",  ui.updatedialog::show),
            database = new MobileButton(Icon.book, "@database",  ui.database::show),
            achievements = new MobileButton(Icon.star, "@achievements",  ui.achievements::show);

        play.clicked(this::randomLabel);
        custom.clicked(this::randomLabel);
        maps.clicked(this::randomLabel);
        join.clicked(this::randomLabel);
        editor.clicked(this::randomLabel);
        tools.clicked(this::randomLabel);
        mods.clicked(this::randomLabel);
        cn_arc.clicked(this::randomLabel);
        updatedialog.clicked(this::randomLabel);
        database.clicked(this::randomLabel);
        achievements.clicked(this::randomLabel);

        Seq<MobileButton> customs = customButtons.map(b -> new MobileButton(b.icon, b.text, b.runnable == null ? () -> {} : b.runnable));

        if(!Core.graphics.isPortrait()){
            container.marginTop(60f);
            if(Core.settings.getInt("changelogreaded") == changeLogRead){
                container.add(play);
                container.add(join);
                container.add(custom);
                container.add(maps);
                // add odd custom buttons
                for(int i = 1; i < customs.size; i += 2){
                    customs.get(i).clicked(this::randomLabel);
                    container.add(customs.get(i));
                }
                container.row();
                container.add(editor);
            }
            container.add(tools);
            container.add(mods);
            container.add(achievements);
            // add even custom buttons (before the exit button)
            for(int i = 0; i < customs.size; i += 2){
                customs.get(i).clicked(this::randomLabel);
                container.add(customs.get(i));
            }
            container.row();
            container.add(cn_arc);
            container.add(updatedialog);
            container.add(database);
            if(!ios) container.add(exit);
        }else{
            container.marginTop(0f);
            if(Core.settings.getInt("changelogreaded") == changeLogRead){
                container.add(play);
                container.add(maps);
                container.row();
                container.add(custom);
                container.add(join);
                container.row();
                container.add(editor);
            }
            container.add(tools);
            container.row();
            container.add(mods);
            // add custom buttons
            for(int i = 0; i < customs.size; i++){
                customs.get(i).clicked(this::randomLabel);
                container.add(customs.get(i));
                if(i % 2 == 0) container.row();
            }
            if(!ios) container.add(exit);
            container.row();
            container.add(cn_arc);
            container.add(database);
            container.row();
            container.add(achievements);
            container.add(updatedialog);
        }
    }

    void initAchievement(){
        service = new GameService(){
            @Override
            public boolean enabled(){ return true; }

            @Override
            public void completeAchievement(String name){
                Core.settings.put("achievement." + name, true);
                //TODO draw the sprite of the achievement
                ui.hudfrag.showToast(Core.atlas.getDrawable("error"), Core.bundle.get("achievement.unlocked") +"\n"+ Core.bundle.get("achievement."+name+".name"));
            }

            @Override
            public boolean isAchieved(String name){
                return Core.settings.getBool("achievement." + name, false);
            }

            @Override
            public int getStat(String name, int def) {
                return Core.settings.getInt("achievementstat." + name, def);
            }

            @Override
            public void setStat(String name, int amount) {
                Core.settings.put("achievementstat." + name, amount);
            }
        };

        service.init();
    }

    private void buildDesktop(){
        container.clear();
        container.setSize(Core.graphics.getWidth(), Core.graphics.getHeight());

        float width = 230f;
        Drawable background = Styles.black6;

        container.left();
        container.add().width(Core.graphics.getWidth()/10f);
        container.table(background, t -> {
            t.defaults().width(width).height(70f);
            t.name = "buttons";

            if(Core.settings.getInt("changelogreaded") != changeLogRead) {
                buttons(t,
                    new MenuButton("@database.button", Icon.menu,
                            new MenuButton("@schematics", Icon.paste, ui.schematics::show),
                            new MenuButton("@database", Icon.book, ui.database::show),
                            new MenuButton("@about.button", Icon.info, ui.about::show),
                            new MenuButton("@updatedialog.button", Icon.distribution, ui.updatedialog::show)
                    ),
                    new MenuButton("@settings", Icon.settings, ui.settings::show),
                    new MenuButton("@aboutcn_arc.button", Icon.info, ui.aboutcn_arc::show)
                );
            } else {
                buttons(t,
                    new MenuButton("@play", Icon.play,
                        new MenuButton("@campaign", Icon.play, () -> checkPlay(ui.planet::show)),
                        new MenuButton("@joingame", Icon.add, () -> checkPlay(ui.join::show)),
                        new MenuButton("@customgame", Icon.terrain, () -> checkPlay(ui.custom::show)),
                        new MenuButton("@loadgame", Icon.download, () -> checkPlay(ui.load::show)),
                        new MenuButton("@editor", Icon.terrain, () -> checkPlay(ui.maps::show)), steam ? new MenuButton("@workshop", Icon.steam, platform::openWorkshop) : null
                    ),
                    new MenuButton("@database.button", Icon.menu,
                        new MenuButton("@schematics", Icon.paste, ui.schematics::show),
                        new MenuButton("@database", Icon.book, ui.database::show),
                        new MenuButton("@about.button", Icon.info, ui.about::show),
                        new MenuButton("@updatedialog.button", Icon.distribution, ui.updatedialog::show)
                    ),

                    new MenuButton("@achievements", Icon.star, ui.achievements::show),
                    new MenuButton("@mods", Icon.book, ui.mods::show),
                    new MenuButton("@settings", Icon.settings, ui.settings::show),
                    new MenuButton("@aboutcn_arc.button", Icon.info, ui.aboutcn_arc::show)
                );

            }
            buttons(t, customButtons.toArray(MenuButton.class));
            buttons(t, new MenuButton("@quit", Icon.exit, Core.app::exit));
        }).width(width).growY();

        container.table(background, t -> {
            submenu = t;
            t.name = "submenu";
            t.color.a = 0f;
            t.top();
            t.defaults().width(width).height(70f);
            t.visible(() -> !t.getChildren().isEmpty());

        }).width(width).growY();
    }

    private void checkPlay(Runnable run){
        if(!mods.hasContentErrors()){
            run.run();
        }else{
            ui.showInfo("@mod.noerrorplay");
        }
    }

    private void fadeInMenu(){
        submenu.clearActions();
        submenu.actions(Actions.alpha(1f, 0.15f, Interp.fade));
    }

    private void fadeOutMenu(){
        //nothing to fade out
        if(submenu.getChildren().isEmpty()){
            return;
        }

        submenu.clearActions();
        submenu.actions(Actions.alpha(1f), Actions.alpha(0f, 0.2f, Interp.fade), Actions.run(() -> submenu.clearChildren()));
    }

    private void buttons(Table t, MenuButton... buttons){
        for(MenuButton b : buttons){
            if(b == null) continue;
            Button[] out = {null};
            out[0] = t.button(b.text, b.icon, Styles.flatToggleMenut, () -> {
                if(currentMenu == out[0]){
                    currentMenu = null;
                    fadeOutMenu();
                }else{
                    if(b.submenu != null){
                        currentMenu = out[0];
                        submenu.clearChildren();
                        fadeInMenu();
                        //correctly offset the button
                        submenu.add().height((Core.graphics.getHeight() - Core.scene.marginTop - Core.scene.marginBottom - out[0].getY(Align.topLeft)) / Scl.scl(1f));
                        submenu.row();
                        buttons(submenu, b.submenu);
                    }else{
                        currentMenu = null;
                        fadeOutMenu();
                        randomLabel();
                        b.runnable.run();
                    }
                }
            }).marginLeft(11f).get();
            out[0].update(() -> out[0].setChecked(currentMenu == out[0]));
            t.row();
        }
    }

    /** Adds a custom button to the menu. */
    public void addButton(String text, Drawable icon, Runnable callback){
        addButton(new MenuButton(text, icon, callback));
    }

    /** Adds a custom button to the menu. */
    public void addButton(String text, Runnable callback){
        addButton(text, Styles.none, callback);
    }

    /**
     * Adds a custom button to the menu.
     * If {@link MenuButton#submenu} is null or the player is on mobile, {@link MenuButton#runnable} is invoked on click.
     * Otherwise, {@link MenuButton#submenu} is shown.
     */
    public void addButton(MenuButton button){
        customButtons.add(button);
    }

    /** Represents a menu button definition. */
    public static class MenuButton{
        public final Drawable icon;
        public final String text;
        /** Runnable ran when the button is clicked. Ignored on desktop if {@link #submenu} is not null. */
        public final Runnable runnable;
        /** Submenu shown when this button is clicked. Used instead of {@link #runnable} on desktop. */
        public final @Nullable MenuButton[] submenu;

        /** Constructs a simple menu button, which behaves the same way on desktop and mobile. */
        public MenuButton(String text, Drawable icon, Runnable runnable){
            this.icon = icon;
            this.text = text;
            this.runnable = runnable;
            this.submenu = null;
        }

        /** Constructs a button that runs the runnable when clicked on mobile or shows the submenu on desktop. */
        public MenuButton(String text, Drawable icon, Runnable runnable, MenuButton... submenu){
            this.icon = icon;
            this.text = text;
            this.runnable = runnable;
            this.submenu = submenu;
        }

        /** Comstructs a desktop-only button; used internally. */
        MenuButton(String text, Drawable icon, MenuButton... submenu){
            this.icon = icon;
            this.text = text;
            this.runnable = () -> {};
            this.submenu = submenu;
        }
    }
}
