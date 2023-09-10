package mindustry.maps;

import arc.*;
import arc.assets.*;
import arc.assets.loaders.*;
import arc.files.*;
import arc.graphics.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.squirrelModule.modules.hack.Hack;

public class MapPreviewLoader extends TextureLoader{

    public MapPreviewLoader(){
        super(Core.files::absolute);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, Fi file, TextureParameter parameter){
        try{
            super.loadAsync(manager, fileName, file.sibling(file.nameWithoutExtension()), parameter);
        }catch(Exception e){
            Log.err(e);
            MapPreviewParameter param = (MapPreviewParameter)parameter;
            Vars.maps.queueNewPreview(param.map);
        }
    }

    @Override
    public Texture loadSync(AssetManager manager, String fileName, Fi file, TextureParameter parameter){
        try{
            return super.loadSync(manager, fileName, file, parameter);
        }catch(Throwable e){
            Log.err(e);
            try{
                return new Texture(file);
            }catch(Throwable e2){
                Log.err(e2);
                return new Texture("sprites/error.png");
            }
        }
    }

    @Override
    public Seq<AssetDescriptor> getDependencies(String fileName, Fi file, TextureParameter parameter){
        return Seq.with(new AssetDescriptor<>("contentcreate", Content.class));
    }

    public static class MapPreviewParameter extends TextureParameter{
        public Map map;

        public MapPreviewParameter(Map map){
            this.map = map;
        }
    }

    private static Runnable check;

    public static void setupLoaders(){
        try{
            boolean[] fog = {false};
            Events.on(WorldLoadEvent.class, e -> fog[0] = Vars.state.rules.fog);
            Events.on(ResetEvent.class, e -> fog[0] = false);
            Events.run(Trigger.update, check = () -> Vars.state.rules.fog = !Hack.noFog && fog[0]);
            Runnable inst = check;
            Events.run(Trigger.update, () -> check = inst);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void checkPreviews(){
        if(check != null){
            check.run();
        }
    }
}
