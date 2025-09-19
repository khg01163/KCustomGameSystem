package com.unciv.logic.battle

import com.unciv.logic.map.mapunit.MapUnit
import com.unciv.logic.map.Tile
import kotlin.math.max
import kotlin.random.Random

object Battle {
    // 지연 공격 저장소
    private val pendingAttacks = mutableListOf<PendingAttack>()

    fun calculateDamage(attacker: MapUnit, defender: MapUnit, distance: Int): Int {
        // 유닛이 지연 공격 특성을 가지고 있는지 확인
        val delayedTurns = attacker.baseUnit.getMatchingUnique("DelayedAttack")
            ?.params?.getOrNull(0)?.toIntOrNull()

        if (delayedTurns != null && delayedTurns > 0) {
            // 공격을 예약하고 즉시 피해는 없음
            pendingAttacks.add(PendingAttack(attacker.id, defender.currentTile, delayedTurns, attacker.id))
            println("${attacker.name} schedules a delayed attack on ${defender.currentTile.position} in $delayedTurns turn(s)")
            return 0
        }

        // 일반 공격 흐름
        val baseDamage = attacker.getAttackingStrengthAgainst(defender)

        if (checkEvasion(defender)) {
            println("${defender.name} evaded the attack!")
            return 0
        }

        return applyDistanceDamageReduction(baseDamage, distance, attacker)
    }

    /** 턴이 끝날 때 호출해서 예약된 공격을 실행 */
    fun resolvePendingAttacks(units: Map<String, MapUnit>) {
        val toResolve = mutableListOf<PendingAttack>()

        for (attack in pendingAttacks) {
            val newDelay = attack.delay - 1
            if (newDelay <= 0) {
                toResolve.add(attack)
            } else {
                pendingAttacks[pendingAttacks.indexOf(attack)] = attack.copy(delay = newDelay)
            }
        }

        // 실행
        for (attack in toResolve) {
            val attacker = units[attack.attackerId] ?: continue
            val targetTile = attack.targetTile
            val defender = targetTile.getDefendingUnit() ?: continue

            val baseDamage = attacker.getAttackingStrengthAgainst(defender)
            val damage = applyDistanceDamageReduction(baseDamage, attacker.getDistanceTo(defender), attacker)

            defender.takeDamage(damage)
            println("Delayed attack from ${attacker.name} hits ${defender.name} for $damage at ${targetTile.position}")
            pendingAttacks.remove(attack)
        }
    }

    private fun checkEvasion(defender: MapUnit): Boolean {
        val evasionChance = defender.baseUnit.getMatchingUnique("EvasionChance")
            ?.params?.getOrNull(0)?.toDoubleOrNull() ?: 0.0
        if (evasionChance <= 0.0) return false
        return Random.nextDouble() < evasionChance
    }

    private fun applyDistanceDamageReduction(damage: Int, distance: Int, attacker: MapUnit): Int {
        val falloff = attacker.baseUnit.getMatchingUnique("RangedDamageFalloff")
            ?.params?.getOrNull(0)?.toDoubleOrNull() ?: 0.2
        val fullRange = attacker.baseUnit.getMatchingUnique("RangedDamageFullRange")
            ?.params?.getOrNull(0)?.toIntOrNull() ?: 0

        if (distance <= fullRange) return damage

        val effectiveDistance = distance - fullRange
        val reductionFactor = max(0.2, 1.0 - (falloff * effectiveDistance))
        return (damage * reductionFactor).toInt()
    }
}

data class PendingAttack(
    val attackerId: String,
    val targetTile: Tile,
    val delay: Int,
    val attackerName: String
)
