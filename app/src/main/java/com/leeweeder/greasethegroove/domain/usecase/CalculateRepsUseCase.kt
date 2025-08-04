package com.leeweeder.greasethegroove.domain.usecase

class CalculateRepsUseCase {
    operator fun invoke(maxReps: Int, restDuration: Int): Int {
        val percentage = if (restDuration <= 60) 0.45 else 0.55
        return (maxReps * percentage).toInt().coerceAtLeast(1)
    }
}