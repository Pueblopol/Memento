import org.eclipse.jgit.api.RebaseResult
println(RebaseResult.Status.values().map { it.name }.joinToString())
