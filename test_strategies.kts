import org.eclipse.jgit.merge.MergeStrategy
println(MergeStrategy.get().map { it.name }.joinToString())
