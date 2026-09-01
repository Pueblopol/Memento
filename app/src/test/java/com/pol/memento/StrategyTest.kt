package com.pol.memento

import org.eclipse.jgit.merge.MergeStrategy
import org.junit.Test

class StrategyTest {
    @Test
    fun testStrats() {
        System.out.println("STRATS: " + MergeStrategy.get().map { it.name }.joinToString())
    }
}
