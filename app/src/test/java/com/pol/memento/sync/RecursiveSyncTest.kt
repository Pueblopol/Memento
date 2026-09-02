package com.pol.memento.sync

import org.eclipse.jgit.api.Git
import org.junit.Test
import java.io.File
import org.junit.Assert.assertTrue

class RecursiveSyncTest {
    @Test
    fun testRecursiveMove() {
        val remoteDir = File("build/test_rec_remote")
        if (remoteDir.exists()) remoteDir.deleteRecursively()
        remoteDir.mkdirs()
        Git.init().setDirectory(remoteDir).setBare(true).call()

        val repoDir = File("build/test_rec_repo")
        if (repoDir.exists()) repoDir.deleteRecursively()
        val git = Git.cloneRepository().setURI(remoteDir.absolutePath).setDirectory(repoDir).call()

        val note1 = File(repoDir, "Note1.md")
        note1.writeText("---\nid: 1\nfolders: []\n---\nHello")
        git.add().addFilepattern(".").call()
        git.commit().setMessage("Init").call()
        git.push().call()

        val pcDir = File("build/test_rec_pc")
        if (pcDir.exists()) pcDir.deleteRecursively()
        val pcGit = Git.cloneRepository().setURI(remoteDir.absolutePath).setDirectory(pcDir).call()
        File(pcDir, "Note2.md").writeText("---\nid: 2\nfolders: []\n---\nHello 2")
        pcGit.add().addFilepattern(".").call()
        pcGit.commit().setMessage("PC add note 2").call()
        pcGit.push().call()

        note1.delete()
        val folder1 = File(repoDir, "Folder1")
        folder1.mkdirs()
        val note1New = File(folder1, "Note1.md")
        note1New.writeText("---\nid: 1\nfolders: [\"Folder1\"]\n---\nHello from Phone")
        git.add().addFilepattern(".").call()
        git.add().setUpdate(true).addFilepattern(".").call()
        git.commit().setMessage("Phone move").call()

        val pullResult = git.pull().setContentMergeStrategy(org.eclipse.jgit.merge.ContentMergeStrategy.THEIRS).call()
        System.out.println("Merge status: " + pullResult.mergeResult?.mergeStatus)

        repoDir.walkTopDown().forEach { if (it.isFile && it.name.endsWith(".md")) System.out.println("FILE: " + it.absolutePath) }
    }
}
