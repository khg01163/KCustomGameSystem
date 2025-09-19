package com.unciv.logic.battle

import com.unciv.logic.map.mapunit.MapUnit
import kotlin.math.max
import kotlin.random.Random

object Battle {

    fun calculateDamage(attacker: MapUnit, defender: MapUnit, distance: Int): Int {
        // 기본 피해 계산 (간단화 — 원래 Unciv에는 더 많은 요소가 있음)
        val baseDamage = attacker.getAttackingStrengthAgainst(defender)

        // 회피율 적용
        if (checkEvasion(defender)) {
            println("${defender.name} evaded the attack!")
            return 0
        }

        // 거리별 피해 감소 적용
        val finalDamage = applyDistanceDamageReduction(baseDamage, distance, attacker)

        return finalDamage
    }

    /** 유닛의 회피 확률을 계산한다 (기본값 0%) */
    private fun checkEvasion(defender: MapUnit): Boolean {
        val evasionChance = defender.baseUnit.getMatchingUnique("EvasionChance")
            ?.params?.getOrNull(0)?.toDoubleOrNull() ?: 0.0

        if (evasionChance <= 0.0) return false

        val roll = Random.nextDouble()
        return roll < evasionChance
    }

    /** 유닛의 사거리에 따른 피해 감소를 계산한다 */
    private fun applyDistanceDamageReduction(damage: Int, distance: Int, attacker: MapUnit): Int {
        // 감소율 (기본 0.2 = 20%)
        val falloff = attacker.baseUnit.getMatchingUnique("RangedDamageFalloff")
            ?.params?.getOrNull(0)?.toDoubleOrNull() ?: 0.2

        // 풀데미지 보장 사거리 (기본 0)
        val fullRange = attacker.baseUnit.getMatchingUnique("RangedDamageFullRange")
            ?.params?.getOrNull(0)?.toIntOrNull() ?: 0

        if (distance <= fullRange) return damage

        val effectiveDistance = distance - fullRange
        val reductionFactor = max(0.2, 1.0 - (falloff * effectiveDistance))
        return (damage * reductionFactor).toInt()
    }
}
