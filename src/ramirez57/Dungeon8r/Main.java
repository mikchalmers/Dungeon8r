package ramirez57.Dungeon8r;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {
	Plugin plugin;
	Server mc;
	PluginManager pluginmgr;
	Logger logger;
	World world;
	Random random;
	FileConfiguration config;
	File configFile;
	Location location;
	Chunk chunk;
	Block block;
	CreatureSpawner spawner;
	Chest chest;
	EntityType dutype;
	List<String> dutypes;
	
	public void onEnable() {
		plugin = this;
		mc = this.getServer();
		pluginmgr = mc.getPluginManager();
		logger = this.getLogger();
		random = new Random();
		config = this.getConfig();
		configFile = new File(this.getDataFolder(),"config.yml");
		config.options().copyDefaults(true);
		this.saveConfig();
		pluginmgr.registerEvents(this, this);
	}
	
	public void onDisable() {
		
	}
	
	
	public void makeDungeon(Location loc) {
		boolean generating = true;
		int floor=1;
		dutypes = config.getStringList("dungeon.types");
		dutype = EntityType.fromName(dutypes.get(random.nextInt(dutypes.size())));
		while(generating) 
		{
			int chests = 0;
			loc.subtract(0,1,0);
			for(int i=0;i<9;i++) 
			{
				if(loc.getBlock().getRelative(-1,0,0).getType() == Material.AIR) loc.getBlock().getRelative(-1,0,0).setType(Material.COBBLESTONE);
				loc.getBlock().setType(Material.LADDER);
				loc.getBlock().setData((byte)5);
				loc.subtract(0,1,0);
			}
			loc.subtract(1,-3,1);
			gentile(loc,false);
			int complex = random.nextInt((config.getInt("max_complexity")-config.getInt("min_complexity")))+config.getInt("min_complexity");
			for(int i=0;i<complex;i++) 
			{
				//add new room loc based on random integer (?)
				switch(random.nextInt(4)) 
				{
				case 0:
					loc.add(3,0,0);
					break;
				case 1:
					loc.add(-3,0,0);
					break;
				case 2:
					loc.add(0,0,3);
					break;
				case 3:
					loc.add(0,0,-3);
					break;
				default:
					break;
				}
				
				//test if air block
				if(loc.getBlock().getType() != Material.AIR)
				{
					//call method to generate floor and spawners
					gentile(loc,true);
					//generate chest
					if(chests < floor && random.nextBoolean())
					{	genchest(loc,1);chests++;	}
				}
			}
			if(chests < floor) genchest(loc,(floor-chests));
			if(random.nextBoolean()) 
			{	loc.add(1,-2,1);floor++;chests=0;	}  
			else 	
			{	generating = false;		}
		}
	}
	
	//spawn loot chest
	public void genchest(Location loc, int contents) 
	{
		//go to corner of room							TODO - randomize which corner chest is located in
		loc.add(1,-2,0);loc.getBlock().setType(Material.CHEST);
		block = loc.getBlock();
		chest = (Chest) block.getState();
		List<String> rewards = config.getStringList("dungeon.rewards." + dutype.getName().toUpperCase());
		for(int i=0;i<contents;i++) 
		{
			//parse loot table config
			String[] reward = rewards.get(random.nextInt(rewards.size())).split(":");
			//add item to chest
			chest.getInventory().addItem(new ItemStack(Material.matchMaterial(reward[0]),Integer.parseInt(reward[2]),Short.parseShort(reward[1])));
		}
		//go back to previous location
		loc.add(-1,2,0);
	}
	
	
	public void gentile(Location loc, boolean mobs) 
	{
		//create dungeon floor
		for(int j=0;j<4;j++) 
		{
			for(int i=0;i<3;i++) 
			{
				for(int k=0;k<3;k++) 
				{
					if((loc.getBlock().getType() != Material.LADDER) && (loc.getBlock().getRelative(1, 0, 0).getType() != Material.LADDER) && (loc.getBlock().getType() != Material.CHEST)) 
					{
						if(j==3 && loc.getBlock().getType() == Material.STONE) 
						{	loc.getBlock().setType(Material.MOSSYCOBBLESTONE);	} 
						else if(j<=2) 
						{	loc.getBlock().setTypeId(0);			}
					}
					loc.add(0,0,1);
				}
				loc.subtract(-1,0,3);
			}
			loc.subtract(3,1,0);
		}
		
		//generate spawer
		if(mobs) {
			if(random.nextInt(config.getInt("dungeon.mob_density")) == 0) 
			{
				loc.add(1,1,1);
				block = loc.getBlock();
				block.setType(Material.MOB_SPAWNER);
				spawner = (CreatureSpawner) block.getState();
				spawner.setSpawnedType(dutype);
				loc.subtract(1,1,1);
			}
		}
		loc.add(0,4,0);
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(cmd.getName().equalsIgnoreCase("genhere")) {
			if(sender instanceof Player) {
				Player player = (Player)sender;
				makeDungeon(player.getLocation());
			} else {
				sender.sendMessage("Only players can use this command.");
			}
			return true;
		}
		return false;
	}
	
	@EventHandler
	public void gen(ChunkPopulateEvent e) {
		if(!config.getStringList("worlds").contains(e.getWorld().getName())) return;
		chunk = e.getChunk();
		if(random.nextInt(config.getInt("chance")) == 0) {
			int x,z;
			logger.info("Generated dungeon at chunk (" + chunk.getX() + "," + chunk.getZ() + ")");
			world = chunk.getWorld();
			x = chunk.getX()*16;
			z = chunk.getZ()*16;
			location = new Location(world,x+8,0,z+8);
			makeDungeon(location.getWorld().getHighestBlockAt(location).getLocation());
		}
	}
}
