package com.mrcrayfish.mightymail.clienttest;

import com.mrcrayfish.mightymail.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A small step-by-step in-game test runner for the NeoForge jar, since NeoForge has no client game
 * test framework like Fabric's. It only runs in the {@code clientTest} run
 * ({@code ./gradlew :neoforge:runClientTest}), which sets the system property below; this source set
 * is never in the jar players get.
 *
 * <p>Each step runs on the game's own thread, one or more ticks at a time. Checks log
 * {@code [test] ok:} or {@code [test] FAILED:}; at the end, any failure throws, which crashes the game
 * with a crash report and a non-zero exit code, so Gradle (and CI) fail. When every check has passed,
 * the game closes normally. A watchdog ends a run that hangs.
 */
@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
final class ClientTest
{
    static final boolean ENABLED = Boolean.getBoolean(Constants.MOD_ID + ".clienttest");
    /** A run that takes longer than this has hung (the game thread may be stuck, so a separate thread checks). */
    private static final long WATCHDOG_MILLIS = 10 * 60 * 1000;

    /** One step: returns true once it's done, and is called again next tick until then. */
    interface Step
    {
        boolean tick(Minecraft mc);
    }

    private static final Deque<Step> STEPS = new ArrayDeque<>();
    private static final List<String> FAILURES = new ArrayList<>();
    private static boolean started;
    private static boolean finished;

    private ClientTest() {}

    /** Runs once, on the client thread. */
    static void run(Consumer<Minecraft> action)
    {
        STEPS.add(mc -> {
            action.accept(mc);
            return true;
        });
    }

    /** Runs once on the singleplayer server's thread, and waits for it to finish. */
    static void onServer(Consumer<MinecraftServer> action)
    {
        run(mc -> {
            IntegratedServer server = mc.getSingleplayerServer();
            if(server == null)
                throw new AssertionError("No singleplayer server");
            server.submit(() -> action.accept(server)).join();
        });
    }

    /** Waits this many ticks. */
    static void waitTicks(int ticks)
    {
        int[] left = {ticks};
        STEPS.add(mc -> --left[0] <= 0);
    }

    /** Waits until the condition holds, failing the run after this many ticks. */
    static void waitUntil(String what, int timeoutTicks, Predicate<Minecraft> condition)
    {
        int[] waited = {0};
        STEPS.add(mc -> {
            if(condition.test(mc))
                return true;
            if(++waited[0] > timeoutTicks)
                throw new AssertionError("Timed out after " + timeoutTicks + " ticks waiting for: " + what);
            return false;
        });
    }

    /** Saves a screenshot in the run's screenshots folder (named by the time it was taken, so the name is logged). */
    static void screenshot(String name)
    {
        run(mc -> {
            Constants.LOG.info("[test] screenshot: {}", name);
            Screenshot.grab(mc, false);
        });
        waitTicks(2);
    }

    /** Logs the check; a failed one fails the run at the end, after every other check has run. */
    static void check(boolean ok, String what)
    {
        if(ok)
        {
            Constants.LOG.info("[test] ok: {}", what);
        }
        else
        {
            Constants.LOG.error("[test] FAILED: {}", what);
            FAILURES.add(what);
        }
    }

    /** Creates a fresh flat creative world with commands on, and waits until the player is in it. */
    static void createWorld(String baseName)
    {
        String name = baseName + "-" + System.currentTimeMillis();
        waitUntil("the title screen", 2400, mc -> mc.gui.overlay() == null && mc.gui.screen() != null);
        run(mc -> mc.createWorldOpenFlows().createFreshLevel(name,
            new LevelSettings(name, GameType.CREATIVE, new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT),
            new WorldOptions(1L, false, false),
            registries -> registries.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
            mc.gui.screen()));
        waitUntil("the player in the new world", 2400, mc -> mc.player != null && mc.level != null && mc.gui.screen() == null);
        // Time for the chunks around the player to arrive and draw
        waitTicks(60);
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event)
    {
        if(!ENABLED || finished)
            return;
        Minecraft mc = Minecraft.getInstance();
        if(!started)
        {
            started = true;
            Constants.LOG.info("[test] NeoForge client test starting");
            Thread watchdog = new Thread(() -> {
                try
                {
                    Thread.sleep(WATCHDOG_MILLIS);
                }
                catch(InterruptedException e)
                {
                    return;
                }
                Constants.LOG.error("[test] the client test hung for {} minutes; stopping", WATCHDOG_MILLIS / 60000);
                Runtime.getRuntime().halt(3);
            }, "Mighty Mail client test watchdog");
            watchdog.setDaemon(true);
            watchdog.start();
            MightyMailClientTest.steps();
        }
        Step step = STEPS.peek();
        if(step == null)
        {
            finished = true;
            if(!FAILURES.isEmpty())
                throw new AssertionError(FAILURES.size() + " check(s) failed:\n - " + String.join("\n - ", FAILURES));
            Constants.LOG.info("[test] NeoForge client test passed, every check");
            mc.stop();
            return;
        }
        if(step.tick(mc))
            STEPS.poll();
    }
}
