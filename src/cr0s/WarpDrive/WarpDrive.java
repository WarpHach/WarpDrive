package cr0s.WarpDrive;

import cpw.mods.fml.common.FMLCommonHandler;
import java.util.List;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.Mod.ServerStarting;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import ic2.api.item.Items;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.LoadingCallback;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.MinecraftForge;

@Mod(modid = "WarpDrive", name = "WarpDrive", version = "1.0.1")
@NetworkMod(clientSideRequired = true, serverSideRequired = true)
/**
 * @author Cr0s
 */
public class WarpDrive implements LoadingCallback {

    public final static int WARP_CORE_BLOCKID = 500;
    public final static int PROTOCOL_BLOCK_BLOCKID = 501;
    public final static int RADAR_BLOCK_BLOCKID = 502;
    public final static int ISOLATION_BLOCKID = 503;
    public final static int AIR_BLOCKID = 504;
    public final static int AIRGEN_BLOCKID = 505;
    public final static int GAS_BLOCKID = 506;
    
        // World limits
    public final static int WORLD_LIMIT_BLOCKS = 100000;
    
    public final static Block warpCore = new BlockReactor(WARP_CORE_BLOCKID, 0, Material.rock)
            .setHardness(0.5F).setStepSound(Block.soundMetalFootstep)
            .setCreativeTab(CreativeTabs.tabRedstone).setUnlocalizedName("Warp Core");
    
    public final static Block protocolBlock = new BlockProtocol(PROTOCOL_BLOCK_BLOCKID, 0, Material.rock)
            .setHardness(0.5F).setStepSound(Block.soundMetalFootstep)
            .setCreativeTab(CreativeTabs.tabRedstone).setUnlocalizedName("Warp Controller");

    public final static Block radarBlock = new BlockRadar(RADAR_BLOCK_BLOCKID, 0, Material.rock)
            .setHardness(0.5F).setStepSound(Block.soundMetalFootstep)
            .setCreativeTab(CreativeTabs.tabRedstone).setUnlocalizedName("W-Radar");    

    public final static Block isolationBlock = new BlockWarpIsolation(ISOLATION_BLOCKID, 0, Material.rock)
            .setHardness(0.5F).setStepSound(Block.soundMetalFootstep)
            .setCreativeTab(CreativeTabs.tabRedstone).setUnlocalizedName("Warp-Field Isolation Block");      

    public final static Block airgenBlock = new BlockAirGenerator(AIRGEN_BLOCKID, 0, Material.rock)
            .setHardness(0.5F).setStepSound(Block.soundMetalFootstep)
            .setCreativeTab(CreativeTabs.tabRedstone).setUnlocalizedName("Air Generator"); 

    public final static Block airBlock = (new BlockAir(AIR_BLOCKID)).setHardness(0.0F).setUnlocalizedName("Air block"); 
    public final static Block gasBlock = (new BlockGas(GAS_BLOCKID)).setHardness(0.0F).setUnlocalizedName("Gas block"); 
    
    public static BiomeGenBase spaceBiome;
    public World space;
    private int spaceProviderID;
    public int spaceDimID;
    public SpaceWorldGenerator spaceWorldGenerator;
    public HyperSpaceWorldGenerator hyperSpaceWorldGenerator;

    public World hyperSpace;
    private int hyperSpaceProviderID;
    public int hyperSpaceDimID;
    
    @Instance("WarpDrive")
    public static WarpDrive instance;
    @SidedProxy(clientSide = "cr0s.WarpDrive.ClientProxy", serverSide = "cr0s.WarpDrive.CommonProxy")
    public static CommonProxy proxy;
    
    public WarpCoresRegistry registry;
    
    @PreInit
    public void preInit(FMLPreInitializationEvent event) {
        if(FMLCommonHandler.instance().getSide().isClient())
        {
            System.out.println("[WarpDrive] Registering sounds event handler...");
            MinecraftForge.EVENT_BUS.register(new SoundHandler());  
        }
    }

    @Init
    public void load(FMLInitializationEvent event) {
        
        LanguageRegistry.addName(warpCore, "Warp Core");
        GameRegistry.registerBlock(warpCore, "warpCore");
        GameRegistry.registerTileEntity(TileEntityReactor.class, "warpCore");
        
        LanguageRegistry.addName(protocolBlock, "Warp Controller");
        GameRegistry.registerBlock(protocolBlock, "protocolBlock");
        GameRegistry.registerTileEntity(TileEntityProtocol.class, "protocolBlock");        
        
        LanguageRegistry.addName(radarBlock, "W-Radar");
        GameRegistry.registerBlock(radarBlock, "radarBlock");
        GameRegistry.registerTileEntity(TileEntityRadar.class, "radarBlock");         

        LanguageRegistry.addName(isolationBlock, "Warp-Field Isolation Block");
        GameRegistry.registerBlock(isolationBlock, "isolationBlock");         
        
        LanguageRegistry.addName(airBlock, "Air block");
        GameRegistry.registerBlock(airBlock, "airBlock");         

        LanguageRegistry.addName(gasBlock, "Gas block");
        GameRegistry.registerBlock(gasBlock, "gasBlock");         
        
        LanguageRegistry.addName(airgenBlock, "Air Generator");
        GameRegistry.registerBlock(airgenBlock, "airgenBlock");
        GameRegistry.registerTileEntity(TileEntityAirGenerator.class, "airgenBlock");         
        
        proxy.registerEntities();

        ForgeChunkManager.setForcedChunkLoadingCallback(instance, instance);

        spaceWorldGenerator = new SpaceWorldGenerator();
        GameRegistry.registerWorldGenerator(spaceWorldGenerator);

        hyperSpaceWorldGenerator = new HyperSpaceWorldGenerator();
        GameRegistry.registerWorldGenerator(hyperSpaceWorldGenerator);        
        
        registerSpaceDimension();
        registerHyperSpaceDimension();
        MinecraftForge.EVENT_BUS.register(new SpaceEventHandler());
    }

    @PostInit
    public void postInit(FMLPostInitializationEvent event) {
        space = DimensionManager.getWorld(spaceDimID);
        hyperSpace = DimensionManager.getWorld(hyperSpaceDimID);
        
        GameRegistry.addRecipe(new ItemStack(warpCore), "ici", "cmc", "ici",
        'i', Items.getItem("iridiumPlate"), 'm', Items.getItem("advancedMachine"), 'c', Items.getItem("advancedCircuit"));
        
        GameRegistry.addRecipe(new ItemStack(protocolBlock), "iic", "imi", "cii",
            'i', Items.getItem("iridiumPlate"), 'm', Items.getItem("advancedMachine"), 'c', Items.getItem("advancedCircuit"));   
        
        GameRegistry.addRecipe(new ItemStack(radarBlock), "ifi", "imi", "imi",
            'i', Items.getItem("iridiumPlate"), 'm', Items.getItem("advancedMachine"), 'f', Items.getItem("frequencyTransmitter")); 
        
        GameRegistry.addRecipe(new ItemStack(isolationBlock), "iii", "idi", "iii",
            'i', Items.getItem("iridiumPlate"), 'm', Items.getItem("advancedMachine"), 'd', Block.blockDiamond);         
        
        GameRegistry.addRecipe(new ItemStack(airgenBlock), "lcl", "lml", "lll",
            'l', Block.leaves, 'm', Items.getItem("advancedMachine"), 'c', Items.getItem("advancedCircuit"));         
        
        registry = new WarpCoresRegistry();
    }

    private void registerSpaceDimension() {
        spaceBiome = (new BiomeSpace(23)).setColor(0).setDisableRain().setBiomeName("Space");
        this.spaceProviderID = 14;

        DimensionManager.registerProviderType(this.spaceProviderID, SpaceProvider.class, true);
        this.spaceDimID = DimensionManager.getNextFreeDimId();
        DimensionManager.registerDimension(this.spaceDimID, this.spaceProviderID);
    }

    private void registerHyperSpaceDimension() {
        this.hyperSpaceProviderID = 15;

        DimensionManager.registerProviderType(this.hyperSpaceProviderID, HyperSpaceProvider.class, true);
        this.hyperSpaceDimID = DimensionManager.getNextFreeDimId();
        DimensionManager.registerDimension(this.hyperSpaceDimID, this.hyperSpaceProviderID);
    }    
    
    @ServerStarting
    public void serverLoad(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new GenerateCommand());
        event.registerServerCommand(new SpaceTpCommand());
    }
    
    @Override
    public void ticketsLoaded(List<Ticket> tickets, World world) {
        for(Ticket ticket : tickets) {
            ForgeChunkManager.releaseTicket(ticket);
        }
    }
}