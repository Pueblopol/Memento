package com.pol.memento.sync

import org.eclipse.jgit.api.Git
import org.junit.Test
import java.io.File

class PullTest {
    @Test
    fun testPull() {
        val repoDir = File("build/test_pull_repo")
        Git.init().setDirectory(repoDir).call().use { git ->
            git.pull().setContentMergeStrategy(org.eclipse.jgit.merge.ContentMergeStrategy.THEIRS)
        }
    }
}
