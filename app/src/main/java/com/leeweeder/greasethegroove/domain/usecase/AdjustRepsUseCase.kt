package com.leeweeder.greasethegroove.domain.usecase

class AdjustRepsUseCase {
    operator fun invoke(currentReps: Int, rpe: Int): Int {
        return when {
            rpe <= 3 -> currentReps + 1
            rpe >= 7 -> (currentReps - 1).coerceAtLeast(1)
            else -> currentReps
        }
    }
}