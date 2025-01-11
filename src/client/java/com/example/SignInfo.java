package com.example;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.util.math.BlockPos;

public record SignInfo(SignBlockEntity sign, BlockPos pos) {
}
