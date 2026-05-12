package timingtest;
import edu.princeton.cs.algs4.Stopwatch;

/**
 * Created by hug.
 */
public class TimeAList {
    private static void printTimingTable(AList<Integer> Ns, AList<Double> times, AList<Integer> opCounts) {
        System.out.printf("%12s %12s %12s %12s\n", "N", "time (s)", "# ops", "microsec/op");
        System.out.printf("------------------------------------------------------------\n");
        for (int i = 0; i < Ns.size(); i += 1) {
            int N = Ns.get(i);
            double time = times.get(i);
            int opCount = opCounts.get(i);
            double timePerOp = time / opCount * 1e6;
            System.out.printf("%12d %12.2f %12d %12.2f\n", N, time, opCount, timePerOp);
        }
    }

    public static void main(String[] args) {
        timeAListConstruction();
    }

    public static void timeAListConstruction() {
        // TODO: YOUR CODE HERE
        AList<Integer> Ns = createNsAList();
        AList<Double> times = new AList<>();
        for (int i = 0; i < Ns.size(); i++) {
            times.addLast(executeAddLast(Ns.get(i)));
        }
        printTimingTable(Ns, times, Ns);
    }

    private static double executeAddLast(int n) {
        AList<Integer> a = new AList<>();
        Stopwatch stopwatch = new Stopwatch();
        for (int i = 0; i < n; i++) {
            a.addLast(1);
        }
        return stopwatch.elapsedTime();
    }

    private static AList<Integer> createNsAList() {
        AList<Integer> Ns = new AList<>();
        int n = 1000;
        while (n <= 10000000) {
            Ns.addLast(n);
            n *= 2;
        }
        return Ns;
    }
}
