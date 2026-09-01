import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.merge.MergeStrategy;
public class test_pull {
    public static void main(String[] args) {
        PullCommand pc = null;
        pc.setStrategy(MergeStrategy.THEIRS);
    }
}
