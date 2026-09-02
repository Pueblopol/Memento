package com.pol.memento.sync

import org.eclipse.jgit.api.Git
import org.junit.Test
import java.io.File
import org.junit.Assert.assertTrue

class DoubleFolderSyncTest {
    @Test
    fun testDoubleFolder() {
        val remoteDir = File("build/test_double_remote")
        if (remoteDir.exists()) remoteDir.deleteRecursively()
        remoteDir.mkdirs()
        Git.init().setDirectory(remoteDir).setBare(true).call()

        val pcDir = File("build/test_double_pc")
        if (pcDir.exists()) pcDir.deleteRecursively()
        val pcGit = Git.cloneRepository().setURI(remoteDir.absolutePath).setDirectory(pcDir).call()

        // 1. Initial note in FolderA
        val folderA = File(pcDir, "FolderA")
        folderA.mkdirs()
        val note1 = File(folderA, "Note1.md")
        note1.writeText("---\nid: 1\nfolders: [\"FolderA\"]\n---\nHello")
        pcGit.add().addFilepattern(".").call()
        pcGit.commit().setMessage("Init Note1 in FolderA").call()
        pcGit.push().call()

        val repoDir = File("build/test_double_repo")
        if (repoDir.exists()) repoDir.deleteRecursively()
        val git = Git.cloneRepository().setURI(remoteDir.absolutePath).setDirectory(repoDir).call()

        // 2. Phone (offline) copies note to FolderB
        val folderB = File(repoDir, "FolderB")
        folderB.mkdirs()
        val note1Old = File(repoDir, "FolderA/Note1.md")
        note1Old.writeText("---\nid: 1\nfolders: [\"FolderA\", \"FolderB\"]\n---\nHello")
        git.add().addFilepattern(".").call()
        git.add().setUpdate(true).addFilepattern(".").call()
        git.commit().setMessage("Phone add to FolderB").call()

        // 3. Phone (offline) removes note from FolderA
        note1Old.delete()
        val note1New = File(folderB, "Note1.md")
        note1New.writeText("---\nid: 1\nfolders: [\"FolderB\"]\n---\nHello")
        git.add().addFilepattern(".").call()
        git.add().setUpdate(true).addFilepattern(".").call()
        git.commit().setMessage("Phone remove from FolderA").call()

        // 4. Phone syncs
        val pullResult = git.pull().setContentMergeStrategy(org.eclipse.jgit.merge.ContentMergeStrategy.THEIRS).call()
        System.out.println("Merge status: " + pullResult.mergeResult?.mergeStatus)

        repoDir.walkTopDown().forEach { if (it.isFile && it.name.endsWith(".md")) System.out.println("FILE: " + it.absolutePath) }
    }
}
