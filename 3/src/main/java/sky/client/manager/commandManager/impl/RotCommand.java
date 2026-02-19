/*package sky.client.manager.commandManager.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Formatting;
import sky.client.manager.ClientManager;
import sky.client.manager.Manager;
import sky.client.manager.commandManager.Command;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class RotCommand extends Command {
    public RotCommand() { super("rot"); }

    @Override
    public void execute(LiteralArgumentBuilder<CommandSource> root) {
        var ai = Manager.FUNCTION_MANAGER.attackAura.neuralAI;

        root.then(literal("record").executes(ctx -> {
            ai.startRecording();
            ClientManager.message(Formatting.GREEN + "Запись! Бей игроков своей мышкой.");
            return SINGLE_SUCCESS;
        }));

        root.then(literal("stop").executes(ctx -> {
            int count = ai.stopRecording();
            ClientManager.message(Formatting.YELLOW + "Стоп: " + count + " сэмплов");
            return SINGLE_SUCCESS;
        }));

        root.then(literal("train").executes(ctx -> {
            if (ai.getSampleCount() < 200) {
                ClientManager.message(Formatting.RED + "Мало данных! Минимум 200, есть " + ai.getSampleCount());
            } else {
                ai.train();
                ClientManager.message(Formatting.AQUA + "Обучение запущено в фоне...");
            }
            return SINGLE_SUCCESS;
        }));

        root.then(literal("save").then(arg("name", StringArgumentType.word()).executes(ctx -> {
            String name = StringArgumentType.getString(ctx, "name");
            ClientManager.message(ai.save(name)
                    ? Formatting.GREEN + "Сохранено: " + name
                    : Formatting.RED + "Ошибка");
            return SINGLE_SUCCESS;
        })));

        root.then(literal("load").then(arg("name", StringArgumentType.word()).executes(ctx -> {
            String name = StringArgumentType.getString(ctx, "name");
            ClientManager.message(ai.load(name)
                    ? Formatting.GREEN + "Загружено!"
                    : Formatting.RED + "Не найден");
            return SINGLE_SUCCESS;
        })));

        root.then(literal("play").executes(ctx -> {
            if (!ai.isTrained()) {
                ClientManager.message(Formatting.RED + "Сначала обучи: .rot train");
            } else if (ai.isPlaying()) {
                ai.stopPlaying();
                ClientManager.message(Formatting.YELLOW + "AI OFF");
            } else {
                ai.startPlaying();
                ClientManager.message(Formatting.GREEN + "AI ON");
            }
            return SINGLE_SUCCESS;
        }));

        root.then(literal("info").executes(ctx -> {
            ClientManager.message(Formatting.GRAY + "=== Neural Rotation AI ===");
            ClientManager.message("Сэмплов: " + Formatting.WHITE + ai.getSampleCount());
            ClientManager.message("Запись: " + (ai.isRecording() ? Formatting.GREEN + "да" : Formatting.RED + "нет"));
            ClientManager.message("Обучена: " + (ai.isTrained() ? Formatting.GREEN + "да" : Formatting.RED + "нет"));
            ClientManager.message("AI: " + (ai.isPlaying() ? Formatting.GREEN + "ON" : Formatting.RED + "OFF"));
            return SINGLE_SUCCESS;
        }));

        root.then(literal("clear").executes(ctx -> {
            ai.clear();
            ClientManager.message(Formatting.YELLOW + "Очищено");
            return SINGLE_SUCCESS;
        }));
    }
}*/