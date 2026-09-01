import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

val repoDir = File("test_repo")
if (repoDir.exists()) repoDir.deleteRecursively()
repoDir.mkdirs()

// 1. Init remote repo
val remoteDir = File("test_remote")
if (remoteDir.exists()) remoteDir.deleteRecursively()
remoteDir.mkdirs()
val remoteGit = Git.init().setDirectory(remoteDir).setBare(true).call()

// 2. Clone to local
val git = Git.cloneRepository().setURI(remoteDir.absolutePath).setDirectory(repoDir).call()

// 3. Create Note 1
val note1 = File(repoDir, "Note1.md")
note1.writeText("---\nid: 1\nfolders: []\n---\nHello")
git.add().addFilepattern(".").call()
git.commit().setMessage("Add Note 1").call()
git.push().call()

// 4. OFFLINE MODE: Move Note 1 to Folder 1
note1.delete()
val folder1 = File(repoDir, "Folder1")
folder1.mkdirs()
val note1New = File(folder1, "Note1.md")
note1New.writeText("---\nid: 1\nfolders: [\"Folder1\"]\n---\nHello")

git.add().addFilepattern(".").call()
git.add().setUpdate(true).addFilepattern(".").call()
git.commit().setMessage("Move Note 1 to Folder 1").call()

// We don't push because we are offline

// 5. ONLINE MODE: Sync
val pullResult = git.pull().setStrategy(org.eclipse.jgit.merge.MergeStrategy.THEIRS).call()
println("Pull successful? ${pullResult.isSuccessful}")

// Check where the file is
println("Files in repo:")
repoDir.walkTopDown().forEach { if (it.isFile && it.name.endsWith(".md")) println(it.absolutePath) }
