package mindustry.world;

import mindustry.game.*;
import mindustry.gen.*;
import mindustry.world.modules.*;

/**
 * A tile which does not trigger change events and whose entity types are cached.
 * Prevents garbage when loading previews.
 */
public class CachedTile extends Tile{

    public CachedTile(){
        super(0, 0);
    }

    @Override
    protected void preChanged(){
        //this basically overrides the old tile code and doesn't remove from proximity
    }

    @Override
    protected void changed(Team team){
        entity = null;

        Block block = block();

        if(block.hasEntity()){
            Tilec n = block.newEntity();
            n.cons(new ConsumeModule(entity));
            n.tile(this);
            n.block(block);
            if(block.hasItems) n.items(new ItemModule());
            if(block.hasLiquids) n.liquids(new LiquidModule());
            if(block.hasPower) n.power(new PowerModule());
            entity = n;
        }
    }
}
