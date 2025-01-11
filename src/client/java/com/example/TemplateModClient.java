package com.example;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.LiteralCommandNode;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;


import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import static net.minecraft.server.command.CommandManager.argument;


import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class TemplateModClient implements ClientModInitializer {
	public static final int RADIUS = 16;
	public static final String MOD_ID = "templatemod";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
	public static List<SignInfo> signs = new ArrayList<>();

	public static boolean isValidShop(SignBlockEntity sign) {
		List<String> rows = new ArrayList<>();
		LOGGER.info("this is isValidShop");
		for(int line = 0; line < 4; line++){
            rows.add(sign.getText(true).getMessage(line, false).getString());
		}

		if (!(Pattern.matches("(((Покупает|Продает|Bying|Selling)? [0-9]+)|((Нет товара|Out of stock)?))?", rows.get(1)))) {
			LOGGER.info("1)" + rows.get(0) +"|" + rows.get(1) +"|" + rows.get(2) +"|"+rows.get(3));
			return false;
		}
		if (!(Pattern.matches("[^0-9]*[0-9,.]+[^0-9]*",rows.get(3)))){
			LOGGER.info("1)" + rows.get(0) +"|" + rows.get(1) +"|" + rows.get(2) +"|"+rows.get(3));
			return false;
		}
		return true;
	}

	public static void getNearbySigns(BlockPos location, int radius) {
		LOGGER.info("this is getNearbySigns");
		List<Block> blocks = new ArrayList<Block>();
		for(int x = location.getX() - radius; x <= location.getX() + radius; x++) {
			for(int y = -64; y <= 319; y++) {
				for(int z = location.getZ() - radius; z <= location.getZ() + radius; z++) {
					BlockEntity blockEntity = MinecraftClient.getInstance().world.getBlockEntity(new BlockPos(x, y, z));
					if (blockEntity instanceof SignBlockEntity) {
						String signText = new String();
						SignBlockEntity sign = (SignBlockEntity) blockEntity;
						for(int line = 0; line < 4; line++){
							signText += (sign.getText(true).getMessage(line,false).getString()) + " ";
						}
						LOGGER.info("[X:"+sign.getPos().getX() + " Y:" + sign.getPos().getY() + " Z:" + sign.getPos().getZ()+"]" + signText);
						if (isValidShop(sign)) {
							signs.add(new SignInfo(sign,sign.getPos()));
						}
					}
				}
			}
		}
	}

	public String convertToCSV(SignInfo sign) {
		LOGGER.info("this is convertToCSV");
		String data = sign.pos().getX()
				+ "," + sign.pos().getY()
				+ "," + sign.pos().getZ()
				+ "," + sign.sign().getText(true).getMessage(0,false).getString()
				+ "," + sign.sign().getText(true).getMessage(1,false).getString().replaceAll("Покупает", "Bying").replaceAll("Продает","Selling").replaceAll("(Нет товара)|(Out of stock)","Out of stock 0").replaceAll("[\\.0123456789]","").strip()
				+ "," + sign.sign().getText(true).getMessage(1,false).getString().replaceAll("(Нет товара)|(Out of stock)","Out of stock 0").replaceAll("[^\\.0123456789]","")
				+ "," + sign.sign().getText(true).getMessage(2,false).getString()
				+ "," + sign.sign().getText(true).getMessage(3,false).getString().replaceAll("[^\\.0123456789]","");
		LOGGER.info(data);
		return data;
	}

	public void writeToCSV(List<SignInfo> signs) {
		LOGGER.info("this is writeToCSV");
		try(PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream("catalogue.csv"), "UTF-8"))) {
			out.println("X,Y,Z,Seller,B/S,Quantity,Item,Cost");
			signs.stream().map(this::convertToCSV).peek(x -> LOGGER.info(x)).forEach(out::println);
		}catch (IOException e){
			LOGGER.error("Couldn't write to file");
		}
	}

	@Override
	public void onInitializeClient() {






		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(
					ClientCommandManager.literal("getSigns").then(ClientCommandManager.argument("radius", IntegerArgumentType.integer(16,2048))
									.executes(context -> {
										LOGGER.info("getSigns called");
										getNearbySigns(MinecraftClient.getInstance().player.getBlockPos(), IntegerArgumentType.getInteger(context,"radius"));
										writeToCSV(signs);
										context.getSource().sendFeedback(Text.literal("Called /getSigns"));
										return 1;
									}))
						);
		});
	}
}