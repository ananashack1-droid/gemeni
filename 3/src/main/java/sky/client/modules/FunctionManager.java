package sky.client.modules;

import sky.client.modules.combat.*;
import sky.client.modules.misc.*;
import sky.client.modules.movement.*;
import sky.client.modules.player.*;
import sky.client.modules.render.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FunctionManager {

    private final List<Function> functions = new CopyOnWriteArrayList<>();

    // Combat
    public AntiBot antiBot;
    public AttackAura attackAura;
    public AutoExplosion autoExplosion;
    public AutoPotion autoPotion;
    public AutoTotem autoTotem;
    public BackTrack backTrack;
    public HitBox hitBox;
    public NoFriendDamage noFriendDamage;

    // Misc
    public ClientSounds clientSounds;
    public NameProtect nameProtect;
    public NoCommands noCommands;
    public Optimizer optimizer;
    public UnHook unHook;
    public AutoJoin autoJoin;
    public AutoRespawn autoRespawn;
    // Movement
    public AutoSprint autoSprint;
    public Blink blink;
    public ElytraTarget elytraTarget;
    public FreeLook freeLook;
    public NoSlow noSlow;
    public PhaseRW phase;
    public Speed speed;
    public SuperFirework superFirework;
    public TeleportRW grimTP = new TeleportRW();

    // Player
    public AutoTool autoTool;
    public ChestStealer chestStealer;
    public ClickAction clickAction;
    public CustomCoolDown customCoolDown;
    public FreeCamera freeCamera;
    public GuiWalk guiWalk;
    public NoPush noPush;
    public InvseeExploit invseeExploit;
    public ItemScroller itemScroller;
    public MiddleClick middleClick;
    public NoInteract noInteract;
    public NoRayTrace noRayTrace;

    // Render
    public Arrows arrows;
    public AspectRatio aspectRatio;
    public BlockESP blockESP;
    public BlockHighLight blockHighLight;
    public ClickGUI clickGUI;
    public CrossHair crossHair;
    public ExtraTab extraTab;
    public FogBlur fogBlur;
    public FullBright fullBright;
    public HUD hud;
    public ItemPhysic itemPhysic;
    public NameTags nameTags;
    public NoRender noRender;
    public SwingAnimations swingAnimations;
    public Trails trails;
    public ViewModel viewModel;
    public World customWorld;

    public FunctionManager() {
        initModules();
        registerFunctions();
    }

    private void initModules() {
        // Combat
        this.antiBot = new AntiBot();
        this.attackAura = new AttackAura();
        this.autoExplosion = new AutoExplosion();
        this.autoPotion = new AutoPotion();
        this.autoTotem = new AutoTotem();
        this.backTrack = new BackTrack();
        this.hitBox = new HitBox();
        this.noFriendDamage = new NoFriendDamage();

        // Misc
        this.clientSounds = new ClientSounds();
        this.nameProtect = new NameProtect();
        this.noCommands = new NoCommands();
        this.optimizer = new Optimizer();
        this.unHook = new UnHook();

        // Movement
        this.autoSprint = new AutoSprint();
        this.blink = new Blink();
        this.elytraTarget = new ElytraTarget();
        this.freeLook = new FreeLook();
        this.noSlow = new NoSlow();
        this.phase = new PhaseRW();
        this.speed = new Speed();
        this.superFirework = new SuperFirework();
        this.grimTP = new TeleportRW();

        // Player
        this.chestStealer = new ChestStealer();
        this.clickAction = new ClickAction();
        this.customCoolDown = new CustomCoolDown();
        this.freeCamera = new FreeCamera();
        this.guiWalk = new GuiWalk();
        this.itemScroller = new ItemScroller();
        this.noInteract = new NoInteract();
        this.noPush = new NoPush();
        this.noRayTrace = new NoRayTrace();

        // Render
        this.arrows = new Arrows();
        this.aspectRatio = new AspectRatio();
        this.blockESP = new BlockESP();
        this.blockHighLight = new BlockHighLight();
        this.clickGUI = new ClickGUI();
        this.crossHair = new CrossHair();
        this.extraTab = new ExtraTab();
        this.fogBlur = new FogBlur();
        this.fullBright = new FullBright();
        this.hud = new HUD();
        this.itemPhysic = new ItemPhysic();
        this.nameTags = new NameTags();
        this.noRender = new NoRender();
        this.swingAnimations = new SwingAnimations();
        this.viewModel = new ViewModel();
        this.customWorld = new World();
    }

    private void registerFunctions() {
        functions.addAll(Arrays.asList(
                // Combat
                antiBot,
                attackAura,
                autoExplosion,
                autoPotion,
                new AutoSwap(),
                autoTotem,
                backTrack,
                hitBox,
                noFriendDamage,
                //new RotationRecorder(),
                new Velocity(),

                // Misc
                new AutoDuel(),
                clientSounds,
                new DeathCoords(),
                new DragonFly(),
                new ElytraHelper(),
                nameProtect,
                noCommands,
                optimizer,
                new ServerRPSpoff(),
                unHook,
                new Xray(),
                new AutoJoin(),

                // Movement
                autoSprint,
                blink,
                new ElytraMotion(),
                new ElytraRecast(),
                elytraTarget,
                new Flight(),
                freeLook,
                noSlow,
                new NoWeb(),
                phase,
                speed,
                new Spider(),
                new Strafe(),
                superFirework,
                new Timer(),
                new TeleportRW(),



                // Player
                new AutoAccept(),
                new AutoLeave(),
                new AutoMessage(),
                new AutoRespawn(),
                new AutoTool(),
                chestStealer,
                clickAction,
                customCoolDown,
                new EnderChestExploit(),
                freeCamera,
                guiWalk,
                new InvseeExploit(),
                new ItemFixSwap(),
                itemScroller,
                new MiddleClick(),
                new NoDelay(),
                noInteract,
                noPush,
                noRayTrace,
                new PerfectTime(),
                new RegionExploit(),


                // Render
                arrows,
                aspectRatio,
                blockESP,
                blockHighLight,
                clickGUI,
                crossHair,
                new ESP(),
                extraTab,
                fogBlur,
                fullBright,
                hud,
                itemPhysic,
                nameTags,
                noRender,
                new Particles(),
                new Prediction(),
                // new ShulkerPreview(),
                swingAnimations,
                new TargetESP(),
                new Trails(),
                viewModel,
                customWorld
        ));
    }

    public List<Function> getFunctions() {
        return functions;
    }

    public List<Function> getFunctions(Type category) {
        List<Function> result = new ArrayList<>();
        for (Function function : functions) {
            if (function.getCategory() == category) {
                result.add(function);
            }
        }
        return result;
    }

    public Function get(String name) {
        for (Function function : functions) {
            if (function != null && function.name.equalsIgnoreCase(name)) {
                return function;
            }
        }
        return null;
    }
}