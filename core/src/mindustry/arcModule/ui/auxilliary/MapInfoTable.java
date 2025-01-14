package mindustry.arcModule.ui.auxilliary;


import arc.Core;
import arc.graphics.Color;
import arc.graphics.Colors;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.*;
import mindustry.content.*;
import mindustry.editor.*;
import mindustry.gen.*;
import mindustry.input.DesktopInput;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;
import static mindustry.arcModule.ui.RStyles.*;
import static mindustry.input.InputHandler.follow;
import static mindustry.input.InputHandler.followIndex;
import static mindustry.ui.Fonts.stringIcons;
import static mindustry.ui.Styles.cleart;

public class MapInfoTable extends BaseToolsTable{
    private final MapInfoDialog mapInfoDialog = new MapInfoDialog();
    private int uiRowIndex = 0;

    public MapInfoTable(){
        super(Icon.map);
    }

    @Override
    protected void setup(){
        defaults().size(40);

        button(Icon.map, clearAccentNonei, mapInfoDialog::show).tooltip("地图信息");
        button(Items.copper.emoji(), clearLineNonet, this::floorStatisticDialog).tooltip("矿物信息");
        button(Icon.chatSmall, clearAccentNonei, () -> ui.MessageDialog.show()).tooltip("中央监控室");
        button(Icon.playersSmall,clearAccentNonei,()->{
            if(ui.listfrag.players.size>1){
                if(control.input instanceof DesktopInput){
                    ((DesktopInput) control.input).panning = true;
                }
                if(follow == null) follow = ui.listfrag.players.get(0);
                followIndex = (followIndex + 1)>=ui.listfrag.players.size?  0 : followIndex + 1;
                follow = ui.listfrag.players.get(followIndex);
                ui.arcInfo("视角追踪：" + follow.name,3f);
            }
        }).tooltip("切换跟踪玩家");
        button(Icon.starSmall, clearAccentNonei, ui.achievements::show).tooltip("统计与成就");
        if(!mobile) button(Icon.editSmall,clearAccentNonei,this::uiTable).tooltip("ui大全");
    }

    private void floorStatisticDialog(){
        BaseDialog dialog = new BaseDialog("ARC-矿物统计");
        Table table = dialog.cont;
        table.clear();

        table.table(c -> {
            c.add("地表矿").color(getThemeColor()).center().fillX().row();
            c.image().color(getThemeColor()).fillX().row();
            c.table(list -> {
                int i = 0;
                for(Block block : content.blocks().select(b -> b instanceof Floor f && !f.wallOre && f.itemDrop != null)){
                    if(indexer.floorOresCount[block.id] == 0) continue;
                    if(i++ % 4 == 0) list.row();
                    list.add(block.emoji() + " " + block.localizedName + "\n" + indexer.floorOresCount[block.id]).width(100f).height(50f);
                }
            }).row();

            c.add("墙矿").color(getThemeColor()).center().fillX().row();
            c.image().color(getThemeColor()).fillX().row();
            c.table(list -> {
                int i = 0;
                for(Block block : content.blocks().select(b -> ((b instanceof Floor f && f.wallOre) || b instanceof StaticWall) && b.itemDrop != null)){
                    if(indexer.wallOresCount[block.id] == 0) continue;
                    if(i++ % 4 == 0) list.row();
                    list.add(block.emoji() + " " + block.localizedName + "\n" + indexer.wallOresCount[block.id]).width(100f).height(50f);
                }
            }).row();

            c.add("液体").color(getThemeColor()).center().fillX().row();
            c.image().color(getThemeColor()).fillX().row();
            c.table(list -> {
                int i = 0;
                for(Block block : content.blocks().select(b -> ((b instanceof Floor f && f.liquidDrop != null)))){
                    if(indexer.floorOresCount[block.id] == 0) continue;
                    if(i++ % 4 == 0) list.row();
                    list.add(block.emoji() + " " + block.localizedName + "\n" + indexer.floorOresCount[block.id]).width(100f).height(50f);
                }
            }).row();
        });
        dialog.addCloseButton();
        dialog.show();
    }

    private void uiTable(){
        BaseDialog dialog = new BaseDialog("ARC-ui大全");
        uiRowIndex = 0;
        TextField sField = dialog.cont.field("", text->{}).fillX().get();
        dialog.cont.row();

        dialog.cont.pane(c -> {
            c.add("颜色").color(getThemeColor()).center().fillX().row();
            c.image().color(getThemeColor()).fillX().row();
            c.table(ct->{
                for(var colorEntry : Colors.getColors()){
                    Color value = colorEntry.value;
                    String key = colorEntry.key;
                    ct.button("[#" + value  +"]" + key,cleart, ()->{
                        Core.app.setClipboardText("[#" + value  +"]");
                        sField.setText(sField.getText() + "[#" + value  +"]");
                    }).size(50f).tooltip(key);
                    uiRowIndex+=1;
                    if(uiRowIndex%15==0) ct.row();
                }
            }).row();
            c.add("物品").color(getThemeColor()).center().fillX().row();
            c.image().color(getThemeColor()).fillX().row();
            c.table(ct->{
                uiRowIndex = 0;
                stringIcons.copy().each((name,iconc)->{
                    ct.button(iconc,cleart, ()->{
                        Core.app.setClipboardText(iconc);
                        sField.setText(sField.getText() + iconc);
                    }).size(50f).tooltip(name);
                    uiRowIndex+=1;
                    if(uiRowIndex%15==0) ct.row();
                });
            }).row();
            c.add("图标").color(getThemeColor()).center().fillX().row();
            c.image().color(getThemeColor()).fillX().row();
            c.table(ct->{
                uiRowIndex = 0;
                for(int i=0;i<Iconc.all.length();i++){
                    String subString = Iconc.all.substring(i,i+1);
                    ct.button(subString,cleart, ()->{
                        Core.app.setClipboardText(subString);
                        sField.setText(sField.getText() + subString);
                    }).size(50f).tooltip(subString);
                    uiRowIndex+=1;
                    if(uiRowIndex%15==0) ct.row();
                }
            }).row();
        }).row();

        dialog.addCloseButton();
        dialog.show();
    }

}
