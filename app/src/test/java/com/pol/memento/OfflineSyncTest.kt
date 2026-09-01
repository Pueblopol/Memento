package com.pol.memento

import org.eclipse.jgit.api.Git
import org.junit.Test
import java.io.File
import org.junit.Assert.assertTrue

class OfflineSyncTest {
    @Test
    fun testOfflineMove() {
        val repoDir = File("build/test_ff_repo")
        if (repoDir.exists()) repoDir.deleteRecursively()
        repoDir.mkdirs()

        val remoteDir = File("build/test_ff_remote")
        if (remoteDir.exists()) remoteDir.deleteRecursively()
        remoteDir.mkdirs()
        Git.init().setDirectory(remoteDir).setBare(true).call()

        val git = Git.cloneRepository().setURI(remoteDir.absolutePath).setDirectory(repoDir).call()

        val note1 = File(repoDir, "Note1.md")
        note1.writeText("---\nid: 1\nfolders: []\n---\nHello")
        git.add().addFilepattern(".").call()
        git.commit().setMessage("Add Note 1").call()
        git.push().call()

        note1.delete()
        val folder1 = File(repoDir, "Folder1")
        folder1.mkdirs()
        val note1New = File(folder1, "Note1.md")
        note1New.writeText("---\nid: 1\nfolders: [\"Folder1\"]\n---\nHello")

        git.add().addFilepattern(".").call()
        git.add().setUpdate(true).addFilepattern(".").call()
        git.commit().setMessage("Move Note 1 to Folder 1").call()

        val pullResult = git.pull().setStrategy(org.eclipse.jgit.merge.MergeStrategy.THEIRS).call()
        System.out.println("Pull successful? " + pullResult.isSuccessful)
        System.out.println("Merge status: " + pullResult.mergeResult?.mergeStatus)

        repoDir.walkTopDown().forEach { if (it.isFile && it.name.endsWith(".md")) System.out.println("FILE: " + it.absolutePath) }
    }
}
